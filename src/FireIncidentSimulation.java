import javax.swing.*;
import java.awt.*;

public class FireIncidentSimulation {

    public static void main(String[] args) {
        String fireIncidentFile = "src//input//test_event_file_with_faults.csv";
        String zoneFile = "src//input//test_zone_file.csv";
        int numDrones = 10;

        MetricsLogger logger = new MetricsLogger();
        logger.markSimulationStart();

        SwingUtilities.invokeLater(() -> {
            MapUI mapUI = new MapUI();
            DroneStatusPanel statusPanel = new DroneStatusPanel();
            LegendPanel legendPanel = new LegendPanel();

            mapUI.setStatusPanel(statusPanel);

            JFrame frame = new JFrame("Fire Incident Map");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // === RIGHT PANEL (VERTICAL STACK: StatusPanel + LegendPanel) ===
            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
            rightPanel.setPreferredSize(new Dimension(250, 720));

            // Wrap the status panel in a scroll pane:
            JScrollPane statusScrollPane = new JScrollPane(statusPanel);
            statusScrollPane.setPreferredSize(new Dimension(220, 400));
            rightPanel.add(statusScrollPane);

            rightPanel.add(Box.createVerticalStrut(10)); // space between
            rightPanel.add(legendPanel);

            // === MAIN LAYOUT ===
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            contentPanel.add(mapUI, BorderLayout.CENTER);
            contentPanel.add(rightPanel, BorderLayout.EAST);

            // === FINALIZE ===
            frame.setContentPane(contentPanel);
            frame.pack();
            frame.setSize(1600, 900);
            frame.setVisible(true);

            // === SIMULATION ===
            Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile, numDrones, 0, mapUI, logger);
            Thread schedulerThread = new Thread(scheduler, "Scheduler");
            schedulerThread.start();

            for (int i = 1; i <= numDrones; i++) {
                DroneSubsystem drone = new DroneSubsystem(scheduler, i, 0, mapUI, logger);
                new Thread(drone, "Drone Subsystem " + i).start();
            }
        });

    }

}