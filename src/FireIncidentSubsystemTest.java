import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FireIncidentSubsystemTest {

    private static Scheduler scheduler;
    private static FireIncidentSubsystem fis;
    private static Thread schedulerThread;

    @BeforeAll
    public static void setUpOnce() throws UnknownHostException {
        MetricsLogger logger = new MetricsLogger();
        MapUI mapUI = new MapUI();
        String fireIncidentFile = "src//input//test_event_file.csv";
        String zoneFile = "src//input//test_zone_file.csv";
        fis = new FireIncidentSubsystem(fireIncidentFile, 1, 0, 0, 100, 100,123);
        scheduler = new Scheduler(zoneFile, fireIncidentFile, 2, 234, mapUI, logger);
        schedulerThread = new Thread(scheduler);
        schedulerThread.start();
    }

    @Test
    public void testFireIncidentSubsystemUDP() throws IOException, InterruptedException {
        FireEvent event = new FireEvent("14:03:15", 1, "FIRE_DETECTED", "LOW", "ARRIVAL", fis);
        List<Object> request = new ArrayList<>();
        request.add("ADD_FIRE_EVENT");
        request.add(event);

        // Redirect System.out to capture printed output
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);
        System.setOut(printStream);  // Redirect System.out


        fis.rpc_send(request, InetAddress.getLocalHost(), 6100);
        Thread.sleep(1000);

        // Flush and get the printed output
        printStream.flush();

        // Verify if the output contains anything
        String output = byteArrayOutputStream.toString();
        System.out.println("Captured output: " + output);

    }
}