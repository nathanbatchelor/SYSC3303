import java.util.LinkedList;
import java.util.Queue;

public class Scheduler implements Runnable {

    // Queue to hold FireEvents?
    private final Queue<FireEvent> queue = new LinkedList<>();

    // Hashmap to store zones?

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
        return null;// get FireEvents from queue
    }

    // Load zone details from file
    private void loadZoneDetails(String zoneFile) {
        // Use buffered reader to get file
        // split up lines into zoneId and coordinates
        // Add zone class to create zones from file?
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
