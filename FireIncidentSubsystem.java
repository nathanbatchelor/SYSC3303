import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * The FireIncidentSubsystem class is responsible for reading an input file, creating FireEvent instances,
 * parsing information from the file line by line, and adding FireEvent objects to the scheduler queue.
 *
 * This class implements Runnable, allowing it to be executed within a thread, and processes events
 * specific to a given zone.
 *
 * @author Anique
 * @author Jasjot
 * @version 1.0 (Iteration 1), 29th January 2025
 */
public class FireIncidentSubsystem implements Runnable {

    private final Scheduler scheduler;
    private final String eventFile;
    private final int zoneId;
    private final int x1, y1, x2, y2;

    /**
     * Constructs a FireIncidentSubsystem to process fire events for a specific zone.
     *
     * @param scheduler The Scheduler instance used to handle scheduling fire events.
     * @param eventFile The path to the file containing fire events data.
     * @param zoneId    The ID of the zone for which this subsystem is responsible.
     * @param x1        The x-coordinate of the top-left corner of the zone.
     * @param y1        The y-coordinate of the top-left corner of the zone.
     * @param x2        The x-coordinate of the bottom-right corner of the zone.
     * @param y2        The y-coordinate of the bottom-right corner of the zone.
     */
    public FireIncidentSubsystem(Scheduler scheduler, String eventFile, int zoneId, int x1, int y1, int x2, int y2) {
        this.scheduler = scheduler;
        this.eventFile = eventFile;
        this.zoneId = zoneId;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * Runs the FireIncidentSubsystem, reading the event file line by line,
     * parsing the data, and adding relevant FireEvent instances to the scheduler.
     * Only events belonging to this subsystem's zone are processed.
     */
    @Override
    public synchronized void run() {
        boolean eventsAdded = false;
        System.out.println(Thread.currentThread().getName() + " running for Zone " + zoneId);

        try (BufferedReader reader = new BufferedReader(new FileReader(eventFile))) {
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue; // Skip CSV header
                }
                FireEvent fireEvent = parseEvent(line);
                // Only process events for this zone
                if (fireEvent.getZoneId() == zoneId) {
                    System.out.println("FireIncidentSubsystem-Zone " + zoneId + " → New Fire Event: " + fireEvent);
                    scheduler.addFireEvent(fireEvent);
                    eventsAdded = true;
                    System.out.println("Setting events to loaded");
                    scheduler.setEventsLoaded();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // Only call if atleast one event was added
        if (eventsAdded) {
            scheduler.setEventsLoaded();
            System.out.println("Setting events to loaded");
            System.out.println("----------------------------------------\n");
        }
    }

    /**
     * Parses a single line from the fire events file and creates a FireEvent object.
     *
     * @param line A line from the fire events file representing an event.
     * @return A FireEvent instance created from the parsed data.
     */
    private FireEvent parseEvent(String line) {
        String[] slices = line.split(",");
        String time = slices[0];
        int eventZoneId = Integer.parseInt(slices[1]);
        String eventType = slices[2];
        String severity = slices[3];

        return new FireEvent(time, eventZoneId, eventType, severity, this);
    }

    /**
     * Returns the coordinates of the zone as a formatted string.
     *
     * @return A string representing the coordinates of the zone in the format "(x1,y1) to (x2,y2)".
     */
    public String getZoneCoordinates() {
        return "(" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")";
    }

    /**
     * Returns a string representation of the FireIncidentSubsystem, including the zone ID and coordinates.
     *
     * @return A string representing this subsystem.
     */
    @Override
    public String toString() {
        return "FireIncidentSubsystem-Zone " + zoneId + " [" + getZoneCoordinates() + "]";
    }
}
