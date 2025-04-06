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

    // Color scheme
    private static final Color ZONE_BORDER_COLOR = new Color(0, 90, 156); // Darker blue for zone borders
    private static final Color ZONE_TEXT_COLOR = new Color(0, 0, 100); // Dark blue for zone text
    private static final Color GRID_COLOR = new Color(172, 171, 171); // Lighter gray for grid
    private static final Color FIRE_ACTIVE_COLOR = new Color(220, 50, 50); // Brighter red for active fires
    private static final Color FIRE_INACTIVE_COLOR = new Color(34, 139, 34); // Forest green for inactive fires

    // Drone state colors with improved contrast
    private static final Color DRONE_IDLE_COLOR = new Color(0, 191, 255); // Deep sky blue
    private static final Color DRONE_ON_ROUTE_COLOR = new Color(255, 140, 0); // Dark orange
    private static final Color DRONE_DROPPING_AGENT_COLOR = new Color(50, 205, 50); // Lime green
    private static final Color DRONE_RETURNING_COLOR = new Color(255, 105, 180); // Hot pink
    private static final Color DRONE_FAULT_COLOR = new Color(148, 0, 211); // Dark violet

    private java.util.List<Zone> zones = new ArrayList<>();
    private java.util.List<FireEvent> fireEvents = new ArrayList<>();
    private final Map<Integer, DroneInfo> drones = new java.util.concurrent.ConcurrentHashMap<>();

    private final javax.swing.Timer repaintTimer;
    private volatile boolean needsRepaint = false;

    private DroneStatusPanel statusPanel;

    //private int droneX = -1;
    //private int droneY = -1;
    //private DroneSubsystem.DroneState droneState = DroneSubsystem.DroneState.IDLE;

    public MapUI() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.WHITE); // Set white background for better contrast

        repaintTimer = new javax.swing.Timer(100, e -> {
            revalidate();  // Optional
            repaint();
            needsRepaint = false;
        });
        repaintTimer.start();
    }

    public void setZones(java.util.List<Zone> zones) {
        this.zones = zones;
    }

    public void setStatusPanel(DroneStatusPanel statusPanel) {
        this.statusPanel = statusPanel;
    }


    private static class DroneInfo {
        final int x, y;
        final DroneSubsystem.DroneState state;

        public DroneInfo(int x, int y, DroneSubsystem.DroneState state) {
            this.x = x;
            this.y = y;
            this.state = state;
        }
    }

    public synchronized void updateDronePosition(int droneId, int x, int y, DroneSubsystem.DroneState droneState, int remainingAgent, double batteryLife) {
        drones.put(droneId, new DroneInfo(x, y, droneState));
        if (statusPanel != null) {
            // Add dummy/default values for battery/agent unless you pass those in too
            statusPanel.updateDroneStatus(droneId, x, y, droneState, remainingAgent, batteryLife);
        }
    }

    // Overloaded method for backward compatibility:
    public synchronized void updateDronePosition(int droneId, int x, int y, DroneSubsystem.DroneState droneState) {
        updateDronePosition(droneId, x, y, droneState, 0, 0);
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background grid
        drawGrid(g2d);
        drawZones(g2d);
        drawFires(g2d);
        drawDrones(g2d);

//        // Draw zones
//        for (Zone zone : zones) {
//            // get the zone coordinates
//            int id = zone.getId();
//            List<List<Integer>> coords = zone.getCoords();
//            int x1 = coords.get(0).get(0);
//            int y1 = coords.get(0).get(1);
//            int x2 = coords.get(1).get(0);
//            int y2 = coords.get(1).get(1);
//
//            int adjustedY1 = PANEL_HEIGHT - (y1 * PIXELS_PER_CELL / METERS_PER_CELL);
//            int adjustedY2 = PANEL_HEIGHT - (y2 * PIXELS_PER_CELL / METERS_PER_CELL);
//
//            int topY = Math.min(adjustedY1, adjustedY2);
//            int height = Math.abs(adjustedY2 - adjustedY1);
//
//            // Convert to screen coordinates (X axis)
//            int screenX = x1 * PIXELS_PER_CELL / METERS_PER_CELL;
//            int screenWidth = (x2 - x1) * PIXELS_PER_CELL / METERS_PER_CELL;
//
//
//            g.setColor(Color.BLUE);
//            g.drawRect(screenX, topY, screenWidth, height);
//            g.drawString("Z(" + id + ")", screenX + 3, topY + 15);
//        }
//
//        for( FireEvent fireEvent : fireEvents){
//            if (fireEvent == null) {
//                continue;
//            }
//            int zoneId = fireEvent.getZoneId();
//            Zone zone = zones.get(zoneId-1);
//
//            List<List<Integer>> coords = zone.getCoords();
//            int x1 = coords.get(0).get(0);
//            int y1 = coords.get(0).get(1);
//            int x2 = coords.get(1).get(0);
//            int y2 = coords.get(1).get(1);
//
//            // Calculate zone center in real-world meters
//
//            int centerX = Math.floorDiv((x1 + x2), 2);
//            int centerY = Math.floorDiv((y1 + y2), 2);
//
//            if (centerX % 2 == 1) {
//                centerX-=METERS_PER_CELL/2;
//            }
//            if (centerY % 2 == 1) {
//                centerY+=METERS_PER_CELL/2;
//            }
//
//            // Convert to screen coordinates
//            int screenX = (centerX * PIXELS_PER_CELL) / METERS_PER_CELL;
//            int screenY = PANEL_HEIGHT - ((centerY * PIXELS_PER_CELL) / METERS_PER_CELL);
//
//            // Fire event should take up 1 whole cell
//            if (fireEvent.getCurrentState() == FireEvent.FireEventState.ACTIVE){
//                g.setColor(Color.RED);
//            }else{
//                g.setColor(new Color(11,69,21));
//            }
//            g.fillRect(screenX, screenY, PIXELS_PER_CELL, PIXELS_PER_CELL);
//            g.setColor(Color.BLACK);
//            g.drawString(fireEvent.getSeverity().split("")[0], screenX + 5, screenY + 15);
//        }
//        for (Map.Entry<Integer, DroneInfo> entry : drones.entrySet()) {
//            int id = entry.getKey();
//            DroneInfo drone = entry.getValue();
//
//            int screenX = (drone.x * PIXELS_PER_CELL) / METERS_PER_CELL;
//            int screenY = getHeight() - (drone.y * PIXELS_PER_CELL) / METERS_PER_CELL;
//
//            Color droneColor = Color.GRAY; // Default color
//            switch (drone.state) {
//                case DROPPING_AGENT -> droneColor = new Color(55, 255, 0);
//                case RETURNING -> droneColor = Color.PINK;
//                case IDLE -> droneColor = new Color(0, 247, 255);
//                case FAULT -> droneColor = new Color(84, 4, 177);
//                case ON_ROUTE -> droneColor = Color.ORANGE;
//            }
//
//            g.setColor(droneColor);
//            g.fillRect(screenX, screenY, PIXELS_PER_CELL, PIXELS_PER_CELL);
//            g.setColor(Color.BLACK);
//            g.drawString("D" + id, screenX + 2, screenY + 15);
//        }
    }

    private void drawZones(Graphics2D g) {
        int panelHeight = getHeight();
        int Y_OFFSET = 2; // move everything up by 2 pixels to show bottom lines

        for (Zone zone : zones) {
            int id = zone.getId();
            List<List<Integer>> coords = zone.getCoords();
            int x1 = coords.get(0).get(0);
            int y1 = coords.get(0).get(1);
            int x2 = coords.get(1).get(0);
            int y2 = coords.get(1).get(1);

            // Convert to screen space (bottom-left aligned + offset)
            int screenX1 = x1 * PIXELS_PER_CELL / METERS_PER_CELL;
            int screenX2 = x2 * PIXELS_PER_CELL / METERS_PER_CELL;
            int screenY1 = panelHeight - (y1 * PIXELS_PER_CELL / METERS_PER_CELL) - Y_OFFSET;
            int screenY2 = panelHeight - (y2 * PIXELS_PER_CELL / METERS_PER_CELL) - Y_OFFSET;

            int screenX = Math.min(screenX1, screenX2);
            int screenY = Math.min(screenY1, screenY2);
            int width = Math.abs(screenX2 - screenX1);
            int height = Math.abs(screenY2 - screenY1);

            // Fill zone background
            g.setColor(new Color(230, 240, 255, 40));
            g.fillRect(screenX, screenY, width, height);

            // Draw border
            g.setColor(ZONE_BORDER_COLOR);
            g.setStroke(new BasicStroke(2.0f));
            g.drawRect(screenX, screenY, width, height);

            // Draw label
            Font originalFont = g.getFont();
            Font boldFont = new Font(originalFont.getName(), Font.BOLD, 12);
            g.setFont(boldFont);
            g.setColor(ZONE_TEXT_COLOR);
            g.drawString("Z" + id, screenX + 5, screenY + 15);
            g.setFont(originalFont);
        }
    }


    private void drawFires(Graphics2D g) {
        int panelHeight = getHeight();
        int Y_OFFSET = 2; // same offset as drawZones

        for (FireEvent fireEvent : fireEvents) {
            if (fireEvent == null) continue;

            int zoneId = fireEvent.getZoneId();
            if (zoneId - 1 >= zones.size()) continue;  // bounds safety
            Zone zone = zones.get(zoneId - 1);

            List<List<Integer>> coords = zone.getCoords();
            int x1 = coords.get(0).get(0);
            int y1 = coords.get(0).get(1);
            int x2 = coords.get(1).get(0);
            int y2 = coords.get(1).get(1);

            // Calculate zone center in real-world meters
            int centerX = Math.floorDiv(x1 + x2, 2);
            int centerY = Math.floorDiv(y1 + y2, 2);

            if (centerX % 2 == 1) centerX -= METERS_PER_CELL / 2;
            if (centerY % 2 == 1) centerY += METERS_PER_CELL / 2;

            // Convert to screen coordinates
            int screenX = (centerX * PIXELS_PER_CELL) / METERS_PER_CELL;
            int screenY = panelHeight - (centerY * PIXELS_PER_CELL) / METERS_PER_CELL - Y_OFFSET;

            // Draw fire event
            g.setColor(fireEvent.getCurrentState() == FireEvent.FireEventState.ACTIVE
                    ? FIRE_ACTIVE_COLOR
                    : FIRE_INACTIVE_COLOR);

            g.fillRect(screenX, screenY, PIXELS_PER_CELL, PIXELS_PER_CELL);

            // Draw severity letter
            g.setColor(Color.WHITE);
            Font originalFont = g.getFont();
            Font boldFont = new Font(originalFont.getName(), Font.BOLD, 12);
            g.setFont(boldFont);
            g.drawString(fireEvent.getSeverity().split("")[0], screenX + 5, screenY + 15);
            g.setFont(originalFont);
        }
    }

    private void drawDrones(Graphics2D g) {
        for (Map.Entry<Integer, DroneInfo> entry : drones.entrySet()) {
            int id = entry.getKey();
            DroneInfo drone = entry.getValue();

            int screenX = (drone.x * PIXELS_PER_CELL) / METERS_PER_CELL;
            int screenY = getHeight() - ((drone.y * PIXELS_PER_CELL) / METERS_PER_CELL) - PIXELS_PER_CELL;

            // Choose drone color based on state
            Color droneColor;
            switch (drone.state) {
                case DROPPING_AGENT -> droneColor = DRONE_DROPPING_AGENT_COLOR;
                case RETURNING -> droneColor = DRONE_RETURNING_COLOR;
                case IDLE -> droneColor = DRONE_IDLE_COLOR;
                case FAULT -> droneColor = DRONE_FAULT_COLOR;
                case ON_ROUTE -> droneColor = DRONE_ON_ROUTE_COLOR;
                default -> droneColor = Color.GRAY;
            }

            // Draw drone
            g.setColor(droneColor);
            g.fillRect(screenX, screenY, PIXELS_PER_CELL, PIXELS_PER_CELL);

            // Border
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(0.8f));
            g.drawRect(screenX, screenY, PIXELS_PER_CELL, PIXELS_PER_CELL);
            //g.drawRect(,0,PIXELS_PER_CELL,PIXELS_PER_CELL);
            g.setStroke(new BasicStroke(1.0f)); // reset

            // Label
            Font originalFont = g.getFont();
            Font boldFont = new Font(originalFont.getName(), Font.BOLD, 12);
            g.setFont(boldFont);
            g.setColor(isDarkColor(droneColor) ? Color.WHITE : Color.BLACK);
            g.drawString("D" + id, screenX + 2, screenY + 15);
            g.setFont(originalFont);
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
        g.setColor(GRID_COLOR); // consistent with your defined grid color

        int width = getWidth();
        int height = getHeight();

        // Vertical lines (X-axis grid)
        for (int x = 0; x <= width; x += PIXELS_PER_CELL) {
            g.drawLine(x, 0, x, height);
        }

        // Horizontal lines (Y-axis grid from bottom up)
        for (int y = 0; y <= height; y += PIXELS_PER_CELL) {
            g.drawLine(0, height - y, width, height - y);
        }
    }

    // Helper method to determine if a color is dark (for text contrast)
    private boolean isDarkColor(Color color) {
        // Using perceived brightness formula
        double brightness = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
        return brightness < 128;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PANEL_WIDTH, PANEL_HEIGHT);
    }
}