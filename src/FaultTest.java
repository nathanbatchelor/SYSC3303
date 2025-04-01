//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//
//import java.io.ByteArrayOutputStream;
//import java.io.PrintStream;
//import java.net.UnknownHostException;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//public class FaultTest {
//
//    static Scheduler scheduler;
//    static FireIncidentSubsystem fis;
//
//
//    @BeforeAll
//    public static void setUpOnce() throws UnknownHostException {
//        String fireIncidentFile = "src//input//test_event_file_with_faults.csv";
//        String zoneFile = "src//input//test_zone_file.csv";
//        fis = new FireIncidentSubsystem(fireIncidentFile, 1001, 0, 0, 100, 100,123);
//        scheduler = new Scheduler(zoneFile, fireIncidentFile, 2, 234);
//    }
//
//    @Test
//    public void testArrivalFault() throws InterruptedException, UnknownHostException {
//        FireEvent event = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "ARRIVAL", fis);
//        scheduler.handleDroneFault(event, "", 1);
//        assertTrue(scheduler.timeoutFault, "Test Message: arrival timout fault occurred");
//        assertEquals("NONE", event.getFault(), "Test Message: event fault removed");
//    }
//
//    @Test
//    public void testNozzleFault() throws InterruptedException, UnknownHostException {
//        FireEvent event = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "NOZZLE", fis);
//        scheduler.handleDroneFault(event, "", 1);
//        assertTrue(scheduler.nozzleFault, "Test Message: nozzle fault occurred");
//        assertEquals("NONE", event.getFault(), "Test Message: event fault removed");
//    }
//
//    @Test
//    public void testPacketFault() throws InterruptedException, UnknownHostException {
//        FireEvent event = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "PACKET_LOSS", fis);
//        scheduler.handleDroneFault(event, "", 1);
//        assertTrue(scheduler.packetFault, "Test Message: packet loss fault occurred");
//        assertEquals("NONE", event.getFault(), "Test Message: event fault removed");
//    }
//}