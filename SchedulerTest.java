import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for the Scheduler class.
 * Ensures proper event handling and synchronization using FireIncidentSubsystem.
 */
class SchedulerTest {
    private Scheduler scheduler;
    private FireIncidentSubsystem fireIncidentSubsystem;

    /**
     * Sets up a new Scheduler instance and a FireIncidentSubsystem before each test.
     */
    @BeforeEach
    void setUp() {
        scheduler = new Scheduler("input//test_zone_file.csv", "input//test_event_file.csv");
    }

    /**
     * Tests if the FireIncidentSubsystem properly adds fire events to the Scheduler.
     */
    @Test
    void testFireIncidentSubsystemAddsEvents() {
        Thread fisThread = new Thread(fireIncidentSubsystem);
        fisThread.start();

        try {
            fisThread.join(); // Ensure FIS has time to populate the Scheduler
        } catch (InterruptedException e) {}

        assertNotNull(scheduler.getNextFireEvent(), "Scheduler should have at least one fire event");
    }

    /**
     * Tests if the Scheduler correctly processes and finishes after handling all fire events.
     */
    @Test
    void testSchedulerProcessesAllEvents() {
        Thread fireIndicentSubsystem = new Thread(fireIncidentSubsystem);
        fireIndicentSubsystem.start();
        try {
            fireIndicentSubsystem.join(); // Ensure FIS completes event loading
        } catch (InterruptedException e) {}
        FireEvent event = scheduler.getNextFireEvent();
        assertNotNull(event, "Scheduler should retrieve an event");
        scheduler.markFireExtinguished(event);
        assertTrue(scheduler.isFinished(), "Scheduler should be marked as finished after extinguishing all events");
    }
}
