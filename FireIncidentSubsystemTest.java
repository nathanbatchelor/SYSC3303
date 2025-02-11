import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FireIncidentSubsystemTest {

    private static final String mockEventFile =
            "Time,Zone ID,Event Type,Severity\n" +
            "14:03:15,3,FIRE_DETECTED,High\n" +
            "14:10:00,7,DRONE_REQUEST,Moderate\n";

    private Scheduler scheduler;
    private FireIncidentSubsystem fireIncidentSubsystem;

    @BeforeEach
    void setUp() throws IOException {

        // Temporary file path
        Path filePath = Files.createTempFile("eventFile", ".csv");

        // Write to the temporary file
        Files.write(filePath, mockEventFile.getBytes());

        // event file used in FIS
        File eventFile = filePath.toFile();

        // initialize scheduler and FIS
        scheduler = new Scheduler("zoneFile", eventFile.getAbsolutePath());
        fireIncidentSubsystem = new FireIncidentSubsystem(scheduler, eventFile.getAbsolutePath(), 3, 0, 0, 700, 600);
    }

//    @Test
//    public void testFireIncidentSubsystemReadAndAddEvent() throws InterruptedException {
//
//        // Create FIS thread and start
//        Thread fireIncidentSubsystemThread = new Thread(fireIncidentSubsystem);
//        fireIncidentSubsystemThread.start();
//
//        // Wait for FIS thread to complete
//        fireIncidentSubsystemThread.join();
//
//        // Assert the correct data is a part of the event
//        FireEvent event = scheduler.getNextFireEvent();
//        assertEquals("14:03:15", event.getTime());
//        assertEquals(3, event.getZoneId());
//        assertEquals("FIRE_DETECTED", event.getEventType());
//        assertEquals("High", event.getSeverity());
//
//        // Nothing else was queued
//        event = scheduler.getNextFireEvent();
//        assertNull(event);
//    }
}