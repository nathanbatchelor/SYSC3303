import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DroneFaultTest {

    static Scheduler scheduler;
    static FireIncidentSubsystem fis;

    @BeforeAll
    public static void setUpOnce() throws UnknownHostException {
        String fireIncidentFile = "src//input//test_event_file_with_faults.csv";
        String zoneFile = "src//input//test_zone_file.csv";
        fis = new FireIncidentSubsystem(fireIncidentFile, 1001, 0, 0, 100, 100,567);
        scheduler = new Scheduler(zoneFile, fireIncidentFile, 2, 896);
    }

    @Test
    public void testDroneFaults() throws InterruptedException, UnknownHostException {
        // setup scheduler and drone for a nozzle fault
        String fireIncidentFile = "src//input//test_event_file_with_faults.csv";
        String zoneFile = "src//input//test_zone_file.csv";

        //FireIncidentSubsystem fis = new FireIncidentSubsystem(fireIncidentFile, 1001, 0, 0, 100, 100,7);
        FireEvent event = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "ARRIVAL", fis);
        FireEvent event2 = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "NOZZLE", fis);
        FireEvent event3 = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "PACKET_LOSS", fis);

        //Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile, 2, 15);

        DroneSubsystem drone1 = new DroneSubsystem(scheduler,1, 15);

        assertTrue(drone1.handleArrivalFault(event, 3));
        assertTrue(drone1.handleNozzleFault(event2));
        assertTrue(drone1.handlePacketLossFault(event3));



    }
}