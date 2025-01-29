import java.io.BufferedReader;
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


    private volatile boolean isFinished = false;

    public Scheduler (String zoneFile) {
       // Future location of drone & FIS objects
        this.zoneFile = zoneFile;
        readZoneFile();
    }

    // Add fire events to queue
    // Would FIS call: scheduler.addFireEvent(event);
    public synchronized void addFireEvent(FireEvent event) {
        queue.add(event);
        notifyAll();
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
        return queue.peek();
    }

    // The FIS could call this when fire is extinguished?
    // FIS has a count, counts number of litres dropped by drone?
    // When count == int severity return true?
    public synchronized void markFireExtinguished(FireEvent event) {
        queue.remove(event);
        System.out.println("Fire at Zone: " + event.getZoneId() + " Extinguished");
    }

    // Signal when all fires from input file are finished?
    public synchronized void finish() {
        isFinished = true;
        notifyAll();
    }

    public void readZoneFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length != 5) {
                    System.out.println("Invalid Line: " + line);
                    continue;
                }

                int zoneId = Integer.parseInt(tokens[0].trim());
                int x1 = Integer.parseInt(tokens[1].trim());
                int y1 = Integer.parseInt(tokens[2].trim());
                int x2 = Integer.parseInt(tokens[3].trim());
                int y2 = Integer.parseInt(tokens[4].trim());

                FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(this, zoneId, x1, y1, x2, y2);
                zones.put(zoneId, fireIncidentSubsystem);
                Thread thread = new Thread(fireIncidentSubsystem);
                thread.setName("Fire Incident Subsystem Zone: " + zoneId);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + filename);
        }
    }

    // Called when thread is finished running
    public synchronized boolean isFinished() {
        return isFinished;
    }

    // Not utilised in iteration #1
    @Override
    public void run() {
        while (!isFinished) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
