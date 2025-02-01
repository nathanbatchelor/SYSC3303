public class FireIncidentSimulation {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java FireIncidentSimulation <FireIncidentFile> <ZoneFile>");
        }
        String fireIncidentFile = "Sample_event_file.csv";
        String zoneFile = "sample_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        // Create the DroneSubsystem Thread
        DroneSubsystem droneSubsystem = new DroneSubsystem(scheduler);
        Thread droneThread = new Thread(droneSubsystem);
        droneThread.setName("Drone Subsystem");

        // Wait until events are loaded before starting drone
        while (!scheduler.isEventsLoaded()) {
            try {
                Thread.sleep(500); // Wait and check again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        droneThread.start();
    }
}
