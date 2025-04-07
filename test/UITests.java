
import org.junit.jupiter.api.*;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

public class UITests {

    MapUI mapUI;

    @BeforeEach
    public void setUp() {
        mapUI = new MapUI();
    }

    @Test
    public void testDroneUpdates() {
        mapUI.updateDronePosition(1, 50, 50, DroneSubsystem.DroneState.ON_ROUTE);
        MapUI.DroneInfo drone = mapUI.getDrones().get(1);
        assertTrue(mapUI.getDrones().containsKey(1));
        assertEquals(DroneSubsystem.DroneState.ON_ROUTE, drone.state);
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