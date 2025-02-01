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

        DroneSubsystem droneSubsystem = new DroneSubsystem(scheduler);
        Thread droneThread = new Thread(droneSubsystem);
        droneThread.setName("Drone Subsystem");
        droneThread.start();
    }
}
