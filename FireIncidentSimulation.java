public class FireIncidentSimulation {
    public static void main(String[] args) {
        String fireIncidentFile = "input//test_event_file.csv";
        String zoneFile = "input//test_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile,2);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();
//        DroneSubsystem droneSubsystem1 = new DroneSubsystem(scheduler);
//        Thread droneThread1 = new Thread(droneSubsystem1);
//        droneThread1.setName("Drone Subsystem 1");
//        droneThread1.start();
//
//        DroneSubsystem droneSubsystem2 = new DroneSubsystem(scheduler);
//        Thread droneThread2 = new Thread(droneSubsystem2);
//        droneThread2.setName("Drone Subsystem 2");
//        droneThread2.start();

        for (int i = 1; i <= 2; i++){
            //String droneName = "droneSubsystem" + i;Pandam
            DroneSubsystem droneName = new DroneSubsystem(scheduler,i);
            Thread droneThread = new Thread(droneName);
            droneThread.setName("Drone Subsystem " + i);
            droneThread.start();
        }
    }
}
