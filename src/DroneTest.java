//import static org.junit.jupiter.api.Assertions.*;
//import org.junit.jupiter.api.Test;
//
//
//public class DroneTest {
//    /**
//     * Tests if the Drone runs for the expected time when running a set single event.
//     */
//    @Test
//    void testDroneCommunications() throws InterruptedException {
//        Scheduler scheduler = new Scheduler("input//test_zone_file.csv", "input//test_event_file.csv");
//        Thread drone = new Thread(new DroneSubsystem(scheduler));
//        drone.start();
//
//        Thread.sleep(1000);
//        assertTrue(drone.isAlive());
//        Thread.sleep(40000);
//        assertTrue(drone.isAlive());
//        Thread.sleep(40000);
//        assertFalse(drone.isAlive());
//    }
//}
