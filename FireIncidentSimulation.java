public class FireIncidentSimulation {
    public static void main(String[] args) {
        String fireIncidentFile = "input//test_event_file.csv";
        String zoneFile = "input//test_zone_file.csv";

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
