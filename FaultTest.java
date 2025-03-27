import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FaultTest {

    ByteArrayOutputStream outputStream;
    PrintStream printStream;
    String output;

    @BeforeEach
    public void setUp() {
        // setup streams to collect output (used to verify faults)
        outputStream = new ByteArrayOutputStream();
        printStream = new PrintStream(outputStream);
        System.setOut(printStream);
        output = outputStream.toString().trim();
    }

    @Test
    public void testArrivalFault() throws InterruptedException {
        // setup scheduler and drone for an arrival fault
        String fireIncidentFile = "input//test_event_file_with_faults.csv";
        String zoneFile = "input//test_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile, 2);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        DroneSubsystem drone = new DroneSubsystem(scheduler, 1);
        Thread droneThread = new Thread(drone);
        droneThread.setName("Drone Subsystem " + 1);
        droneThread.start();

        // wait for threads to finish
        schedulerThread.join();
        droneThread.join();

        assertTrue(output.contains("[Drone " + 1 + "] ARRIVAL fault injected — drone will not move toward target."));
        assertTrue(output.contains("[Drone " + 1 + "] Fault detected: drone did not arrive in time."));
    }

    @Test
    public void testNozzleFault() throws InterruptedException {
        // setup scheduler and drone for a nozzle fault
        String fireIncidentFile = "input//test_event_file_with_nozzle_fault.csv";
        String zoneFile = "input//test_zone_file.csv";

        Scheduler scheduler = new Scheduler(zoneFile, fireIncidentFile, 2);
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        DroneSubsystem drone = new DroneSubsystem(scheduler, 1);
        Thread droneThread = new Thread(drone);
        droneThread.setName("Drone Subsystem " + 1);
        droneThread.start();

        // wait for threads to end
        schedulerThread.join();
        droneThread.join();

        assertTrue(output.contains("[Drone " + 1 + "] NOZZLE fault injected — nozzle stuck open."));
    }
}
