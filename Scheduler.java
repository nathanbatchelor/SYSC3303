import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**
 * The Scheduler class acts as a central system for handling fire events.
 * It manages incoming fire events and assigns tasks to drones
 *
 * @author Joey Andrwes
 * @author Grant Phillips
 */

public class Scheduler implements Runnable {

    // Queue to hold FireEvents?
    private final Queue<FireEvent> queue = new LinkedList<>();

    // Hashmap to store zones?
    // private final HashMap<Integer, Zone> zones = new HashMap<>();

    private volatile boolean isFinished = false;

    public Scheduler (String zoneFile) {
        loadZoneDetails(zoneFile);
    }

    // Add fire events to queue
    public synchronized void addFireEvent(FireEvent event) {
        queue.add(event);
        notifyAll();
    }

    // get next fire event
    public synchronized FireEvent getNextFireEvent() {
        // get FireEvents from queue
    }

    // Load zone details from file
    private void loadZoneDetails(String zoneFile) {
        // Use buffered reader to get file
        // split up lines into zoneId and coordinates
        // Add zone class to create zones from file?
    }

    // Potiential function to get a zone by zone id (if we use a zone class)
    public synchronized Zone getZone(int zoneId){
        return zones.get(zoneId);
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
