import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FireIncidentSimulation {

    public static void main(String[] args) {
        String fireIncidentFile = "src//input//test_event_file_with_faults.csv";
        String zoneFile = "src//input//test_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile, 5, 0);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        for (int i = 1; i <= 2; i++){
            DroneSubsystem drone = new DroneSubsystem(scheduler, i, 0);
            Thread droneThread = new Thread(drone);
            droneThread.setName("Drone Subsystem " + i);
            droneThread.start();
        }
    }
}
