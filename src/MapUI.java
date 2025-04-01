import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

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
            // get the zone coordinates
            int id = zone.getId();
            List<List<Integer>> coords = zone.getCoords();
            int x1 = coords.get(0).get(0);
            int y1 = coords.get(0).get(1);
            int x2 = coords.get(1).get(0);
            int y2 = coords.get(1).get(1);

            int gridX = x1 / 25;
            int gridY = y1 / 25;
            int gridWidth = (x2 - x1) / 25;
            int gridHeight = (y2 - y1) / 25;

            g.setColor(Color.BLUE);
            g.drawRect(gridX * CELL_SIZE, gridY * CELL_SIZE, gridWidth * CELL_SIZE, gridHeight * CELL_SIZE);
            g.drawString("Z(" + id + ")", gridX * CELL_SIZE + 3, gridY * CELL_SIZE + 15);
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
}
