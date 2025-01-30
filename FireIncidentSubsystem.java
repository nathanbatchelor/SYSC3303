import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * The FireIncidentSubsystem class will help read input file, create FireEvent and parse information from the file line by line and add teh FireEvent to the
 * scheduler queue.
 *
 * @author Anique
 * @author Jasjot
 *
 * @version 1.0 (Iteration 1), 29th January 2025
 */
public class FireIncidentSubsystem implements Runnable {
    private final Scheduler scheduler;
    private final String eventFile;
    private final int zoneId;
    private final int x1, y1, x2, y2;

    public FireIncidentSubsystem(Scheduler scheduler, String eventFile, int zoneId, int x1, int y1, int x2, int y2) {
        this.scheduler = scheduler;
        this.eventFile = eventFile;
        this.zoneId = zoneId;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    @Override
    public void run() {
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
                    Thread.sleep(800);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Parses a single line from `Sample_event_file.csv` and creates a FireEvent.
     */
    private FireEvent parseEvent(String line) {
        String[] slices = line.split(",");
        String time = slices[0];
        int eventZoneId = Integer.parseInt(slices[1]);
        String eventType = slices[2];
        String severity = slices[3];

        return new FireEvent(time, eventZoneId, eventType, severity, this);
    }

    public String getZoneCoordinates() {
        return "(" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")";
    }

    @Override
    public String toString() {
        return "FireIncidentSubsystem-Zone " + zoneId + " [" + getZoneCoordinates() + "]";
    }
}
