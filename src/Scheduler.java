import java.io.*;
import java.net.*;
import java.util.*;
/**
 * The Scheduler class acts as a centralized system for handling fire events.
 * It manages incoming fire events and assigns tasks to drones.
 * This class handles zones, fire events, and communication with subsystems
 * for fire incident management.
 *
 * Implements the Runnable interface to allow it to execute in a separate thread.
 *
 * @author Joey Andrwes
 * @author Grant Phillips
 * @version 1.0
 *
 * @author Joey Andrews
 * @author Grant Phillips
 * @version 2.0
 */
public class Scheduler implements Runnable {
    private final Queue<FireEvent> queue = new LinkedList<>();
    private final Map<Integer, FireIncidentSubsystem> zones = new HashMap<>();
    private final Set<Integer> droneIds = new HashSet<>();
    private final String zoneFile;
    private final String eventFile;
    private volatile boolean isFinished = false;
    private volatile boolean isLoaded = false;
    private Boolean stopDrones = false;
    private SchedulerState state = SchedulerState.WAITING_FOR_EVENTS;
    public static final int DEFAULT_FIS_PORT = 5000;
    private final int DEFAULT_DRONE_PORT = 5500;
    private DatagramSocket dronesendSocket;
    private DatagramSocket FISsendSocket;
    private ArrayList<DatagramSocket> FIS_Sockets = new ArrayList<>();
    private ArrayList<DatagramSocket> drone_Sockets = new ArrayList<>();
    private boolean zonesLoaded = false;
    private int numEvents = 0;
    private int completedEvents = 0;

    public boolean timeoutFault = false;
    public boolean nozzleFault = false;
    public boolean packetFault = false;

    public MapUI map;
    public MetricsLogger logger;

    private int totalZonesExpected = 0;   // Set dynamically
    private int zonesFinishedLoading = 0; // Counter

    public static class DroneStatus {
        public String droneId;
        public int x;
        public int y;
        public double batteryLife;
    }

    /**
     * Enumeration representing the possible states of the Scheduler.
     */
    public enum SchedulerState {
        WAITING_FOR_EVENTS,
        ASSIGNING_DRONE,
        WAITING_FOR_DRONE,
        SHUTTING_DOWN,
    }

    /**
     * Constructs a new Scheduler.
     *
     * @param zoneFile the file path containing zone definitions
     * @param eventFile the file path containing fire events
     * @param numDrones the number of drones available for the simulation
     * @param baseOffsetport the offset to avoid port conflicts
     * @param map the MapUI instance for visual updates
     * @param logger the MetricsLogger instance for recording metrics
     */
    public Scheduler(String zoneFile, String eventFile, int numDrones, int baseOffsetport, MapUI map, MetricsLogger logger) {
        this.zoneFile = zoneFile;
        this.eventFile = eventFile;
        int droneBasePort = 6500 + baseOffsetport;
        int fisBasePort = 6100 + baseOffsetport;
        int fisSendPort = 6000 + baseOffsetport;
        int droneSendPort = 6001 + baseOffsetport;
        this.map = map;
        this.logger = logger;
        for (int i = 1; i <= numDrones; i++) {
            try {
                DatagramSocket drone_socket = new DatagramSocket(droneBasePort + i);
                drone_socket.setSoTimeout(1000);
                drone_Sockets.add(drone_socket);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            FISsendSocket = new DatagramSocket(fisSendPort);
            dronesendSocket = new DatagramSocket(droneSendPort);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        readZoneFile(fisBasePort);
    }

    /**
     * Reads the zone file to initialize zones and start corresponding FireIncidentSubsystem threads.
     *
     * @param fisBasePort the base port number for Fire Incident Subsystems
     */
    public void readZoneFile(int fisBasePort) {
        try {
            List<Zone> uiZones = new ArrayList<>();
            File file = new File(this.zoneFile);
            System.out.println("Checking path: " + file.getAbsolutePath());
            if (!file.exists()) {
                System.out.println("Zone file does not exist");
                return;
            }
            System.out.println("Attempting to read file: " + zoneFile);
            try (BufferedReader br = new BufferedReader(new FileReader(zoneFile))) {
                String line;
                boolean isFirstLine = true;
                while ((line = br.readLine()) != null) {
                    totalZonesExpected++;
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }
                    System.out.println("Reading line: " + line);
                    String[] tokens = line.split(",");
                    if (tokens.length != 3) {
                        System.out.println("Invalid Line: " + line);
                        continue;
                    }
                    try {
                        int zoneId = Integer.parseInt(tokens[0].trim());
                        int[] startCoords = parseCoordinates(tokens[1].trim());
                        int[] endCoords = parseCoordinates(tokens[2].trim());
                        if (startCoords == null || endCoords == null) {
                            System.out.println("Invalid Coordinates: " + line);
                            continue;
                        }
                        int x1 = startCoords[0], y1 = startCoords[1];
                        int x2 = endCoords[0], y2 = endCoords[1];
                        uiZones.add(new Zone(zoneId, x1, y1, x2, y2));
                        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(eventFile, zoneId, x1, y1, x2, y2, 0);
                        zones.put(zoneId, fireIncidentSubsystem);
                        DatagramSocket socket = new DatagramSocket(fisBasePort + zoneId);
                        socket.setSoTimeout(1000);
                        FIS_Sockets.add(socket);
                        Thread thread = new Thread(fireIncidentSubsystem);
                        thread.setName("Fire Incident Subsystem Zone: " + zoneId);
                        thread.start();
                    } catch (NumberFormatException e) {
                        System.out.println("Error parsing numbers in line: " + line);
                    }
                }
            }
            if (map != null) {
                //map.setZones(uiZones);
                uiZones.sort(Comparator.comparingInt(Zone::getId));
                map.setZones(uiZones);
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + zoneFile);
        }
    }

    /**
     * Marks that events have been loaded once all zones have finished loading.
     * Sorts the event queue by timestamp.
     */
    public synchronized void setEventsLoaded() {
        zonesFinishedLoading++;
        if (zonesFinishedLoading == totalZonesExpected) {
            // Sort queue by timestamp
            List<FireEvent> events = new ArrayList<>(queue);
            events.sort(Comparator.comparing(FireEvent::getTimeAsLocalTime));
            queue.clear();
            queue.addAll(events);

            isLoaded = true;
            state = SchedulerState.WAITING_FOR_DRONE;

            System.out.println("Scheduler: All zones finished. Queue sorted by timestamp. Ready to assign to drones.");
            notifyAll();
        }
    }

    /**
     * Parses a coordinate string into an integer array.
     *
     * @param coordinate the coordinate string (e.g., "(x;y)")
     * @return an array containing the x and y values, or null if parsing fails
     */

    private int[] parseCoordinates(String coordinate) {
        coordinate = coordinate.replaceAll("[()]", "");
        String[] parts = coordinate.split(";");
        if (parts.length != 2) return null;
        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            return new int[]{x, y};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Causes the scheduler to wait until fire events have been loaded.
     */
    public synchronized void waitForEvents() {
        while (!isLoaded) {
            try {
                System.out.println("Scheduler: Waiting for fire events to be loaded...");
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Adds a fire event to the scheduler's event queue.
     * Also calculates the required amount of firefighting agent for the event.
     *
     * @param event the FireEvent to add
     */
    public synchronized void addFireEvent(FireEvent event) {
        int totalWaterNeeded = calculateWaterNeeded(event.getSeverity());
        event.setLitres(totalWaterNeeded);
        queue.add(event);
        logger.recordFireDetected(event);
        notifyAll();
        System.out.println("Scheduler: Added FireEvent → " + event);
    }

    /**
     * Calculates the amount of firefighting agent needed based on event severity.
     *
     * @param severity the severity of the fire event (e.g., low, moderate, high)
     * @return the required amount of agent in liters
     */
    private int calculateWaterNeeded(String severity) {
        return switch (severity.toLowerCase()) {
            case "low" -> 10;
            case "moderate" -> 20;
            case "high" -> 30;
            default -> 0;
        };
    }

    /**
     * Calculates the travel time from a drone's current position to the fire event.
     *
     * @param xDrone the drone's current x-coordinate
     * @param yDrone the drone's current y-coordinate
     * @param event the target fire event
     * @return the estimated travel time in seconds
     */
    public double calculateTravelTime(int xDrone, int yDrone, FireEvent event) {
        int cruiseSpeed = 18;
        System.out.println("Calculating travel time");
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");

        int x1 = Integer.parseInt(startCoords[0].trim());
        int y1 = Integer.parseInt(startCoords[1].trim());
        int x2 = Integer.parseInt(endCoords[0].trim());
        int y2 = Integer.parseInt(endCoords[1].trim());

        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;
        double distance = Math.sqrt(Math.pow(centerX - xDrone, 2) + Math.pow(centerY - yDrone, 2));
        double travelTimeToFire = distance / cruiseSpeed;
        System.out.println("\nScheduler: Travel time to fire: " + travelTimeToFire);
        return travelTimeToFire;
    }

    /**
     * Calculates the distance from the fire event's zone center to the home base (assumed at 0,0).
     *
     * @param event the fire event
     * @return the distance to home base in meters
     */
    public double calculateDistanceToHomeBase(FireEvent event) {
        int homeBaseX = 0;
        int homeBaseY = 0;
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");
        int x1 = Integer.parseInt(startCoords[0].trim());
        int y1 = Integer.parseInt(startCoords[1].trim());
        int x2 = Integer.parseInt(endCoords[0].trim());
        int y2 = Integer.parseInt(endCoords[1].trim());
        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;
        double distanceToHomeBase = Math.sqrt(Math.pow(centerX - homeBaseX, 2) + Math.pow(centerY - homeBaseY, 2));
        System.out.println("\nScheduler: Distance to home base is: " + distanceToHomeBase + " meters\n" +
                "Scheduler: Time to Home Base is: " + distanceToHomeBase / 18 + " seconds\n");
        return distanceToHomeBase;
    }

    /**
     * Returns the next fire event that is within a specified threshold of the drone's current position.
     *
     * @param droneId the identifier of the drone (as a String)
     * @param currentX the drone's current x-coordinate
     * @param currentY the drone's current y-coordinate
     * @return a FireEvent within range, or null if none is found
     */
    public synchronized FireEvent getNextAssignedEvent(String droneId, int currentX, int currentY) {
        double threshold = 50; // meters
        if (queue.isEmpty()) return null;
        for (FireEvent event : queue) {
            int[] center = calculateZoneCenter(event);
            double distance = Math.sqrt(Math.pow(center[0] - currentX, 2) + Math.pow(center[1] - currentY, 2));
            if (distance <= threshold) {
                queue.remove(event);
                return event;
            }
        }
//        if (!queue.isEmpty()) {
//            return queue.poll();
//        }
        return null;
    }

    /**
     * Calculates the center coordinates of a fire event's zone.
     *
     * @param event the fire event
     * @return an array containing the x and y coordinates of the zone center
     */
    private int[] calculateZoneCenter(FireEvent event) {
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");
        int centerX = (Integer.parseInt(startCoords[0].trim()) + Integer.parseInt(endCoords[0].trim())) / 2;
        int centerY = (Integer.parseInt(startCoords[1].trim()) + Integer.parseInt(endCoords[1].trim())) / 2;
        return new int[]{centerX, centerY};
    }

    /**
     * Retrieves the next fire event from the queue.
     * If no events remain and the simulation is finished, it signals drones to stop.
     *
     * @return the next FireEvent, or null if finished
     */
    public synchronized FireEvent getNextFireEvent() {
        while(completedEvents == numEvents){
            if (isFinished) {
                System.out.println("Scheduler: No more fire events. Notifying all waiting drones to stop.");
                stopDrones = true;
                notifyAll();
                return null;
            }
            try {
                System.out.println("Scheduler: Waiting for fire events to be added...");
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        FireEvent event = queue.poll();

        if (event != null) {
            System.out.println("Scheduler: Sending fire event to drone: " + event);
        }

        // If the queue is now empty, and no more are expected, mark finished
        if (queue.isEmpty()) {
            isFinished = true;
//            stopDrones = true;
            notifyAll();
        }
        return event;
    }

    /**
     * Retrieves an additional fire event for a drone based on battery life and current position.
     *
     * @param batteryLife the remaining battery life of the drone
     * @param x the drone's current x-coordinate
     * @param y the drone's current y-coordinate
     * @return a suitable FireEvent, or null if none is found
     */
    public synchronized FireEvent getAdditionalFireEvent(double batteryLife, int x, int y) {
        for (FireEvent currentEvent : queue) {
            double range = calculateTravelTime(x, y, currentEvent);
            double travelToHome = calculateDistanceToHomeBase(currentEvent);
            if (range + travelToHome < batteryLife) {
                System.out.println("\nSending new event to the drone\n");
                queue.remove(currentEvent);
                return currentEvent;
            }
        }
        return null;
    }

    /**
     * Updates the fire event's status based on the amount of firefighting agent dropped.
     * If the event still requires more agent, it is re-added to the queue.
     *
     * @param event the fire event to update
     * @param waterDropped the amount of agent dropped in liters
     */
    public synchronized void updateFireStatus(FireEvent event, int waterDropped) {
        event.removeLitres(waterDropped);
        int remainingLiters = event.getLitres();
        if (remainingLiters > 0 && waterDropped > 0) {
            System.out.println("Scheduler: Fire at Zone: " + event.getZoneId() + " still needs " + remainingLiters + "L.");
            queue.add(event);
            notifyAll();
        } else {
            markFireExtinguished(event);
            completedEvents++;
        }
    }

    /**
     * Returns the map of zones (fire incident subsystems) managed by the scheduler.
     *
     * @return a Map with zone IDs as keys and FireIncidentSubsystem instances as values
     */
    public Map<Integer, FireIncidentSubsystem> getZones() {
        return zones;
    }

    /**
     * Marks a fire event as extinguished, updates the map display, and logs the event.
     *
     * @param event the fire event to mark as extinguished
     */
    public synchronized void markFireExtinguished(FireEvent event) {
        System.out.println("\nScheduler: Fire at Zone: " + event.getZoneId() + " Extinguished\n");
        map.drawFireEvents(event);
        event.setCurrentState(FireEvent.FireEventState.INACTIVE);
        //logger.recordFireExtinguished(event);
        if (queue.isEmpty()) {
            state = SchedulerState.SHUTTING_DOWN;
            isFinished = true;
            notifyAll();
        }
    }

    /**
     * Handles a drone fault by re-adding the corresponding fire event to the queue.
     *
     * @param event the fire event related to the fault
     * @param type the type of fault (e.g., "ARRIVAL", "NOZZLE", "PACKET_LOSS")
     * @param idnum the identifier of the drone that experienced the fault
     */
    public synchronized void handleDroneFault(FireEvent event, String type,int idnum){
        if (event.getFault().equals("ARRIVAL")){
            System.out.println("\u001B[33m !!!!Scheduler: handling drone TRAVEL TIMEOUT!!!! \u001B[0m");
            timeoutFault = true;
        } else if (event.getFault().equals("NOZZLE")) {
            System.out.println("\u001B[33m !!!!Scheduler: handling drone NOZZLE failure!!!! \u001B[0m");
            nozzleFault = true;
        } else if (event.getFault().equals("PACKET_LOSS")) {
            System.out.println("\u001B[33m !!!!Scheduler: handling drone PACKET_LOSS failure!!!! \u001B[0m");
            packetFault = true;
        }

        event.remFault();
        queue.add(event);
    }

    @Override
    public synchronized void run() {
        byte[] buffer = new byte[4096];
        ArrayList<String> knownFISMethods = new ArrayList<>(Arrays.asList("ADD_FIRE_EVENT", "SET_EVENTS_LOADED"));
        ArrayList<String> knowndroneMethods = new ArrayList<>(Arrays.asList(
                "getNextAssignedEvent", "ADD_FIRE_EVENT", "calculateDistanceToHomeBase",
                "getNextFireEvent", "calculateTravelTime", "updateFireStatus", "getAdditionalFireEvent","handleDroneFault", "STOP_?"));
        try {
            FISsendSocket.setSoTimeout(1000);
            dronesendSocket.setSoTimeout(1000);
        } catch (SocketException e) {
            System.out.println("Error in scheduler run");
            throw new RuntimeException(e);
        }

        // Variables for graceful shutdown after finishing events
        long lastRequestTime = System.currentTimeMillis();
        final long GRACE_PERIOD_MS = 30000; // e.g., 30 seconds

        while (true) {
            boolean messageProcessed = false;
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Process messages from FIS sockets
                for (DatagramSocket FIS_Socket : FIS_Sockets) {
                    try {
                        FIS_Socket.receive(packet);
                        lastRequestTime = System.currentTimeMillis();
                        ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                        ObjectInputStream objStream = new ObjectInputStream(byteStream);
                        Object obj = objStream.readObject();
                        if (obj instanceof List) {
                            List<Object> list = (List<Object>) obj;
                            if (knownFISMethods.contains(list.get(0))) {
                                System.out.println("Calling invokeMethod for FIS");
                                invokeMethod((String) list.get(0), list.subList(1, list.size()), true);
                                messageProcessed = true;
                            }
                        }
                    } catch (Exception e) {
                    }
                }

                // Process messages from Drone sockets
                for (DatagramSocket drone_Socket : drone_Sockets) {
                    try {
                        drone_Socket.receive(packet);
                        lastRequestTime = System.currentTimeMillis();
                        ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                        ObjectInputStream objStream = new ObjectInputStream(byteStream);
                        Object obj = objStream.readObject();
                        if (obj instanceof List) {
                            List<Object> list = (List<Object>) obj;
                            if (knowndroneMethods.contains(list.get(0))) {
                                invokeMethod((String) list.get(0), list.subList(1, list.size()), false);
                                messageProcessed = true;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception in scheduler run");
                throw new RuntimeException(e);
            }


            if (!messageProcessed) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Invokes the appropriate method based on the provided method name and parameters.
     * This method acts as a centralized RPC handler, processing various requests such as
     * adding fire events, retrieving events, updating statuses, and handling drone faults.
     *
     * @param methodName the name of the method to invoke
     * @param params a list of parameters required by the method
     * @param from a boolean flag indicating the source of the request (true if from the Fire Incident Subsystem, false if from a drone)
     * @return a result object from the invoked method, or a status string indicating success or failure
     */
    private Object invokeMethod(String methodName, List<Object> params, boolean from) {
        switch (methodName) {
            case "ADD_FIRE_EVENT": {
                this.numEvents++;
                FireEvent event = (FireEvent) params.get(0);

                map.drawFireEvents(event);

                System.out.println("Received event: " + event);
                addFireEvent(event);
                if (from) {
                    FISRPCSend("ACK:", (Integer) params.get(0));
                    FISRPCSend("SUCCESS", (Integer) params.get(0));
                } else {
                    int droneId = (Integer) params.get(params.size() - 1);
                    droneRPCSend("ACK:done", droneId);
                }
                break;
            }
            case "getNextAssignedEvent": {
                String droneId = (String) params.get(0);
                int currentX = (Integer) params.get(1);
                int currentY = (Integer) params.get(2);
                FireEvent event = getNextAssignedEvent(droneId, currentX, currentY);
                int droneId2 = (Integer) params.get(params.size() - 1);
                droneRPCSend(event, droneId2);
                break;
            }
            case "calculateDistanceToHomeBase": {
                FireEvent event = (FireEvent) params.get(0);
                double distance = calculateDistanceToHomeBase(event);
                int droneId = (Integer) params.get(params.size() - 1);
                droneRPCSend(distance, droneId);
                break;
            }
            case "getNextFireEvent": {
                System.out.println("Sending drone the event");
                FireEvent event = getNextFireEvent();
                int droneId = (Integer) params.get(params.size() - 1);
                droneRPCSend(event, droneId);
                break;
            }
            case "calculateTravelTime": {
                int x = (Integer) params.get(0);
                int y = (Integer) params.get(1);

                FireEvent event = (FireEvent) params.get(2);
                double travelTime = calculateTravelTime(x, y, event);
                int droneId = (Integer) params.get(params.size() - 1);
                droneRPCSend(travelTime, droneId);
                break;
            }
            case "updateFireStatus": {
                FireEvent event = (FireEvent) params.get(0);
                int waterDropped = (Integer) params.get(1);
                updateFireStatus(event, waterDropped);
                int droneId = (Integer) params.get(params.size() - 1);
                droneRPCSend("ACK:done", droneId);
                break;
            }
            case "getAdditionalFireEvent": {
                FireEvent event = getAdditionalFireEvent(
                        (Double) params.get(0),
                        (Integer) params.get(1),
                        (Integer) params.get(2)
                );
                int droneId = (Integer) params.get(params.size() - 1);
                droneRPCSend(event, droneId);
                break;
            }
            case "SET_EVENTS_LOADED": {
                setEventsLoaded();
                FISRPCSend("ACK:", (Integer) params.get(0));
                FISRPCSend("SUCCESS", (Integer) params.get(0));
                break;
            }
            case "handleDroneFault": {
                int droneId = (Integer) params.get(2);
                handleDroneFault((FireEvent) params.get(0), (String) params.get(1), droneId);
                droneRPCSend("ACK:done", droneId);
                break;
            }
            case "STOP_?": {
                boolean stop = isStopDrones();
                int droneId = (Integer) params.get(params.size() - 1);
                droneRPCSend(stop, droneId);
                break;
            }
            default:
                System.out.println("Unknown method: " + methodName);
                return "FAILED";
        }
        return "???";
    }

    /**
     * Sends an RPC response to a drone with the specified ID.
     * This method serializes the response object and transmits it via UDP
     * to the drone listening on the designated port.
     *
     * @param response the object to send as the response (cannot be null)
     * @param idnum the identifier of the target drone
     */
    public synchronized void droneRPCSend(Object response, int idnum) {
        try {
            if (response!=null){
                System.out.println("Sending response to drone " + idnum + ": " + response);
            }
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
            objStream.writeObject(response);
            objStream.flush();
            byte[] responseData = byteStream.toByteArray();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, InetAddress.getLocalHost(), DEFAULT_DRONE_PORT + idnum);
            dronesendSocket.send(responsePacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends an RPC message to the Fire Incident Subsystem (FIS) for a given zone.
     * This method serializes the provided message and sends it via UDP to the
     * FIS listening on a port determined by the zone number.
     *
     * @param message the message object to be sent
     * @param zone the zone number representing the target FIS
     */
    public synchronized void FISRPCSend(Object message, int zone) {
        try {
            System.out.println("Sending FIS message to zone " + zone + ": " + message);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
            objStream.writeObject(message);
            objStream.flush();
            byte[] responseData = byteStream.toByteArray();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, InetAddress.getLocalHost(), DEFAULT_FIS_PORT + zone);
            FISsendSocket.send(responsePacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks whether the stop flag for drones is set.
     *
     * @return true if the drones are signaled to stop, false otherwise
     */
    public boolean isStopDrones() {
        return stopDrones;
    }
}
