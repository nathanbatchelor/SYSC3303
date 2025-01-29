import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

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

    // Queue to hold FireEvents?
    private final Queue<FireEvent> queue = new LinkedList<>();
    // Something to store zones
    private final Map<Integer, FireIncidentSubsystem> zones = new HashMap<>();
    // File for zones
    private final String zoneFile;


    private volatile boolean isFinished = false;

    public Scheduler (String zoneFile) {
        loadZoneDetails(zoneFile);
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

    public synchronized boolean isFinished() {
        return isFinished;
    }


    @Override
    public void run() {

    }
}
