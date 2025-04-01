import javax.swing.*;
import java.awt.*;
import java.util.*;

public class MapUI extends JPanel {
    private static final int CELL_SIZE = 25;  // 25 pixels per 25 meters
    private java.util.List<Zone> zones = new ArrayList<>();

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
            int gridX = zone.x1 / 25;
            int gridY = zone.y1 / 25;
            int gridWidth = (zone.x2 - zone.x1) / 25;
            int gridHeight = (zone.y2 - zone.y1) / 25;

            g.setColor(Color.BLUE);
            g.drawRect(gridX * CELL_SIZE, gridY * CELL_SIZE, gridWidth * CELL_SIZE, gridHeight * CELL_SIZE);
            g.drawString("Z(" + zone.id + ")", gridX * CELL_SIZE + 3, gridY * CELL_SIZE + 15);
        }
    }

    private void drawGrid(Graphics g) {
        g.setColor(new Color(200, 200, 200)); // light gray

        int width = getWidth();
        int height = getHeight();

        for (int x = 0; x < width; x += CELL_SIZE) {
            g.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += CELL_SIZE) {
            g.drawLine(0, y, width, y);
        }
    }

    static class Zone {
        int id, x1, y1, x2, y2;

        public Zone(int id, int x1, int y1, int x2, int y2) {
            this.id = id;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }
}
