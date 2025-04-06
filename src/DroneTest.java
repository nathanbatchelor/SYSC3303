import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.*;

import static org.junit.jupiter.api.Assertions.*;


public class DroneTest {

    private static Scheduler scheduler;
    private static int port = 5500;
    private static int idnum;
    private static InetAddress localhost;
    private static DatagramSocket socket;

    @BeforeAll
    public static void setUpOnce() throws UnknownHostException, SocketException {
        MetricsLogger logger = new MetricsLogger();
        MapUI mapUI = new MapUI();
        String fireIncidentFile = "src//input//test_event_file.csv";
        String zoneFile = "src//input//test_zone_file.csv";
        scheduler = new Scheduler(zoneFile, fireIncidentFile, 2, 200, mapUI, logger);
        localhost = InetAddress.getLocalHost();
        socket = new DatagramSocket(port);
    }

    @AfterAll
    public static void cleanup() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Test
    public void testDroneRPCSend()  {
        try {
            String response = "TEST";
            InetAddress address = InetAddress.getLocalHost();
            scheduler.droneRPCSend(response, 0);

            byte[] buffer = new byte[4096];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

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
