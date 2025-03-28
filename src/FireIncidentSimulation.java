public class FireIncidentSimulation {
    public static void main(String[] args) {
        String fireIncidentFile = "src//input//test_event_file_with_faults.csv";
        String zoneFile = "src//input//test_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile, 2);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        for (int i = 1; i <= 1; i++){
            DroneSubsystem drone = new DroneSubsystem(scheduler, i);
            Thread droneThread = new Thread(drone);
            droneThread.setName("Drone Subsystem " + i);
            droneThread.start();
        }
    }
}
