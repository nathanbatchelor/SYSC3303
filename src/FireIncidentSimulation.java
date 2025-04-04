import javax.swing.*;
import java.awt.*;

public class FireIncidentSimulation {

    public static void main(String[] args) {
        String fireIncidentFile = "src//input//test_event_file_with_faults.csv";
        String zoneFile = "src//input//test_zone_file.csv";

        SwingUtilities.invokeLater(() -> {
            MapUI mapUI = new MapUI();
            int WIDTH = 1280;
            int HEIGHT = 720;

            int PADDING = 20;

            // Create a window and add the map panel
            JFrame frame = new JFrame("Fire Incident Map");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

            // Add the map to the content panel
            contentPanel.add(mapUI, BorderLayout.CENTER);

            // Add the content panel to the frame
            frame.setContentPane(contentPanel);

            // Pack and set size (adjust to include padding)
            frame.pack();
            Insets insets = frame.getInsets();
//            frame.setSize(WIDTH + insets.right + insets.left + (PADDING * 2),
//                    HEIGHT + insets.top + insets.bottom + (PADDING * 2));
            frame.setSize(WIDTH,HEIGHT);

            frame.setResizable(false);
            frame.setVisible(true);

            // Start simulation components
            Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile, 5, 0, mapUI);
            Thread schedulerThread = new Thread(scheduler);
            schedulerThread.setName("Scheduler");
            schedulerThread.start();

            for (int i = 1; i <= 1; i++) {
                DroneSubsystem drone = new DroneSubsystem(scheduler, i, 0, mapUI);
                Thread droneThread = new Thread(drone);
                droneThread.setName("Drone Subsystem " + i);
                droneThread.start();
            }
        });
    }
}
