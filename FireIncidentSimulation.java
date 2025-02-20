public class FireIncidentSimulation {
    public static void main(String[] args) {
        String fireIncidentFile = "input//test_event_file.csv";
        String zoneFile = "input//test_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        for (int i = 1; i < 3; i++){
            //String droneName = "droneSubsystem" + i;
            DroneSubsystem droneName = new DroneSubsystem(scheduler);
            Thread droneThread = new Thread(droneName);
            droneThread.setName("Drone Subsystem " + i);
            droneThread.start();
        }
    }
}
