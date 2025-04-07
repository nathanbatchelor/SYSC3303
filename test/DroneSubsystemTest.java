import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;

public class DroneSubsystemTest {
    private static Scheduler scheduler;
    private static DroneSubsystem drone;
    private static FireIncidentSubsystem fis;

    @BeforeAll
    public static void setUpOnce() throws UnknownHostException {
        MetricsLogger logger = new MetricsLogger();
        MapUI mapUI = new MapUI();
        String fireIncidentFile = "src//input//test_event_file.csv";
        String zoneFile = "src//input//test_zone_file_2.csv";
        fis = new FireIncidentSubsystem(fireIncidentFile, 1001, 0, 0, 100, 100,123);
        scheduler = new Scheduler(zoneFile, fireIncidentFile, 2, 234, mapUI, logger);
        drone = new DroneSubsystem(scheduler, 1, 99, mapUI, logger);
    }

    @Test
    public void testIdleDroneState() {
        MetricsLogger logger = new MetricsLogger();
        MapUI mapUI = new MapUI();
        DroneSubsystem idleDrone = new DroneSubsystem(scheduler, 1, 199, mapUI, logger);
        assertEquals(DroneSubsystem.DroneState.IDLE, idleDrone.getState(),"Drone should start in IDLE state.");
    }

    @Test
    public void testDroneOnRouteState() {
        FireEvent event = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "ARRIVAL", fis);
        drone.travelToZoneCenter(10, event);
        assertEquals(DroneSubsystem.DroneState.ON_ROUTE, drone.getState(),"Drone should start in ON_ROUTE when travelling.");
    }

    @Test
    public void testDroppingAgentState(){
        drone.extinguishFire(4);
        assertEquals(DroneSubsystem.DroneState.DROPPING_AGENT, drone.getState(),"Drone should be DROPPING_AGENT when extinguishing fire.");
    }

    @Test
    public void testReturningState(){
        FireEvent event = new FireEvent("12:00", 1, "FIRE_DETECTED", "HIGH", "NONE", null );
        drone.testingReturningState = true;
        drone.returnToBase(event);
        assertEquals(DroneSubsystem.DroneState.RETURNING, drone.getState(), "Drone should be RETURNING when its going back to base.");
    }
}