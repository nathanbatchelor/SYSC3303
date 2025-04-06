import javax.swing.*;
import java.awt.*;

public class LegendPanel extends JPanel {
    // Use the same color constants as in MapUI for consistency
    private static final Color ZONE_BORDER_COLOR = new Color(0, 90, 156);
    private static final Color FIRE_ACTIVE_COLOR = new Color(220, 50, 50);
    private static final Color FIRE_INACTIVE_COLOR = new Color(34, 139, 34);
    private static final Color DRONE_IDLE_COLOR = new Color(0, 191, 255);
    private static final Color DRONE_ON_ROUTE_COLOR = new Color(255, 140, 0);
    private static final Color DRONE_DROPPING_AGENT_COLOR = new Color(50, 205, 50);
    private static final Color DRONE_RETURNING_COLOR = new Color(255, 105, 180);
    private static final Color DRONE_FAULT_COLOR = new Color(148, 0, 211);

    private static final int LEGEND_WIDTH = 200;
    private static final int LEGEND_HEIGHT = 300;
    private static final int ITEM_HEIGHT = 25;
    private static final int SQUARE_SIZE = 15;
    private static final int TEXT_OFFSET = 25;
    private static final int VERTICAL_PADDING = 15;
    private static final int HORIZONTAL_PADDING = 10;

    /**
     * Constructs a new LegendPanel with preset dimensions, background color,
     * and border settings.
     */
    public LegendPanel() {
        setPreferredSize(new Dimension(LEGEND_WIDTH, LEGEND_HEIGHT));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING)
        ));
    }

    /**
     * Paints the legend panel by drawing the title, sections, and individual legend items.
     *
     * @param g the Graphics context used for drawing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw legend title
        Font originalFont = g2d.getFont();
        Font titleFont = new Font(originalFont.getName(), Font.BOLD, 14);
        g2d.setFont(titleFont);
        g2d.drawString("Legend", HORIZONTAL_PADDING, 20);
        g2d.setFont(originalFont);

        int y = 40;

        // Draw Zone section
        g2d.setFont(new Font(originalFont.getName(), Font.BOLD, 12));
        g2d.drawString("Zones", HORIZONTAL_PADDING, y);
        g2d.setFont(originalFont);
        y += 20;

        // Zone item
        drawLegendItem(g2d, ZONE_BORDER_COLOR, "Zone Border", y);
        y += ITEM_HEIGHT;

        // Draw Fire section
        g2d.setFont(new Font(originalFont.getName(), Font.BOLD, 12));
        g2d.drawString("Fires", HORIZONTAL_PADDING, y + 10);
        g2d.setFont(originalFont);
        y += 30;

        // Fire items
        drawLegendItem(g2d, FIRE_ACTIVE_COLOR, "Active Fire", y);
        y += ITEM_HEIGHT;
        drawLegendItem(g2d, FIRE_INACTIVE_COLOR, "Inactive Fire", y);
        y += ITEM_HEIGHT;

        // Draw Drone section
        g2d.setFont(new Font(originalFont.getName(), Font.BOLD, 12));
        g2d.drawString("Drone States", HORIZONTAL_PADDING, y + 10);
        g2d.setFont(originalFont);
        y += 30;

        // Drone items
        drawLegendItem(g2d, DRONE_IDLE_COLOR, "Idle", y);
        y += ITEM_HEIGHT;
        drawLegendItem(g2d, DRONE_ON_ROUTE_COLOR, "On Route", y);
        y += ITEM_HEIGHT;
        drawLegendItem(g2d, DRONE_DROPPING_AGENT_COLOR, "Dropping Agent", y);
        y += ITEM_HEIGHT;
        drawLegendItem(g2d, DRONE_RETURNING_COLOR, "Returning", y);
        y += ITEM_HEIGHT;
        drawLegendItem(g2d, DRONE_FAULT_COLOR, "Fault", y);
    }

    /**
     * Draws an individual legend item consisting of a colored square and a label.
     *
     * @param g the Graphics2D context used for drawing
     * @param color the color to fill the square
     * @param label the text label for the legend item
     * @param y the y-coordinate at which to draw the legend item
     */
    private void drawLegendItem(Graphics2D g, Color color, String label, int y) {
        g.setColor(color);
        g.fillRect(HORIZONTAL_PADDING, y, SQUARE_SIZE, SQUARE_SIZE);
        g.setColor(Color.BLACK);
        g.drawRect(HORIZONTAL_PADDING, y, SQUARE_SIZE, SQUARE_SIZE);
        g.drawString(label, HORIZONTAL_PADDING + TEXT_OFFSET, y + SQUARE_SIZE);
    }
}