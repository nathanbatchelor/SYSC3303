import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.*;
import static org.junit.jupiter.api.Assertions.*;

public class SchedulerUDPTests {

    private static Scheduler scheduler;
    private static FireIncidentSubsystem fis;
    private static int dronePort = 5500;
    private static int fisPort = 5000;
    private static InetAddress localhost;
    private static DatagramSocket droneSocket;
    private static DatagramSocket fisSocket;

    @BeforeAll
    public static void setUpOnce() throws UnknownHostException, SocketException {
        MetricsLogger logger = new MetricsLogger();
        MapUI mapUI = new MapUI();
        String fireIncidentFile = "src//input//test_event_file.csv";
        String zoneFile = "src//input//test_zone_file.csv";
        scheduler = new Scheduler(zoneFile, fireIncidentFile, 2, 200, mapUI, logger);
        localhost = InetAddress.getLocalHost();
        droneSocket = new DatagramSocket(dronePort);
        fisSocket = new DatagramSocket(fisPort);
        fis = new FireIncidentSubsystem("test.csv", 0, 0, 0, 10, 10, 200);
    }

    @AfterAll
    public static void cleanup() {
        if (droneSocket != null && !droneSocket.isClosed()) {
            droneSocket.close();
        }
        if (fisSocket != null && !fisSocket.isClosed()) {
            fisSocket.close();
        }
    }

    @Test
    public void testDroneRPCSend()  {
        try {
            String response = "TEST";
            scheduler.droneRPCSend(response, 0);

            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            droneSocket.receive(packet);

            // Obtain the data from the packet
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
            Object data = objectInputStream.readObject();

            assertEquals("TEST", data);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Test
    public void testFISRPCSend()  {
        try {
            String response = "TEST";
            scheduler.FISRPCSend(response, 0);

            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            fisSocket.receive(packet);

            // Obtain the data from the packet
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
            Object data = objectInputStream.readObject();

            assertEquals("TEST", data);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}