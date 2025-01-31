import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * The Scheduler class acts as a central system for handling fire events.
 * It manages incoming fire events and assigns tasks to drones
 *
 * @author Joey Andrwes
 * @author Grant Phillips
 *
 * @version 1.0
 */

public class Scheduler implements Runnable {

    private final Queue<FireEvent> queue = new LinkedList<>();
    private final Map<Integer, FireIncidentSubsystem> zones = new HashMap<>();
    private final String zoneFile;
    private final String eventFile;
    private volatile boolean isFinished = false;
    private volatile boolean isLoaded = false;

    /**
     * Constructs a Scheduler object with specified zone and event files
     *
     *
     * @param zoneFile The file containing zone information
     * @param eventFile The file containing fire event information
     */
    public Scheduler (String zoneFile, String eventFile) {
        this.zoneFile = zoneFile;
        this.eventFile = eventFile;
        readZoneFile();
    }


    /**
     * Reads the zone files to initialize FireIncidentResponse for each zone
     * It also starts the respective threads for handling fire events
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
                    // Skip header row
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }
                    // Debugging: Log the line being read
                    System.out.println("Reading line: " + line);

                    String[] tokens = line.split(",");
                    if (tokens.length != 3) {  // Adjusted for new format (ID, Start, End)
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

                        // Create and start FireIncidentSubsystem
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
     * Signals that all events have been loaded and starts Drone Subsystem
     */
    public synchronized void setEventsLoaded() {
        this.isLoaded = true;
        notifyAll();
        System.out.println("Setting up drone");
        DroneSubsystem drone = new DroneSubsystem(this);
        Thread droneSubsystem = new Thread(drone);
        droneSubsystem.setName("Drone Subsystem");
        droneSubsystem.start();
    }

    /**
     * Parses a string of coordinates from the Zone file into an integer array
     *
     * @param coordinate The coordinate string in format (x;y)
     * @return An array containing the x and y values
     */
    private int[] parseCoordinates(String coordinate) {
        coordinate = coordinate.replaceAll("[()]", ""); // Remove parentheses
        String[] parts = coordinate.split(";");
        if (parts.length != 2) return null;  // Invalid format

        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            return new int[]{x, y};
        } catch (NumberFormatException e) {
            return null;  // Parsing failed
        }
    }


    /**
     * Adds a FireEvent to the queue and notfies
     * @param event
     */
    public synchronized void addFireEvent(FireEvent event) {
        queue.add(event);
        notifyAll();
        System.out.println("Scheduler: Added FireEvent → " + event);
    }

    /**
     * Returns the next FireEvent in the
     *
     * @return The first FireEvent in the queue
     */
    // Would DS call: scheduler.getNextFireEvent();
    public synchronized FireEvent getNextFireEvent() {
        System.out.println("Queue has: " + queue);
        if(queue.isEmpty() && isLoaded) {
            System.out.println("No more events. Marking scheduler as finished");
            isFinished = true;
            notifyAll();
            return null;
        }
        while (queue.isEmpty() && !isFinished) {
            try {
                System.out.println("System is waiting for fire events to be added");
                wait();
            } catch (InterruptedException e) {}
        }
        return queue.peek();
    }

    /**
     *
     *
     * @param event
     */
    public synchronized void markFireExtinguished(FireEvent event) {
        queue.remove(event);
        System.out.println("Scheduler: Fire at Zone: " + event.getZoneId() + " Extinguished");
    }

    public synchronized void editFireEvent(FireEvent event, int litres) {
        event.removeLitres(litres);
    }

    // Signal when all fires from input file are finished?
    public synchronized void finish() {
        isFinished = true;
        notifyAll();
    }


    // Called when thread is finished running
    public synchronized boolean isFinished() {
        return isFinished;
    }

    // Not utilised in iteration #1
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
