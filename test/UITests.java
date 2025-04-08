
import org.junit.jupiter.api.*;
import java.util.List;
import java.awt.*;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

public class UITests {

    private static MapUI mapUI;
    private static FireIncidentSubsystem fis;

    @BeforeAll
    public static void setUp() throws UnknownHostException {
        String fireIncidentFile = "src//input//test_event_file.csv";
        mapUI = new MapUI();
        fis = new FireIncidentSubsystem(fireIncidentFile, 50, 0, 0, 100, 100,145);
    }

    @Test
    public void setZones() {
        Zone zone1 = new Zone(1, 0, 0, 100, 100);
        Zone zone2 = new Zone(2, 100, 100, 500, 500);
        Zone zone3 = new Zone(3, 300, 0, 250, 500);
        mapUI.setZones(List.of(zone1, zone2, zone3));
        assertEquals(3, mapUI.zones.size());
    }

    @Test
    public void testDroneUpdates() {
        mapUI.updateDronePosition(1, 0, 0, DroneSubsystem.DroneState.IDLE, 14, 100);
        MapUI.DroneInfo drone = mapUI.getDrones().get(1);
        assertEquals(DroneSubsystem.DroneState.IDLE, drone.state);
        assertEquals(0, drone.x);
        assertEquals(0, drone.y);


        mapUI.updateDronePosition(1, 50, 100, DroneSubsystem.DroneState.ON_ROUTE, 14, 100);
        drone = mapUI.getDrones().get(1);
        assertEquals(DroneSubsystem.DroneState.ON_ROUTE, drone.state);
        assertEquals(50, drone.x);
        assertEquals(100, drone.y);
    }

    @Test
    public void testFireEventDrawing() {
        assertEquals(0, mapUI.fireEvents.size());
        FireEvent event = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "ARRIVAL", fis);
        mapUI.drawFireEvents(event);
        assertEquals(1, mapUI.fireEvents.size());
        assertEquals(event, mapUI.fireEvents.get(0));
    }

    @Test
    public void testBackgroundIsWhite() {
        assertEquals(Color.WHITE, mapUI.getBackground());
    }

    @Test
    public void testMapUISize() {
        assertEquals(new Dimension(800, 600), mapUI.getPreferredSize());
    }
}