import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeoutException;

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

// When giving an Event to a drone, figure out what zones it goes through
// When drone gets to each zone, have it check if there is a fire that meets criteria there
// If so drone swaps its event with the one at current zone, and places its old event at the front of queue
//


// Edit getNextFireEvent method to check if the drone is within a threshold and check severity
// But we need to get that event back and go to the original event if anything happened.
//
//




public class Scheduler implements Runnable {

    private final Queue<FireEvent> queue = new LinkedList<>();
    private final Map<Integer, FireIncidentSubsystem> zones = new HashMap<>();
    private final String zoneFile;
    private final String eventFile;
    private volatile boolean isFinished = false;
    private volatile boolean isLoaded = false;
    private boolean droneStarted = false;
    private SchedulerState state = SchedulerState.WAITING_FOR_EVENTS; // Default State
    public static final int DEFAULT_FIS_PORT = 5000;
    private final int DEFAULT_DRONE_PORT = 5500;
    private DatagramSocket dronesendSocket;
    private DatagramSocket FISsendSocket;
    private ArrayList<DatagramSocket> FIS_Sockets= new ArrayList<DatagramSocket>();
    private ArrayList<DatagramSocket> drone_Sockets = new ArrayList<DatagramSocket>();
    public static class DroneStatus {
        public String droneId;
        public int x;
        public int y;
        public double batteryLife;
    }

    public enum SchedulerState {
        WAITING_FOR_EVENTS,
        ASSIGNING_DRONE,
        WAITING_FOR_DRONE,
        SHUTTING_DOWN,
    }

    /**
     * Constructs a Scheduler object with specified zone and event files.
     *
     * @param zoneFile  The file containing zone information.
     * @param eventFile The file containing fire event information.
     */
    public Scheduler(String zoneFile, String eventFile,int numDrones){
        this.zoneFile = zoneFile;
        this.eventFile = eventFile;
        for (int i = 1; i <= numDrones; i++) {
            try {
                DatagramSocket drone_socket = new DatagramSocket(6500+i);
                drone_socket.setSoTimeout(1000);
                drone_Sockets.add(drone_socket);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            FISsendSocket=new DatagramSocket(6000);
            dronesendSocket=new DatagramSocket(6001);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        readZoneFile();
    }

    /**
     * Reads the zone file to initialize FireIncidentSubsystems for each zone.
     * Starts a thread for each zone to handle fire events within it.
     */
    public void readZoneFile() {
        try {
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
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue; // Skip header row
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

                        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(eventFile, zoneId, x1, y1, x2, y2);
                        zones.put(zoneId, fireIncidentSubsystem);
                        DatagramSocket socket = new DatagramSocket(6100+zoneId);//give individual listeners for each zoneid to prevent overwriting
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
        } catch (IOException e) {
            System.out.println("Error reading file: " + zoneFile);
        }
    }

    /**
     * Signals that all events have been loaded and initializes the DroneSubsystem.
     * Starts the DroneSubsystem in a new thread.
     */
    public synchronized void setEventsLoaded() {
        if (!isLoaded) {
            isLoaded = true;
            state = SchedulerState.WAITING_FOR_DRONE;
            System.out.println("Scheduler: Fire events are loaded. Notifying waiting drones...");
            notifyAll(); // Wake up all waiting threads (Drone)
        }
    }


    /**
     * Parses a string of coordinates from the zone file into an integer array.
     *
     * @param coordinate The coordinate string in format (x;y).
     * @return An integer array containing the x and y values.
     * Returns null if the format is invalid or parsing fails.
     */
    private int[] parseCoordinates(String coordinate) {
        coordinate = coordinate.replaceAll("[()]", ""); // Remove parentheses
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

    public synchronized void waitForEvents() {
        while (!isLoaded) {
            try {
                System.out.println("Scheduler: Waiting for fire events to be loaded...");
                wait(); // Wait until events are available
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    /**
     * Adds a FireEvent to the queue and notifies waiting threads.
     *
     * @param event The FireEvent to add to the queue.
     */
    public synchronized void addFireEvent(FireEvent event) {
        int totalWaterNeeded = calculateWaterNeeded(event.getSeverity());
        event.setLitres(totalWaterNeeded);
        queue.add(event);
        notifyAll();
        System.out.println("Scheduler: Added FireEvent → " + event);
    }

    /**
     * Calculates the amount of water needed to extinguish a fire based on fire severity.
     *
     * @param severity the severity level of the fire (e.g., "low", "moderate", "high").
     * @return the amount of water needed in liters.
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
     * Simulates the drone traveling to the center of the fire zone.
     *
     * @param event the FireEvent object containing details about the fire zone.
     */
    public double calculateTravelTime(int xDrone, int yDrone, FireEvent event) {
        int cruiseSpeed = 18;

        // Extract zone coordinates from the FireEvent
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");

        // Parse the coordinates
        int x1 = Integer.parseInt(startCoords[0].trim());
        int y1 = Integer.parseInt(startCoords[1].trim());
        int x2 = Integer.parseInt(endCoords[0].trim());
        int y2 = Integer.parseInt(endCoords[1].trim());

        // Calculate the center of the fire zone
        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;

        // Calculate the distance from the drone's position to the fire zone center
        double distance = Math.sqrt(Math.pow(centerX - xDrone, 2) + Math.pow(centerY - yDrone, 2));

        // Calculate travel time based on cruise speed
        double travelTimeToFire = distance / cruiseSpeed;

        System.out.println("\nScheduler: Travel time to fire: " + travelTimeToFire);
        return travelTimeToFire;
    }

    /**
     * Calculates the Euclidean distance from the home base to the center of the fire zone.
     *
     * @param event The FireEvent containing the zone details where the fire is located.
     * @return The distance in meters from the home base to the center of the fire zone.
     */
    public double calculateDistanceToHomeBase(FireEvent event) {
        // Home base coordinates
        int homeBaseX = 0;
        int homeBaseY = 0;

        // Extract zone coordinates from the FireEvent
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");

        // Parse the coordinates
        int x1 = Integer.parseInt(startCoords[0].trim());
        int y1 = Integer.parseInt(startCoords[1].trim());
        int x2 = Integer.parseInt(endCoords[0].trim());
        int y2 = Integer.parseInt(endCoords[1].trim());

        // Calculate the center of the fire zone
        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;

        // Calculate the distance from the center of the zone to home base
        double distanceToHomeBase = Math.sqrt(Math.pow(centerX - homeBaseX, 2) + Math.pow(centerY - homeBaseY, 2));
        System.out.println("\nScheduler: Distance to home base is: " + distanceToHomeBase + " meters\n" + "Scheduler: Time to Home Base is: " + distanceToHomeBase/18 + " seconds\n");

        return distanceToHomeBase;
    }


    /**
     * This method is called by a drone when it is ready for a new event.
     * It first checks the event queue to see if any event’s zone center is close enough
     * (i.e. "on route") to the drone’s current position. If so, that event is returned.
     * Otherwise, the first event in the queue is returned.
     */
    public synchronized FireEvent getNextAssignedEvent(String droneId, int currentX, int currentY) {
        double threshold = 50.0; // Threshold distance in meters for "on route" events.
        for (FireEvent event : queue) {
            int[] center = calculateZoneCenter(event);
            double distance = Math.sqrt(Math.pow(center[0] - currentX, 2) + Math.pow(center[1] - currentY, 2));
            if (distance <= threshold) {
                queue.remove(event);
                return event;
            }
        }
        if (!queue.isEmpty()) {
            return queue.poll();
        }
        return null;
    }

    // Helper: Calculate the center coordinates of the fire zone.
    private int[] calculateZoneCenter(FireEvent event) {
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");
        int centerX = (Integer.parseInt(startCoords[0].trim()) + Integer.parseInt(endCoords[0].trim())) / 2;
        int centerY = (Integer.parseInt(startCoords[1].trim()) + Integer.parseInt(endCoords[1].trim())) / 2;
        return new int[]{centerX, centerY};
    }
    /**
     * Retrieves the next FireEvent from the queue for processing.
     * If the queue is empty, the method waits until a new event is added.
     * If no more fire events are expected (isFinished is true), it notifies all waiting threads and returns null.
     *
     * @return The next FireEvent to be processed, or null if no more events remain.
     */
    public synchronized FireEvent getNextFireEvent() {
        while (queue.isEmpty()) {
            if (isFinished) {
                System.out.println("Scheduler: No more fire events. Notifying all waiting drones to stop.");
                notifyAll();  // Notify all waiting threads (drones) to exit
                return null;
            }
            try {
                System.out.println("Scheduler: Waiting for fire events to be added...");
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } // peek for multiple drones?
        return queue.poll();
    }


    /**
     * Retrieves the next FireEvent from the queue and returns it.
     * If the queue is empty, waits until a FireEvent is added or signals the system is finished.
     *
     * @return The next FireEvent in the queue, or null if processing is complete.
     */
    public synchronized FireEvent getAdditionalFireEvent(double batteryLife,int x,int y) {
        // Process the queue to find a suitable fire event
        for (FireEvent currentEvent : queue) {
            double range = calculateTravelTime(x, y, currentEvent);
            double travelToHome = calculateDistanceToHomeBase(currentEvent);

            // Check if the event satisfies the condition
            if (range + travelToHome < batteryLife) {
                System.out.println("\nSending new event to the drone\n");
                queue.remove(currentEvent); // Remove the event from the queue
                return currentEvent;       // Return the event
            }
        }

        return null;
    }




    /**
     * Updates the status of a fire event after water has been dropped.
     * If the fire still requires more water, it is re-added to the front of the queue.
     * If the fire is extinguished, it marks the event as completed.
     *
     * @param event The FireEvent being updated.
     * @param waterDropped The amount of water (in liters) dropped on the fire.
     */
    public synchronized void updateFireStatus(FireEvent event, int waterDropped) {
        event.removeLitres(waterDropped);
        int remainingLiters = event.getLitres();

        if (remainingLiters > 0 && waterDropped > 0) {
            // Only re-add the event to the queue if it still needs to be extinguished
            System.out.println("Scheduler: Fire at Zone: " + event.getZoneId() + " still needs " + remainingLiters + "L.");
            ((LinkedList<FireEvent>) queue).addFirst(event);
            notifyAll();
        } else {
            markFireExtinguished(event);
        }
    }



    /**
     * Marks a FireEvent as extinguished and removes it from the queue.
     *
     * @param event The FireEvent to mark as extinguished.
     */
    public synchronized void markFireExtinguished(FireEvent event) {
        //queue.remove(event);
        System.out.println("\nScheduler: Fire at Zone: " + event.getZoneId() + " Extinguished\n");

        if (queue.isEmpty()) {
            System.out.println("Scheduler: All fires events have been marked as extinguished. Shutting down.");
            state = SchedulerState.SHUTTING_DOWN;
            isFinished = true;
            notifyAll();
        }
    }


    public synchronized void removeFireEvent(FireEvent event) {
        queue.remove(event);
    }

    /**
     * Edits an existing FireEvent to update the litres needed.
     *
     * @param event  The FireEvent to edit.
     * @param litres The amount of litres to remove from the event's total.
     */
    public synchronized void editFireEvent(FireEvent event, int litres) {
        event.removeLitres(litres);
    }

    /**
     * Signals that all processing is complete and notifies any waiting threads.
     */
    public synchronized void finish() {
        isFinished = true;
        notifyAll();
    }

    /**
     * Checks if the Scheduler has finished processing.
     *
     * @return true if the Scheduler has finished; false otherwise.
     */
    public synchronized boolean isFinished() {
        return isFinished;
    }

    public synchronized boolean isEventsLoaded() {
        return isLoaded;
    }

    /**
     * The main run method for the Scheduler thread.
     * Waits in a loop until the system is marked as finished.
     */
    @Override
    public synchronized void run() {
        byte[] buffer = new byte[1024];
        ArrayList<String> knownFISMethods = new ArrayList<String>(Arrays.asList("ADD_FIRE_EVENT"));
        ArrayList<String> knowndroneMethods = new ArrayList<String>(Arrays.asList("getNextAssignedEvent","ADD_FIRE_EVENT","calculateDistanceToHomeBase","getNextFireEvent","calculateTravelTime","updateFireStatus","getAdditionalFireEvent"));
        try {
            FISsendSocket.setSoTimeout(1000);
            dronesendSocket.setSoTimeout(1000);
        } catch (SocketException e) {
            System.out.println("error in scheduler run");
            throw new RuntimeException(e);
        }
        while (!isFinished) {
            try {
                //FIS check segment
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                for (DatagramSocket FIS_Socket:FIS_Sockets) {
                    try {
                        FIS_Socket.receive(packet);
                        String message = new String(buffer, 0, packet.getLength());
                        ArrayList<String> elements = new ArrayList<>(List.of(message.split(":")));
                        if (knownFISMethods.contains(elements.get(0))){
                            System.out.println("calling invokemethod");
                            invokeMethod(elements.get(0),elements.subList(1,elements.size()),true);
                        }
                    }catch (Exception e){
                        System.out.println("timout in FIS socket");
                    }
                }

                //Drone check segment
                for (DatagramSocket drone_Socket:drone_Sockets) {
                    try {
                        drone_Socket.receive(packet);
                        ArrayList<String> list = null;
                        ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData());
                        ObjectInputStream objStream = new ObjectInputStream(byteStream);
                        Object obj = objStream.readObject();
                        if (obj instanceof ArrayList) {
                            list = (ArrayList<String>) obj;
                            System.out.println("Deserialized List: " + list);
                        } else {
                            System.out.println("Deserialized object is not an ArrayList.");
                        }
                        if (knowndroneMethods.contains(list.get(0))){
                            System.out.println("calling invokemethod");
                            invokeMethod(list.get(0),list.subList(1,list.size()),false);
                        }
                    }catch (Exception e){
                        System.out.println("timout in drone socket");
                    }
                }


            } catch (Exception e) {
                System.out.println("exeption in scheduler run");
                throw new RuntimeException(e);
            }



            /*try {
                //wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
        }
        System.out.println("schedluler left run loop");
    }

    private synchronized String invokeMethod(String methodName,List<String> params,boolean from){
        // Remove the method name from params
        System.out.println(methodName);
        if (methodName.equals("ADD_FIRE_EVENT")){
            FireEvent event = new FireEvent(params.get(0) + ":" + params.get(1) + ":" +params.get(2),Integer.parseInt(params.get(3)),params.get(4),params.get(5),zones.get(Integer.parseInt(params.get(3))));
            System.out.println(event);
            addFireEvent(event);
            System.out.println("queue---------------------------------------------" +queue);
            if (from) {
                try {
                    FISRPCSend("ACK:SUCCESS", Integer.parseInt(params.get(3)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }else
                droneRPCSend("ACK:done",Integer.parseInt(params.get(6)));
            return "ACK:SUCCESS";
        } else if (methodName.equals("getNextAssignedEvent")) {
            droneRPCSend(getNextAssignedEvent(params.get(0),Integer.parseInt(params.get(1)),Integer.parseInt(params.get(2))).toString(),Integer.parseInt(params.get(0)));
        } else if (methodName.equals("calculateDistanceToHomeBase")) {
            FireEvent event = new FireEvent(params.get(0) + ":" + params.get(1) + ":" +params.get(2),Integer.parseInt(params.get(3)),params.get(4),params.get(5),zones.get(Integer.parseInt(params.get(3))));
            droneRPCSend(String.valueOf(calculateDistanceToHomeBase(event)),Integer.parseInt(params.get(6)));
        } else if (methodName.equals("getNextFireEvent")) {
            droneRPCSend(String.valueOf(getNextFireEvent()),Integer.parseInt(params.get(0)));
        } else if (methodName.equals("calculateTravelTime")) {
            FireEvent event = new FireEvent(params.get(2) + ":" + params.get(3) + ":" +params.get(4),Integer.parseInt(params.get(5)),params.get(6),params.get(7),zones.get(Integer.parseInt(params.get(5))));
            droneRPCSend(String.valueOf(calculateTravelTime(Integer.parseInt(params.get(0)),Integer.parseInt(params.get(1)),event)), Integer.parseInt(params.get(8)));
        } else if (methodName.equals("updateFireStatus")){
            FireEvent event = new FireEvent(params.get(0) + ":" + params.get(1) + ":" +params.get(2),Integer.parseInt(params.get(3)),params.get(4),params.get(5),zones.get(Integer.parseInt(params.get(3))));
            updateFireStatus(event,Integer.parseInt(params.get(6)));
            droneRPCSend("ACK:done",Integer.parseInt(params.get(6)));
        } else if (methodName.equals("getAdditionalFireEvent")){
            droneRPCSend(getAdditionalFireEvent(Double.valueOf(params.get(0)),Integer.valueOf(params.get(1)),Integer.valueOf(params.get(2))).toString(),Integer.parseInt(params.get(3)));
        }
        System.out.println("invokemethod Failed-----------------------------------");
        return "FAILED";
    }


    public synchronized void droneRPCSend(String message,int idnum){
        System.out.println("entered drone send------------------------------------------------------");
    }
    public synchronized void FISRPCSend(String message,int zone) throws IOException {
        byte[] responseData = message.getBytes();
        System.out.println("sending " + message + " to " + zone );
        DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, InetAddress.getLocalHost(), DEFAULT_FIS_PORT+zone);
        FISsendSocket.send(responsePacket);
    }
}