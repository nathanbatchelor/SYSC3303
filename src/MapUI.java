import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MapUI extends JPanel {
    private static final int REAL_WIDTH = 2000;  // 2000 meters width
    private static final int REAL_HEIGHT = 1500; // 1500 meters height
    private static final int METERS_PER_CELL = 25; // Each cell represents 20m×20m
    private static final int PIXELS_PER_CELL = 10;  // Each cell is 5×5 pixels

    // Calculate panel dimensions
    private static final int PANEL_WIDTH = REAL_WIDTH / METERS_PER_CELL * PIXELS_PER_CELL;  // 500 pixels
    private static final int PANEL_HEIGHT = REAL_HEIGHT / METERS_PER_CELL * PIXELS_PER_CELL; // 375 pixels

    private java.util.List<Zone> zones = new ArrayList<>();

    public MapUI() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
    }

    public void setZones(java.util.List<Zone> zones) {
        this.zones = zones;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw background grid
        drawGrid(g);

        // Draw zones
        for (Zone zone : zones) {
            // get the zone coordinates
            int id = zone.getId();
            List<List<Integer>> coords = zone.getCoords();
            int x1 = coords.get(0).get(0);
            int y1 = coords.get(0).get(1);
            int x2 = coords.get(1).get(0);
            int y2 = coords.get(1).get(1);

            int adjustedY1 = PANEL_HEIGHT - (y1 * PIXELS_PER_CELL / METERS_PER_CELL);
            int adjustedY2 = PANEL_HEIGHT - (y2 * PIXELS_PER_CELL / METERS_PER_CELL);

            int topY = Math.min(adjustedY1, adjustedY2);
            int height = Math.abs(adjustedY2 - adjustedY1);

            // Convert to screen coordinates (X axis)
            int screenX = x1 * PIXELS_PER_CELL / METERS_PER_CELL;
            int screenWidth = (x2 - x1) * PIXELS_PER_CELL / METERS_PER_CELL;


            g.setColor(Color.BLUE);
            g.drawRect(screenX, topY, screenWidth, height);
            g.drawString("Z(" + id + ")", screenX + 3, topY + 15);
        }
    }

    private void drawGrid(Graphics g) {
        g.setColor(new Color(200, 200, 200)); // light gray

        // Draw regular grid lines
        for (int x = 0; x <= PANEL_WIDTH; x += PIXELS_PER_CELL) {
            g.drawLine(x, 0, x, PANEL_HEIGHT);
        }
        for (int y = 0; y <= PANEL_HEIGHT; y += PIXELS_PER_CELL) {
            g.drawLine(0, y, PANEL_WIDTH, y);
        }

        // Draw emphasized axis lines
        g.drawLine(100, 100, 100, 100);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
    }
}
