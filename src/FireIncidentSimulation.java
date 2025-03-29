import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FireIncidentSimulation {

    public static void main(String[] args) {
        String fireIncidentFile = "C:\\Users\\Nathan\\OneDrive\\Desktop\\Sysc3303\\SYSC3303\\src\\input\\test_event_file_with_faults.csv";
        String zoneFile = "C:\\Users\\Nathan\\OneDrive\\Desktop\\Sysc3303\\SYSC3303\\src\\input\\test_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile, 5);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        for (int i = 1; i <= 5; i++){
            DroneSubsystem drone = new DroneSubsystem(scheduler, i);
            Thread droneThread = new Thread(drone);
            droneThread.setName("Drone Subsystem " + i);
            droneThread.start();
        }
    }
}
