public class FireIncidentSimulation {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java FireIncidentSimulation <FireIncidentFile> <ZoneFile>");
        }

        String fireIncidentFile = "/Users/jandrews/Documents/Carleton/Year\\ 3/SYSC3303/Assignments/Assignment\\ 1.2/SYSC3303/Sample_event_file.csv ";
        String zoneFile = "/Users/jandrews/Documents/Carleton/Year\\ 3/SYSC3303/Assignments/Assignment\\ 1.2/SYSC3303/sample_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        DroneSubsystem drone = new DroneSubsystem(scheduler);
        Thread droneSubsystem = new Thread(drone);
        droneSubsystem.setName("Drone Subsystem");
        droneSubsystem.start();
    }
}
