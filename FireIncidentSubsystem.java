import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.*;

/**
 * The FireIncidentSubsystem class is responsible for reading an input file, creating FireEvent instances,
 * parsing information from the file line by line, and adding FireEvent objects to the scheduler queue.
 *
 * This class implements Runnable, allowing it to be executed within a thread, and processes events
 * specific to a given zone.
 *
 * @author Anique
 * @author Jasjot
 * @version 1.0
 */
public class FireIncidentSubsystem implements Runnable {

    public static final int DEFAULT_FIS_PORT = 5000;
    public static final int DEFAULT_SCHEDULER_PORT = 6100;
    private static int PORT;

    private final int zoneId;
    private final int x1, y1, x2, y2;
    private final InetAddress schedulerAddress;

    // UDP communication properties
    private DatagramSocket socket;
    private boolean running = true;

    // Scheduler properties
    private final int schedulerPort;

    // Thread for UDP listening
    private Thread udpListenerThread;
    private String eventFile;
    /**
     * Constructs a FireIncidentSubsystem to process fire events for a specific zone.
     * Processes the event file immediately upon construction.
     *
     * @param eventFile        The path to the file containing fire events data.
     * @param zoneId           The ID of the zone for which this subsystem is responsible.
     * @param x1               The x-coordinate of the top-left corner of the zone.
     * @param y1               The y-coordinate of the top-left corner of the zone.
     * @param x2               The x-coordinate of the bottom-right corner of the zone.
     * @param y2               The y-coordinate of the bottom-right corner of the zone.
     */
    public FireIncidentSubsystem(String eventFile, int zoneId,
                                 int x1, int y1, int x2, int y2) throws UnknownHostException {
        this.schedulerAddress = InetAddress.getLocalHost();
        this.schedulerPort = DEFAULT_SCHEDULER_PORT;
        this.zoneId = zoneId;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        PORT = DEFAULT_FIS_PORT + zoneId; // Use zoneId to make each FIS port uniqu
        this.eventFile=eventFile;
        try{
            this.socket = new DatagramSocket(PORT);
            System.out.println("FireIncidentSubsystem for Zone " + zoneId + " listening on port " + PORT);
        }catch (SocketException e){
            System.err.println("Error initializing UDP socket on port " + PORT);
            e.printStackTrace();
        }


    }

    /**
     * Process the event file, parsing events and sending them to the scheduler via UDP.
     *
     * @param eventFile The path to the file containing fire events data.
     */
    private void processEventFile(String eventFile) {
        boolean eventsAdded = false;
        System.out.println("Processing event file for Zone " + zoneId);

        try (BufferedReader reader = new BufferedReader(new FileReader(eventFile))) {
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue; // Skip CSV header
                }

                FireEvent fireEvent = parseEvent(line);
                // Only process events for this zone
                if (fireEvent.getZoneId() == zoneId) {
                    System.out.println("FireIncidentSubsystem-Zone " + zoneId + " → New Fire Event: " + fireEvent);

                    // Send the fire event to the scheduler via RPC
                    String response = rpc_send("ADD_FIRE_EVENT:" + fireEvent.getTime() + ":" + fireEvent.getZoneId() + ":" +
                            fireEvent.getEventType() + ":" + fireEvent.getSeverity(), schedulerAddress, schedulerPort);
                    if (response.contains("SUCCESS")) {
                        eventsAdded = true;
                    } else {
                        System.err.println("Failed to add fire event: " + response);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Only call if at least one event was added
        if (eventsAdded) {
            String response = rpc_send("SET_EVENTS_LOADED:" + zoneId, schedulerAddress, schedulerPort);
            System.out.println("Setting events to loaded for Zone " + zoneId + ": " + response);
            System.out.println("----------------------------------------\n");
        }
    }

    /**
     * Parses a single line from the fire events file and creates a FireEvent object.
     *
     * @param line A line from the fire events file representing an event.
     * @return A FireEvent instance created from the parsed data.
     */
    private FireEvent parseEvent(String line) {
        String[] slices = line.split(",");
        String time = slices[0];
        int eventZoneId = Integer.parseInt(slices[1]);
        String eventType = slices[2];
        String severity = slices[3];

        return new FireEvent(time, eventZoneId, eventType, severity, this);
    }

    /**
     * Runs the FireIncidentSubsystem, reading the event file line by line,
     * parsing the data, and adding relevant FireEvent instances to the scheduler.
     * Only events belonging to this subsystem's zone are processed.
     */
    @Override
    public synchronized void run() {
        processEventFile(eventFile);
        boolean eventsAdded = false;
        System.out.println(Thread.currentThread().getName() + " running for Zone " + zoneId);

        // Start UDP listener thread
        udpListenerThread = new Thread(new UDPListener());
        udpListenerThread.setName("UDPListener-Zone" + zoneId);
        udpListenerThread.start();
    }

    private class UDPListener implements Runnable {
        @Override
        public synchronized void run() {
            System.out.println("UDPListener running for Zone " + zoneId);
            byte[] buffer = new byte[1024];

            while (running) {
                try{
                    // Prepare to receive a packet
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Proccess the packet
                    processUDPPacket(packet);
                }catch (IOException e) {
                    if (running) {
                        System.err.println("Error receiving UDP packet: " + e.getMessage());
                    }
                }
            }
        }
    }
    /**
     * Process an incoming UDP packet, extracting the method name and parameters,
     * executing the method, and sending back the response.
     *
     * @param packet The received UDP packet
     */
    private void processUDPPacket(DatagramPacket packet) throws IOException {
        // Extract packet data
        String request = new String(packet.getData(), 0, packet.getLength()).trim();
        InetAddress clientAddress = packet.getAddress();
        int clientPort = packet.getPort();

        System.out.println("[FIS-Zone " + zoneId + "] Received request: " + request +
                " (from " + clientAddress + ":" + clientPort + ")");

        // Send ACK first
        sendResponse("ACK:" + request, clientAddress, clientPort);

        // Parse the request (format: METHOD_NAME:param1:param2:...)
        String[] parts = request.split(":");
        if (parts.length == 0) {
            sendResponse("ERROR:Invalid Request Format", clientAddress, clientPort);
            return;
        }

        String methodName = parts[0];
        String response;

        try {
            // Invoke the appropriate method
            response = invokeMethod(methodName, parts);
        } catch (Exception e) {
            System.err.println("Error invoking method " + methodName + ": " + e.getMessage());
            e.printStackTrace();
            response = "ERROR:" + e.getMessage();
        }

        // Send actual response back to the client
        sendResponse(response, clientAddress, clientPort);
    }

    /**
     * Send a response back to the client.
     *
     * @param response The response message
     * @param address The client's address
     * @param port The client's port
     */
    private void sendResponse(String response, InetAddress address, int port) throws IOException {
        byte[] responseData = response.getBytes();
        DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, address, port);
        socket.send(responsePacket);
        System.out.println("[FIS-Zone " + zoneId + "] Sent response: " + response +
                " (to " + address + ":" + port + ")");
    }

    /**
     * Invoke a method by name with the given parameters.
     *
     * @param methodName The name of the method to invoke
     * @param params The parameters for the method (including the method name at index 0)
     * @return The result of the method invocation as a string
     */
    private String invokeMethod(String methodName, String[] params) throws Exception {
        // Remove the method name from params
        String[] methodParams = new String[params.length - 1];
        System.arraycopy(params, 1, methodParams, 0, methodParams.length);

        switch (methodName) {
            case "GET_ZONE_COORDINATES":
                return getZoneCoordinates();

            case "TO_STRING":
                return toString();


            default:
                return "ERROR:Unknown method: " + methodName;
        }
    }

    /**
     * Sends an RPC request to a specified host and waits for a response.
     * The method implements a two-phase protocol: first receiving an acknowledgment,
     * then waiting for the actual response.
     *
     * @param request The complete request string in format "METHOD:param1:param2:..."
     * @param hostAddress The address of the target host
     * @param hostPort The port number of the target host
     * @return The response from the server
     */
    private String rpc_send(String request, InetAddress hostAddress, int hostPort) {
        try {
            System.out.println("\n[Zone " + zoneId + " -> Host] Sent request: " + request);

            // Send request to target
            byte[] requestData = request.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, hostAddress, hostPort+zoneId);
            socket.send(requestPacket);

            // Receive acknowledgment
            byte[] ackBuffer = new byte[1024];
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
            socket.receive(ackPacket);
            String ackResponse = new String(ackPacket.getData(), 0, ackPacket.getLength()).trim();
            System.out.println("[Zone " + zoneId + " <- Host] Got reply: ACCEPT(" + request + ")");

            if (!ackResponse.startsWith("ACK:")) {
                System.out.println(" Unexpected response instead of ACK. Aborting.");
                return "ERROR: No ACK received";
            }

            // Wait for actual response (loop until a valid response arrives)
            String serverResponse;
            while (true) {
                byte[] responseBuffer = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                socket.receive(responsePacket);  // Blocking call, waits for a packet
                serverResponse = new String(responsePacket.getData(), 0, responsePacket.getLength()).trim();

                if (!serverResponse.startsWith("ACK:")) {
                    break; // Exit loop if we get a valid response (not an ACK)
                }
            }

            System.out.println("[Server -> Host -> Zone " + zoneId + "] Got reply: " + serverResponse);
            return serverResponse;

        } catch (Exception e) {
            System.err.println("Error in rpc_send: " + e.getMessage());
            e.printStackTrace();
            return "ERROR: Communication failed: " + e.getMessage();
        }
    }



    /**
     * Returns the coordinates of the zone as a formatted string.
     *
     * @return A string representing the coordinates of the zone in the format "(x1,y1) to (x2,y2)".
     */
    public String getZoneCoordinates() {
        return "(" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")";
    }

    /**
     * Returns a string representation of the FireIncidentSubsystem, including the zone ID and coordinates.
     *
     * @return A string representing this subsystem.
     */
    @Override
    public String toString() {
        return "FireIncidentSubsystem-Zone " + zoneId + " [" + getZoneCoordinates() + "]";
    }
}