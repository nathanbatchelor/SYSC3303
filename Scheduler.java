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

    // Queue to hold FireEvents
    private final Queue<FireEvent> queue = new LinkedList<>();
    // Something to store zones
    private final Map<Integer, FireIncidentSubsystem> zones = new HashMap<>();
    // File for zones
    private final String zoneFile;
    // File for Events - To be passed to FIS
    private final String eventFile;

    private volatile boolean isFinished = false;

    public Scheduler (String zoneFile, String eventFile) {
       // Future location of drone & FIS objects
        this.zoneFile = zoneFile;
        this.eventFile = eventFile;
        readZoneFile();
    }

    // Add event file here, pass through to FIS
    public void readZoneFile() {
        try {
            File file = new File(zoneFile);
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

    private int[] parseCoordinates(String coordinate) {
        coordinate = coordinate.replaceAll("[()]", ""); // Remove parentheses
        String[] parts = coordinate.split(";");
        if (parts.length != 2) return null;  // Invalid format

        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            return new int[]{x, y};
        } catch (NumberFormatException e) {
            System.out.println("Error parsing coordinates: " + coordinate);
            return null;  // Parsing failed
        }
    }

    // Add fire events to queue
    // Would FIS call: scheduler.addFireEvent(event);
    public synchronized void addFireEvent(FireEvent event) {
        queue.add(event);
        notifyAll();
        System.out.println("Scheduler: Added FireEvent → " + event);
    }

    /**
     *
     * @return The first FireEvent in the queue
     */
    // Would DS call: scheduler.getNextFireEvent();
    public synchronized FireEvent getNextFireEvent() {
        // get FireEvents from queue
        while (queue.isEmpty() && !isFinished) {
            try {
                wait();
            } catch (InterruptedException e) {}

        }
        // return first event. If fire isn't put out, send another drone.
        // Call FIS to see if the fire is out. If so, delete event from queue
        // queue.poll(); returns the first object in the queue, we only want to do this if the fire is extinguished
        if (queue.isEmpty()) return null;

        return queue.peek();
    }

    // The FIS could call this when fire is extinguished?
    // FIS has a count, counts number of litres dropped by drone?
    // When count == int severity return true?
    public synchronized void markFireExtinguished(FireEvent event) {
        queue.remove(event);
        System.out.println("Scheduler: Fire at Zone: " + event.getZoneId() + " Extinguished");
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
    public void run() {
        synchronized (this){
            while (!isFinished) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
