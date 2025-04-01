import javax.swing.*;

public class FireIncidentSimulation {

    public static void main(String[] args) {
        String fireIncidentFile = "SYSC3303/src/input/test_event_file_with_faults.csv";
        String zoneFile = "SYSC3303/src/input/test_zone_file.csv";

        SwingUtilities.invokeLater(() -> {
            MapUI mapUI = new MapUI();

            // Create a window and add the map panel
            JFrame frame = new JFrame("Fire Incident Map");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600); // adjust as needed
            frame.add(mapUI);
            frame.setVisible(true);  // <--- Show the GUI!

            // Start simulation components
            Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile, 5, 0, mapUI);
            Thread schedulerThread = new Thread(scheduler);
            schedulerThread.setName("Scheduler");
            schedulerThread.start();

            for (int i = 1; i <= 2; i++) {
                DroneSubsystem drone = new DroneSubsystem(scheduler, i, 0, mapUI);
                Thread droneThread = new Thread(drone);
                droneThread.setName("Drone Subsystem " + i);
                droneThread.start();
            }
        });
    }
}
