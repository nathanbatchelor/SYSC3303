import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

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
    public Scheduler(String zoneFile, String eventFile) {
        this.zoneFile = zoneFile;
        this.eventFile = eventFile;
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

                        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(this, eventFile, zoneId, x1, y1, x2, y2);
                        zones.put(zoneId, fireIncidentSubsystem);
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
        while (!isFinished) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
