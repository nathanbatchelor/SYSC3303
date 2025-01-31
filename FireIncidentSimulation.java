public class FireIncidentSimulation {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java FireIncidentSimulation <FireIncidentFile> <ZoneFile>");
        }

        String fireIncidentFile = "SYSC3303/Sample_event_file.csv";
        String zoneFile = "SYSC3303/sample_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        Thread droneSubsystem = new Thread(new DroneSubsystem(scheduler));
        droneSubsystem.setName("Drone Subsystem");
        droneSubsystem.start();
    }
}
