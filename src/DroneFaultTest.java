import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DroneFaultTest {

    static Scheduler scheduler;
    static FireIncidentSubsystem fis;

    @BeforeAll
    public static void setUpOnce() throws UnknownHostException {
        MapUI mapUI = new MapUI();
        MetricsLogger logger = new MetricsLogger();
        String fireIncidentFile = "src//input//test_event_file_with_faults.csv";
        String zoneFile = "src//input//test_zone_file.csv";
        fis = new FireIncidentSubsystem(fireIncidentFile, 1001, 0, 0, 100, 100,567);
        scheduler = new Scheduler(zoneFile, fireIncidentFile, 2, 896, mapUI, logger);
    }

    @Test
    public void testDroneArrivalFault() {
        FireEvent event = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "ARRIVAL", fis);
        assertFalse(scheduler.timeoutFault);
        scheduler.handleDroneFault(event, event.getFault(), 1);
        assertTrue(scheduler.timeoutFault);
        assertEquals("NONE", event.getFault(), "Fault is removed");
    }

    @Test
    public void testDroneNozzleFault() {
        FireEvent event = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "NOZZLE", fis);
        assertFalse(scheduler.nozzleFault);
        scheduler.handleDroneFault(event, event.getFault(), 1);
        assertTrue(scheduler.nozzleFault);
        assertEquals("NONE", event.getFault(), "Fault is removed");
    }

    @Test
    public void testDronePacketFault() {
        FireEvent event = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "PACKET_LOSS", fis);
        assertFalse(scheduler.packetFault);
        scheduler.handleDroneFault(event, event.getFault(), 1);
        assertTrue(scheduler.packetFault);
        assertEquals("NONE", event.getFault(), "Fault is removed");
    }
}