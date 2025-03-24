public class FireIncidentSimulation {
    public static void main(String[] args) {
        String fireIncidentFile = "input//test_event_file.csv";
        String zoneFile = "input//test_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile, 2);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        for (int i = 1; i <= 2; i++){
            DroneSubsystem drone = new DroneSubsystem(scheduler, i);
            Thread droneThread = new Thread(drone);
            droneThread.setName("Drone Subsystem " + i);
            droneThread.start();
        }
    }
}
