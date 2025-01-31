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


        System.out.println("Setting up drone");
        DroneSubsystem drone = new DroneSubsystem(scheduler);
        Thread droneSubsystem = new Thread(drone);
        droneSubsystem.setName("Drone Subsystem");
        droneSubsystem.start();
    }
}
