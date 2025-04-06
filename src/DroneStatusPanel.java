import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DroneStatusPanel extends JPanel {
    private final Map<Integer, DroneStatus> droneStatuses = new ConcurrentHashMap<>();
    private final Color panelBackground = new Color(240, 240, 240);
    private final Font titleFont = new Font("Arial", Font.BOLD, 14);
    private final Font contentFont = new Font("Arial", Font.PLAIN, 12);

    // Status colors matching the map visualization
    private static final Color DRONE_IDLE_COLOR = new Color(0, 191, 255); // Deep sky blue
    private static final Color DRONE_ON_ROUTE_COLOR = new Color(255, 140, 0); // Dark orange
    private static final Color DRONE_DROPPING_AGENT_COLOR = new Color(50, 205, 50); // Lime green
    private static final Color DRONE_RETURNING_COLOR = new Color(255, 105, 180); // Hot pink
    private static final Color DRONE_FAULT_COLOR = new Color(148, 0, 211); // Dark violet

    private final Timer refreshTimer;

    /**
     * Internal class representing the status of a single drone.
     */
    private static class DroneStatus {
        final int x, y;
        final DroneSubsystem.DroneState state;
        final int remainingAgent;
        final double batteryLife;

        /**
         * Constructs a new DroneStatus.
         *
         * @param x the x-coordinate of the drone
         * @param y the y-coordinate of the drone
         * @param state the current state of the drone
         * @param remainingAgent the remaining firefighting agent (in liters)
         * @param batteryLife the remaining battery life in seconds
         */
        public DroneStatus(int x, int y, DroneSubsystem.DroneState state, int remainingAgent, double batteryLife) {
            this.x = x;
            this.y = y;
            this.state = state;
            this.remainingAgent = remainingAgent;
            this.batteryLife = batteryLife;
        }
    }

    /**
     * Constructs a new DroneStatusPanel.
     */
    public DroneStatusPanel() {
        //setPreferredSize(new Dimension(200, 400));
        setBackground(panelBackground);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10),
                BorderFactory.createLineBorder(Color.GRAY)
        ));

        // Create timer for periodic refresh
        refreshTimer = new Timer(500, e -> repaint());
        refreshTimer.start();
    }

    /**
     * Returns the preferred size of this panel based on the number of drone entries.
     *
     * @return the preferred Dimension of the panel
     */
    @Override
    public Dimension getPreferredSize() {
        // Header height + (approx. 90 pixels per drone entry)
        int baseHeight = 50;
        int droneCount = droneStatuses.size();
        int dynamicHeight = baseHeight + (droneCount * 90);
        // Ensure a minimum height (e.g., 400 pixels)
        return new Dimension(200, Math.max(400, dynamicHeight));
    }

    /**
     * Updates the status information for a given drone.
     *
     * @param droneId       the drone's identifier
     * @param x             the x-coordinate of the drone
     * @param y             the y-coordinate of the drone
     * @param state         the current state of the drone
     * @param remainingAgent the amount of firefighting agent remaining (in liters)
     * @param batteryLife   the remaining battery life in seconds
     */
    public synchronized void updateDroneStatus(int droneId, int x, int y,
                                               DroneSubsystem.DroneState state,
                                               int remainingAgent, double batteryLife) {
        droneStatuses.put(droneId, new DroneStatus(x, y, state, remainingAgent, batteryLife));
        repaint();
    }

    /**
     * Paints the component by drawing the title, separator, and the status information for each drone.
     *
     * @param g the Graphics context used for painting
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw title
        g2d.setFont(titleFont);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Drone Status Monitor", 10, 25);

        // Draw separator line
        g2d.drawLine(10, 30, getWidth() - 20, 30);

        // Draw drone statuses
        g2d.setFont(contentFont);
        int y = 50;

        if (droneStatuses.isEmpty()) {
            g2d.drawString("No active drones", 10, y);
        } else {
            for (Map.Entry<Integer, DroneStatus> entry : droneStatuses.entrySet()) {
                int droneId = entry.getKey();
                DroneStatus status = entry.getValue();

                // Status indicator color box
                Color statusColor;
                String statusText;

                switch (status.state) {
                    case IDLE:
                        statusColor = DRONE_IDLE_COLOR;
                        statusText = "IDLE";
                        break;
                    case ON_ROUTE:
                        statusColor = DRONE_ON_ROUTE_COLOR;
                        statusText = "ON ROUTE";
                        break;
                    case DROPPING_AGENT:
                        statusColor = DRONE_DROPPING_AGENT_COLOR;
                        statusText = "DROPPING AGENT";
                        break;
                    case RETURNING:
                        statusColor = DRONE_RETURNING_COLOR;
                        statusText = "RETURNING";
                        break;
                    case FAULT:
                        statusColor = DRONE_FAULT_COLOR;
                        statusText = "FAULT";
                        break;
                    default:
                        statusColor = Color.GRAY;
                        statusText = "UNKNOWN";
                }

                // Draw status box
                g2d.setColor(statusColor);
                g2d.fillRect(10, y - 12, 12, 12);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(10, y - 12, 12, 12);

                // Draw drone info
                g2d.drawString("Drone " + droneId + ": " + statusText, 30, y);
                y += 20;
                g2d.drawString("Position: (" + status.x + ", " + status.y + ")", 30, y);
                y += 20;

                // Draw agent level bar
                g2d.drawString("Agent: " + status.remainingAgent + "L", 30, y);
                drawProgressBar(g2d, 100, y - 12, 80, 12, status.remainingAgent, 14, Color.BLUE);
                y += 20;

                // Draw battery level bar
                int batteryPercent = (int)((status.batteryLife / 1800) * 100);
                g2d.drawString("Battery: " + batteryPercent + "%", 30, y);

                Color batteryColor;
                if (batteryPercent > 60) {
                    batteryColor = new Color(50, 205, 50); // Green
                } else if (batteryPercent > 30) {
                    batteryColor = new Color(255, 165, 0); // Orange
                } else {
                    batteryColor = new Color(220, 50, 50); // Red
                }

                drawProgressBar(g2d, 100, y - 12, 80, 12, batteryPercent, 100, batteryColor);

                y += 30; // Space between drone entries

                // Draw separator between drones
                g2d.setColor(new Color(200, 200, 200));
                g2d.drawLine(10, y - 10, getWidth() - 20, y - 10);
            }
        }
    }

    /**
     * Draws a progress bar to visually represent a value relative to a maximum.
     *
     * @param g        the Graphics2D context used for drawing
     * @param x        the x-coordinate of the progress bar
     * @param y        the y-coordinate of the progress bar
     * @param width    the width of the progress bar
     * @param height   the height of the progress bar
     * @param value    the current value to display
     * @param maxValue the maximum possible value
     * @param fillColor    the color of the filled portion of the progress bar
     */
    private void drawProgressBar(Graphics2D g, int x, int y, int width, int height,
                                 int value, int maxValue, Color fillColor) {
        // Draw background
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(x, y, width, height);

        // Draw fill level
        g.setColor(fillColor);
        int fillWidth = (int)(((double)value / maxValue) * width);
        fillWidth = Math.min(fillWidth, width); // Ensure we don't exceed the total width
        g.fillRect(x, y, fillWidth, height);

        // Draw border
        g.setColor(Color.DARK_GRAY);
        g.drawRect(x, y, width, height);
    }
}