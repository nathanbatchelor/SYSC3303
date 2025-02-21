public class FireIncidentSimulation {
    public static void main(String[] args) {
        String fireIncidentFile = "SYSC3303/input/test_event_file.csv";
        String zoneFile = "SYSC3303/input/test_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        for (int i = 1; i < 2; i++){
            //String droneName = "droneSubsystem" + i;
            DroneSubsystem droneName = new DroneSubsystem(scheduler);
            Thread droneThread = new Thread(droneName);
            droneThread.setName("Drone Subsystem " + i);
            droneThread.start();
        }
    }
}
