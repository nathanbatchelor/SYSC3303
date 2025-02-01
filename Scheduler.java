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
 */
public class Scheduler implements Runnable {

    private final Queue<FireEvent> queue = new LinkedList<>();
    private final Map<Integer, FireIncidentSubsystem> zones = new HashMap<>();
    private final String zoneFile;
    private final String eventFile;
    private volatile boolean isFinished = false;
    private volatile boolean isLoaded = false;
    private boolean droneStarted = false;

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
            this.isLoaded = true;
            notifyAll();
            System.out.println("Setting up drone");
        }
        if (!droneStarted) {
            droneStarted = true;
            DroneSubsystem drone = new DroneSubsystem(this);
            Thread droneSubsystem = new Thread(drone);
            droneSubsystem.setName("Drone Subsystem");
            droneSubsystem.start();
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

    /**
     * Adds a FireEvent to the queue and notifies waiting threads.
     *
     * @param event The FireEvent to add to the queue.
     */
    public synchronized void addFireEvent(FireEvent event) {
        queue.add(event);
        notifyAll();
        System.out.println("Scheduler: Added FireEvent → " + event);
    }

    /**
     * Retrieves the next FireEvent from the queue and returns it.
     * If the queue is empty, waits until a FireEvent is added or signals the system is finished.
     *
     * @return The next FireEvent in the queue, or null if processing is complete.
     */
    public synchronized FireEvent getNextFireEvent() {
        System.out.println("Queue has: " + queue);
        if (queue.isEmpty() && isLoaded) {
            System.out.println("No more events. Marking scheduler as finished");
            isFinished = true;
            notifyAll();
            return null;
        }
        while (queue.isEmpty() && !isFinished) {
            try {
                System.out.println("System is waiting for fire events to be added");
                wait();
            } catch (InterruptedException e) {
            }
        }
        return queue.peek();
    }

    /**
     * Marks a FireEvent as extinguished and removes it from the queue.
     *
     * @param event The FireEvent to mark as extinguished.
     */
    public synchronized void markFireExtinguished(FireEvent event) {
        queue.remove(event);
        System.out.println("Scheduler: Fire at Zone: " + event.getZoneId() + " Extinguished");
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
