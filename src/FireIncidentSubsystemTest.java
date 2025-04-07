import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class FireIncidentSubsystemTest {

    private static DatagramSocket socket;
    private static FireIncidentSubsystem fis;
    private static int port = 6200;
    private static InetAddress localhost;

    @BeforeAll
    public static void setUpOnce() throws Exception {
        localhost = InetAddress.getLocalHost();
        socket = new DatagramSocket(port);
        fis = new FireIncidentSubsystem("test.csv", 0, 0, 0, 10, 10, 200);
    }

    @AfterAll
    public static void cleanup() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Test
    public void testRpcSendFireIncidentSubsytem() throws Exception {
        // Test if sending data through rpc send works
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

        Object response = fis.rpc_send(List.of("TEST", "fisData"), localhost, port);
        assertTrue(response.toString().startsWith("ACK:"), "rpc_send works as acknowledgement was recieved.");
    }
}