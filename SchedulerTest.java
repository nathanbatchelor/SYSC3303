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
    private static final String TEST_ZONE_FILE = "SYSC3303/input/test_zone_file.csv";
    private static final String TEST_EVENT_FILE = "SYSC3303/input/test_event_file.csv";

    /**
     * Sets up a new Scheduler instance and a FireIncidentSubsystem before each test.
     */
    @BeforeEach
    void setUp() {
        scheduler = new Scheduler(TEST_ZONE_FILE, TEST_EVENT_FILE);
        fireIncidentSubsystem = new FireIncidentSubsystem(scheduler, TEST_EVENT_FILE, 1, 0, 0, 10, 10);
    }

//    /**
//     * Tests if the FireIncidentSubsystem properly adds fire events to the Scheduler.
//     */
//    @Test
//    void testFireIncidentSubsystemAddsEvents() {
//        Thread fisThread = new Thread(fireIncidentSubsystem);
//        fisThread.start();
//
//        try {
//            fisThread.join(); // Ensure FIS has time to populate the Scheduler
//        } catch (InterruptedException e) {}
//
//        assertNotNull(scheduler.getNextFireEvent(), "Scheduler should have at least one fire event");
//    }
//
//    /**
//     * Tests if the Scheduler correctly processes and finishes after handling all fire events.
//     */
//    @Test
//    void testSchedulerProcessesAllEvents() {
//        Thread fireIndicentSubsystem = new Thread(fireIncidentSubsystem);
//        fireIndicentSubsystem.start();
//        try {
//            fireIndicentSubsystem.join(); // Ensure FIS completes event loading
//        } catch (InterruptedException e) {}
//        FireEvent event = scheduler.getNextFireEvent();
//        assertNotNull(event, "Scheduler should retrieve an event");
//        scheduler.markFireExtinguished(event);
//        assertTrue(scheduler.isFinished(), "Scheduler should be marked as finished after extinguishing all events");
//    }

    @Test
    void testSchedulerHandlesDronesWithExtraWater() {
        FireEvent event1 = new FireEvent("12:00", 1, "FIRE_DETECTED", "MODERATE", fireIncidentSubsystem);
        FireEvent event2 = new FireEvent("12:10", 2, "FIRE_DETECTED", "LOW", fireIncidentSubsystem);

        scheduler.addFireEvent(event1);
        scheduler.addFireEvent(event2);

        FireEvent nextEvent = scheduler.getAdditionalFireEvent(100, 0, 0);

        assertNotNull(nextEvent, "Scheduler should assign a new fire event if drone has extra water.");
    }

    @Test
    void testFireEventWaterManagement() {
        FireEvent fire = new FireEvent("14:30", 3, "FIRE_DETECTED", "HIGH", fireIncidentSubsystem);
        fire.setLitres(30);

        assertEquals(30, fire.getLitres(), "Fire event should start with 30L required.");

        fire.removeLitres(10);
        assertEquals(20, fire.getLitres(), "After removing 10L, 20L should be left.");

        fire.removeLitres(20);
        assertEquals(0, fire.getLitres(), "After removing all remaining water, 0L should be left.");
    }
}
