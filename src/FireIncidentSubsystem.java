import java.io.*;
import java.net.*;
import java.util.*;

/**
 * FireIncidentSubsystem represents a subsystem responsible for handling fire events
 * for a specific zone. It reads fire events from a file, listens for incoming UDP requests,
 * and communicates with the Scheduler to add new fire events.
 * Implements Runnable to allow concurrent execution.
 */
public class FireIncidentSubsystem implements Runnable {
    public static final int DEFAULT_FIS_PORT = 5000;
    public static final int DEFAULT_SCHEDULER_PORT = 6100;
    private static int PORT;
    private final int zoneId;
    private final int x1, y1, x2, y2;
    private final InetAddress schedulerAddress;
    private DatagramSocket socket;
    private boolean running = true;
    private final int schedulerPort;
    private Thread udpListenerThread;
    private String eventFile;
    private int numEvents;

    /**
     * Constructs a new FireIncidentSubsystem.
     *
     * @param eventFile the file path containing fire events
     * @param zoneId the identifier of the zone for which this subsystem is responsible
     * @param x1 the x-coordinate of the zone's starting point
     * @param y1 the y-coordinate of the zone's starting point
     * @param x2 the x-coordinate of the zone's ending point
     * @param y2 the y-coordinate of the zone's ending point
     * @param baseOffsetport an offset to avoid port conflicts
     * @throws UnknownHostException if the local host address cannot be determined
     */
    public FireIncidentSubsystem(String eventFile, int zoneId,
                                 int x1, int y1, int x2, int y2, int baseOffsetport) throws UnknownHostException {
        this.schedulerAddress = InetAddress.getLocalHost();
        this.schedulerPort = DEFAULT_SCHEDULER_PORT;
        this.zoneId = zoneId;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        PORT = DEFAULT_FIS_PORT + zoneId + baseOffsetport;
        this.eventFile = eventFile;
        try {
            this.socket = new DatagramSocket(PORT);
            System.out.println("FireIncidentSubsystem for Zone " + zoneId + " listening on port " + PORT);
        } catch (SocketException e) {
            System.err.println("Error initializing UDP socket on port " + PORT);
            e.printStackTrace();
        }
    }

    /**
     * UDPListener is a private inner class that continuously listens for UDP packets.
     * When a packet is received, it processes the packet.
     */
    private class UDPListener implements Runnable {
        @Override
        public void run() {
            System.out.println("UDPListener running for Zone " + zoneId);
            byte[] buffer = new byte[4096];
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    processUDPPacket(packet);
                } catch (SocketException se) {
                    // Socket is closed; exit the loop.
                    System.out.println("UDPListener for Zone " + zoneId + " detected socket closure, exiting.");
                    break;
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error receiving UDP packet: " + e.getMessage());
                    }
                    // Optionally, break or continue depending on your design.
                }
            }
        }
    }

    /**
     * Processes the event file for this zone by reading each line,
     * parsing it into a FireEvent, and sending it to the Scheduler.
     *
     * @param eventFile the path to the event file
     */
    public void processEventFile(String eventFile) {
        boolean eventsAdded = false;
        System.out.println("Processing event file for Zone " + zoneId);
        try (BufferedReader reader = new BufferedReader(new FileReader(eventFile))) {
            String line;
            boolean skipHeader = true;
            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }
                FireEvent fireEvent = parseEvent(line);
                if (fireEvent.getZoneId() == zoneId) {
                    System.out.println("FireIncidentSubsystem-Zone " + zoneId + " → New Fire Event: " + fireEvent);
                    // Build a request with method name and the FireEvent object
                    List<Object> request = new ArrayList<>();
                    request.add("ADD_FIRE_EVENT");
                    request.add(fireEvent);
                    Object response = rpc_send(request, schedulerAddress, schedulerPort);
                    if (response instanceof String && ((String) response).contains("SUCCESS")) {
                        eventsAdded = true;
                    } else {
                        System.err.println("Failed to add fire event: " + response);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (eventsAdded) {
            List<Object> request = new ArrayList<>();
            request.add("SET_EVENTS_LOADED");
            request.add(zoneId);
            request.add(numEvents);
            Object response = rpc_send(request, schedulerAddress, schedulerPort);
            System.out.println("Setting events to loaded for Zone " + zoneId + ": " + response);
            System.out.println("----------------------------------------\n");
        }
    }

    /**
     * Parses a line from the event file into a FireEvent object.
     *
     * @param line a line from the event file containing event details
     * @return a FireEvent instance representing the parsed event
     */
    private FireEvent parseEvent(String line) {
        String[] slices = line.split(",");
        String time = slices[0];
        int eventZoneId = Integer.parseInt(slices[1]);
        String eventType = slices[2];
        String severity = slices[3];
        String fault = slices[4];
        return new FireEvent(time, eventZoneId, eventType, severity, fault,this);
    }

    @Override
    public synchronized void run() {
        processEventFile(eventFile);
        System.out.println(Thread.currentThread().getName() + " running for Zone " + zoneId);
        udpListenerThread = new Thread(new UDPListener());
        udpListenerThread.setName("UDPListener-Zone" + zoneId);
        udpListenerThread.setDaemon(true);  // Marking as daemon
        udpListenerThread.start();
    }

    /**
     * Processes an incoming UDP packet by reading its content, sending an ACK,
     * and printing the request details.
     *
     * @param packet the received DatagramPacket
     * @throws IOException if an I/O error occurs while processing the packet
     */
    private void processUDPPacket(DatagramPacket packet) throws IOException {
        ObjectInputStream inputStream = new ObjectInputStream(
                new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
        try {
            Object request = inputStream.readObject();
            InetAddress clientAddress = packet.getAddress();
            int clientPort = packet.getPort();
            System.out.println("[FIS-Zone " + zoneId + "] Received request: " + request +
                    " (from " + clientAddress + ":" + clientPort + ")");
            sendResponse("ACK:" + request, clientAddress, clientPort);
            // (Additional processing could be added here.)
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a response back to a client via UDP.
     *
     * @param response the response object to send
     * @param address the destination IP address
     * @param port the destination port
     * @throws IOException if an I/O error occurs while sending the response
     */
    private void sendResponse(Object response, InetAddress address, int port) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
        objStream.writeObject(response);
        objStream.flush();
        byte[] responseData = byteStream.toByteArray();
        DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, address, port);
        socket.send(responsePacket);
        System.out.println("[FIS-Zone " + zoneId + "] Sent response: " + response +
                " (to " + address + ":" + port + ")");
    }

    /**
     * Sends an RPC request as a serialized object to the Scheduler and waits for an ACK.
     *
     * @param request the request object to send
     * @param hostAddress the Scheduler's IP address
     * @param hostPort the Scheduler's port
     * @return the ACK response received from the Scheduler, or an error message if unsuccessful
     */
    public Object rpc_send(Object request, InetAddress hostAddress, int hostPort) {
        // Check if the socket is closed
        if (socket == null || socket.isClosed()) {
            return "ERROR: Socket is closed";
        }
        try {
            System.out.println("\n[Zone " + zoneId + " -> Host] Sent request: " + request);
            // Serialize the request object
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
            objStream.writeObject(request);
            objStream.flush();
            byte[] requestData = byteStream.toByteArray();

            // Send the serialized request packet
            DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, hostAddress, hostPort + zoneId);
            socket.send(requestPacket);

            // Receive ACK first
            byte[] ackBuffer = new byte[4096];
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
            socket.receive(ackPacket);
            ObjectInputStream ackStream = new ObjectInputStream(new ByteArrayInputStream(ackPacket.getData(), 0, ackPacket.getLength()));
            Object ackResponse = ackStream.readObject();
            System.out.println("[FIS-Zone " + zoneId + "] Received ACK: " + ackResponse);
            if (!(ackResponse instanceof String) || !((String) ackResponse).startsWith("ACK:")) {
                System.out.println("Unexpected response instead of ACK. Aborting.");
                return "ERROR: No ACK received";
            }
            return ackResponse;
        } catch (SocketException e) {
            System.err.println("Error in rpc_send: " + e.getMessage());
            return "ERROR: Socket is closed";
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return "ERROR: Communication failed: " + e.getMessage();
        }
    }


    /**
     * Returns the coordinates of the zone
     * @return a String representing the coordinates of the zone
     */
    public String getZoneCoordinates() {
        return "(" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")";
    }

    @Override
    public String toString() {
        return "FireIncidentSubsystem-Zone " + zoneId + " [" + getZoneCoordinates() + "]";
    }
}
