import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


/**
 * Unit tests for the Scheduler class.
 * Ensures proper event handling and synchronization using FireIncidentSubsystem.
 */
class DroneUDPTests {
    private static DatagramSocket socket;
    private static DroneSubsystem drone;
    private static Scheduler scheduler;
    private static InetAddress localhost;

    @BeforeAll
    public static void setUpOnce() throws Exception {
        MetricsLogger logger = new MetricsLogger();
        MapUI mapUI = new MapUI();
        String fireIncidentFile = "src//input//test_event_file.csv";
        String zoneFile = "src//input//test_zone_file_4.csv";
        int port = 6500;
        int droneID = 123;
        localhost = InetAddress.getLocalHost();
        socket = new DatagramSocket(port + droneID);
        scheduler = new Scheduler(zoneFile, fireIncidentFile, 2, 345, mapUI, logger);
        drone = new DroneSubsystem(scheduler, droneID, 111, mapUI, logger);
    }

    @AfterAll
    public static void cleanup() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Test
    public void testSendRequestFromDroneToScheduler() {

        new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Obtain the data from the packet
                ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
                Object data = objectInputStream.readObject();

                // Send back an acknowledgement
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
                objectStream.writeObject("ACK:" + data);
                objectStream.flush();
                byte[] ackData = byteStream.toByteArray();
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, packet.getAddress(), packet.getPort());
                socket.send(ackPacket);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }).start();

        Object response = drone.sendRequest("testMethod", "testParameter");
        assertTrue(response.toString().contains("ACK:[testMethod, testParameter, 123]"), "Drone RPC works.");
    }
}