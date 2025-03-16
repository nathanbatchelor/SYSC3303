//import static org.junit.jupiter.api.Assertions.*;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.BeforeEach;
//
//
//public class DroneSubsystemTest {
//    private Scheduler scheduler;
//    private DroneSubsystem drone;
//    private FireIncidentSubsystem fireIncidentSubsystem;
//    private static final String TEST_ZONE_FILE = "input/test_zone_file.csv";
//    private static final String TEST_EVENT_FILE = "input/test_event_file.csv";
//
//
//    @BeforeEach
//    public void setUp() {
//        scheduler = new Scheduler(TEST_ZONE_FILE, TEST_EVENT_FILE);
//        fireIncidentSubsystem = new FireIncidentSubsystem(scheduler, TEST_EVENT_FILE, 1, 0, 0, 10, 10);
//        drone = new DroneSubsystem(scheduler);
//    }
//
//    @Test
//    public void testInitialDroneState() {
//        assertEquals(DroneSubsystem.DroneState.IDLE, drone.getState(),"Drone should start in IDLE state.");
//    }
//
//    @Test
//    public void testDroneOnRouteState() {
//        FireEvent ev = new FireEvent("12:00", 1, "FIRE_DETECTED", "HIGH", fireIncidentSubsystem);
//        //drone.travelToZoneCenter(10, ev);
//        assertEquals(DroneSubsystem.DroneState.ON_ROUTE, drone.getState(),"Drone should start in ON_ROUTE when travelling.");
//    }
//
//    @Test
//    public void testDroppingAgentState(){
//        drone.extinguishFire(5);
//        assertEquals(DroneSubsystem.DroneState.DROPPING_AGENT, drone.getState(),"Drone should be DROPPING_AGENT when extinguishing fire.");
//    }
//
//    @Test
//    public void testReturningState(){
//        FireEvent ev = new FireEvent("12:00", 1, "FIRE_DETECTED", "HIGH", fireIncidentSubsystem);
//        drone.returnToBase(ev);
//        assertEquals(DroneSubsystem.DroneState.RETURNING, drone.getState(), "Drone should be RETURNING when its going back to base.");
//    }
//}