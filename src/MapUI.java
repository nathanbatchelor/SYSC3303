import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class MapUI extends JPanel {
    private static final int REAL_WIDTH = 2000;  // 2000 meters width
    private static final int REAL_HEIGHT = 1500; // 1500 meters height
    private static final int METERS_PER_CELL = 50; // Each cell represents 25m x 25m
    private static final int PIXELS_PER_CELL = 20;  // Each cell is 10 x 10 pixels

    // Calculate panel dimensions
    private static final int PANEL_WIDTH = REAL_WIDTH / METERS_PER_CELL * PIXELS_PER_CELL;  // 500 pixels
    private static final int PANEL_HEIGHT = REAL_HEIGHT / METERS_PER_CELL * PIXELS_PER_CELL; // 375 pixels

    private java.util.List<Zone> zones = new ArrayList<>();
    private java.util.List<FireEvent> fireEvents = new ArrayList<>();

    public MapUI() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
    }

    public void setZones(java.util.List<Zone> zones) {
        this.zones = zones;
//        repaint();
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

        for( FireEvent fireEvent : fireEvents){
            if (fireEvent == null) {
                continue;
            }
            int zoneId = fireEvent.getZoneId();
            Zone zone = zones.get(zoneId-1);

            List<List<Integer>> coords = zone.getCoords();
            int x1 = coords.get(0).get(0);
            int y1 = coords.get(0).get(1);
            int x2 = coords.get(1).get(0);
            int y2 = coords.get(1).get(1);

            // Calculate zone center in real-world meters

            int centerX = Math.floorDiv((x1 + x2), 2);
            int centerY = Math.floorDiv((y1 + y2), 2);

            if (centerX % 2 == 1) {
                centerX-=METERS_PER_CELL/2;
            }
            if (centerY % 2 == 1) {
                centerY+=METERS_PER_CELL/2;
            }

            // Convert to screen coordinates
            int screenX = (centerX * PIXELS_PER_CELL) / METERS_PER_CELL;
            int screenY = PANEL_HEIGHT - ((centerY * PIXELS_PER_CELL) / METERS_PER_CELL);

            // Fire event should take up 1 whole cell
            if (fireEvent.getCurrentState() == FireEvent.FireEventState.ACTIVE){
                g.setColor(Color.RED);
            }else{
                g.setColor(Color.GREEN);
            }
            g.fillRect(screenX, screenY, PIXELS_PER_CELL, PIXELS_PER_CELL);
            g.setColor(Color.BLACK);
            g.drawString(fireEvent.getSeverity().split("")[0], screenX + 5, screenY + 15);
        }
    }

    public void drawFireEvents(FireEvent fireEvent) {
        int index = fireEvent.getZoneId() - 1;

        // Ensure the list is big enough
        while (fireEvents.size() <= index) {
            fireEvents.add(null);
        }

        fireEvents.set(index, fireEvent);
        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
        //repaint();
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
