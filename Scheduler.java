import java.io.*;
import java.net.*;
import java.util.*;

public class Scheduler implements Runnable {
    private final Queue<FireEvent> queue = new LinkedList<>();
    private final Map<Integer, FireIncidentSubsystem> zones = new HashMap<>();
    private final String zoneFile;
    private final String eventFile;
    private volatile boolean isFinished = false;
    private volatile boolean isLoaded = false;
    private SchedulerState state = SchedulerState.WAITING_FOR_EVENTS;
    public static final int DEFAULT_FIS_PORT = 5000;
    private final int DEFAULT_DRONE_PORT = 5500;
    private DatagramSocket dronesendSocket;
    private DatagramSocket FISsendSocket;
    private ArrayList<DatagramSocket> FIS_Sockets = new ArrayList<>();
    private ArrayList<DatagramSocket> drone_Sockets = new ArrayList<>();
    private boolean zonesLoaded = false;

    public static class DroneStatus {
        public String droneId;
        public int x;
        public int y;
        public double batteryLife;
    }

    public enum SchedulerState {
        WAITING_FOR_EVENTS,
        ASSIGNING_DRONE,
        WAITING_FOR_DRONE,
        SHUTTING_DOWN,
    }

    public Scheduler(String zoneFile, String eventFile, int numDrones) {
        this.zoneFile = zoneFile;
        this.eventFile = eventFile;
        for (int i = 1; i <= numDrones; i++) {
            try {
                DatagramSocket drone_socket = new DatagramSocket(6500 + i);
                drone_socket.setSoTimeout(1000);
                drone_Sockets.add(drone_socket);
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            FISsendSocket = new DatagramSocket(6000);
            dronesendSocket = new DatagramSocket(6001);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        readZoneFile();
    }

    public void readZoneFile() {
        try {
            File file = new File(this.zoneFile);
            System.out.println("Checking path: " + file.getAbsolutePath());
            if (!file.exists()) {
                System.out.println("Zone file does not exist");
                return;
            }
            System.out.println("Attempting to read file: " + zoneFile);
            try (BufferedReader br = new BufferedReader(new FileReader(zoneFile))) {
                String line;
                boolean isFirstLine = true;
                while ((line = br.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }
                    System.out.println("Reading line: " + line);
                    String[] tokens = line.split(",");
                    if (tokens.length != 3) {
                        System.out.println("Invalid Line: " + line);
                        continue;
                    }
                    try {
                        int zoneId = Integer.parseInt(tokens[0].trim());
                        int[] startCoords = parseCoordinates(tokens[1].trim());
                        int[] endCoords = parseCoordinates(tokens[2].trim());
                        if (startCoords == null || endCoords == null) {
                            System.out.println("Invalid Coordinates: " + line);
                            continue;
                        }
                        int x1 = startCoords[0], y1 = startCoords[1];
                        int x2 = endCoords[0], y2 = endCoords[1];
                        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(eventFile, zoneId, x1, y1, x2, y2);
                        zones.put(zoneId, fireIncidentSubsystem);
                        DatagramSocket socket = new DatagramSocket(6100 + zoneId);
                        socket.setSoTimeout(1000);
                        FIS_Sockets.add(socket);
                        Thread thread = new Thread(fireIncidentSubsystem);
                        thread.setName("Fire Incident Subsystem Zone: " + zoneId);
                        thread.start();
                    } catch (NumberFormatException e) {
                        System.out.println("Error parsing numbers in line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + zoneFile);
        }
    }

    public synchronized void setEventsLoaded() {
        if (!isLoaded) {
            isLoaded = true;
            state = SchedulerState.WAITING_FOR_DRONE;
            System.out.println("Scheduler: Fire events are loaded. Notifying waiting drones...");
            notifyAll();
        }
    }

    private int[] parseCoordinates(String coordinate) {
        coordinate = coordinate.replaceAll("[()]", "");
        String[] parts = coordinate.split(";");
        if (parts.length != 2) return null;
        try {
            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            return new int[]{x, y};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public synchronized void waitForEvents() {
        while (!isLoaded) {
            try {
                System.out.println("Scheduler: Waiting for fire events to be loaded...");
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void addFireEvent(FireEvent event) {
        int totalWaterNeeded = calculateWaterNeeded(event.getSeverity());
        event.setLitres(totalWaterNeeded);
        queue.add(event);
        notifyAll();
        System.out.println("Scheduler: Added FireEvent → " + event);
    }

    private int calculateWaterNeeded(String severity) {
        return switch (severity.toLowerCase()) {
            case "low" -> 10;
            case "moderate" -> 20;
            case "high" -> 30;
            default -> 0;
        };
    }

    public double calculateTravelTime(int xDrone, int yDrone, FireEvent event) {
        int cruiseSpeed = 18;
        System.out.println("Calculating travel time");
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");
        int x1 = Integer.parseInt(startCoords[0].trim());
        int y1 = Integer.parseInt(startCoords[1].trim());
        int x2 = Integer.parseInt(endCoords[0].trim());
        int y2 = Integer.parseInt(endCoords[1].trim());
        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;
        double distance = Math.sqrt(Math.pow(centerX - xDrone, 2) + Math.pow(centerY - yDrone, 2));
        double travelTimeToFire = distance / cruiseSpeed;
        System.out.println("\nScheduler: Travel time to fire: " + travelTimeToFire);
        return travelTimeToFire;
    }

    public double calculateDistanceToHomeBase(FireEvent event) {
        int homeBaseX = 0;
        int homeBaseY = 0;
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");
        int x1 = Integer.parseInt(startCoords[0].trim());
        int y1 = Integer.parseInt(startCoords[1].trim());
        int x2 = Integer.parseInt(endCoords[0].trim());
        int y2 = Integer.parseInt(endCoords[1].trim());
        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;
        double distanceToHomeBase = Math.sqrt(Math.pow(centerX - homeBaseX, 2) + Math.pow(centerY - homeBaseY, 2));
        System.out.println("\nScheduler: Distance to home base is: " + distanceToHomeBase + " meters\n" +
                "Scheduler: Time to Home Base is: " + distanceToHomeBase / 18 + " seconds\n");
        return distanceToHomeBase;
    }

    public synchronized FireEvent getNextAssignedEvent(String droneId, int currentX, int currentY) {
        double threshold = 50.0; // meters
        for (FireEvent event : queue) {
            int[] center = calculateZoneCenter(event);
            double distance = Math.sqrt(Math.pow(center[0] - currentX, 2) + Math.pow(center[1] - currentY, 2));
            if (distance <= threshold) {
                queue.remove(event);
                return event;
            }
        }
        if (!queue.isEmpty()) {
            return queue.poll();
        }
        return null;
    }

    private int[] calculateZoneCenter(FireEvent event) {
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");
        int centerX = (Integer.parseInt(startCoords[0].trim()) + Integer.parseInt(endCoords[0].trim())) / 2;
        int centerY = (Integer.parseInt(startCoords[1].trim()) + Integer.parseInt(endCoords[1].trim())) / 2;
        return new int[]{centerX, centerY};
    }

    public synchronized FireEvent getNextFireEvent() {
        while (queue.isEmpty()) {
            if (isFinished) {
                System.out.println("Scheduler: No more fire events. Notifying all waiting drones to stop.");
                notifyAll();
                return null;
            }
            try {
                System.out.println("Scheduler: Waiting for fire events to be added...");
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        FireEvent event = queue.poll();
        System.out.println("Scheduler: Sending fire event to drone: " + event);
        return event;
    }

    public synchronized FireEvent getAdditionalFireEvent(double batteryLife, int x, int y) {
        for (FireEvent currentEvent : queue) {
            double range = calculateTravelTime(x, y, currentEvent);
            double travelToHome = calculateDistanceToHomeBase(currentEvent);
            if (range + travelToHome < batteryLife) {
                System.out.println("\nSending new event to the drone\n");
                queue.remove(currentEvent);
                return currentEvent;
            }
        }
        return null;
    }

    public synchronized void updateFireStatus(FireEvent event, int waterDropped) {
        event.removeLitres(waterDropped);
        int remainingLiters = event.getLitres();
        if (remainingLiters > 0 && waterDropped > 0) {
            System.out.println("Scheduler: Fire at Zone: " + event.getZoneId() + " still needs " + remainingLiters + "L.");
            ((LinkedList<FireEvent>) queue).addFirst(event);
            notifyAll();
        } else {
            markFireExtinguished(event);
        }
    }

    public Map<Integer, FireIncidentSubsystem> getZones() {
        return zones;
    }

    public synchronized void markFireExtinguished(FireEvent event) {
        System.out.println("\nScheduler: Fire at Zone: " + event.getZoneId() + " Extinguished\n");
        if (queue.isEmpty()) {
            System.out.println("Scheduler: All fire events have been marked as extinguished. Shutting down.");
            state = SchedulerState.SHUTTING_DOWN;
            isFinished = true;
            notifyAll();
        }
    }

    public synchronized void removeFireEvent(FireEvent event) {
        queue.remove(event);
    }

    public synchronized void editFireEvent(FireEvent event, int litres) {
        event.removeLitres(litres);
    }

    public synchronized void finish() {
        isFinished = true;
        notifyAll();
    }

    public synchronized boolean isFinished() {
        return isFinished;
    }

    public synchronized boolean isEventsLoaded() {
        return isLoaded;
    }

    @Override
    public synchronized void run() {
        byte[] buffer = new byte[4096];
        ArrayList<String> knownFISMethods = new ArrayList<>(Arrays.asList("ADD_FIRE_EVENT", "SET_EVENTS_LOADED"));
        ArrayList<String> knowndroneMethods = new ArrayList<>(Arrays.asList(
                "getNextAssignedEvent", "ADD_FIRE_EVENT", "calculateDistanceToHomeBase",
                "getNextFireEvent", "calculateTravelTime", "updateFireStatus", "getAdditionalFireEvent"));
        try {
            FISsendSocket.setSoTimeout(1000);
            dronesendSocket.setSoTimeout(1000);
        } catch (SocketException e) {
            System.out.println("Error in scheduler run");
            throw new RuntimeException(e);
        }

        // Variables for graceful shutdown after finishing events
        long lastRequestTime = System.currentTimeMillis();
        final long GRACE_PERIOD_MS = 30000; // e.g., 30 seconds

        while (true) {
            boolean messageProcessed = false;
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                // Process messages from FIS sockets
                for (DatagramSocket FIS_Socket : FIS_Sockets) {
                    try {
                        FIS_Socket.receive(packet);
                        lastRequestTime = System.currentTimeMillis();
                        ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                        ObjectInputStream objStream = new ObjectInputStream(byteStream);
                        Object obj = objStream.readObject();
                        if (obj instanceof List) {
                            List<Object> list = (List<Object>) obj;
                            if (knownFISMethods.contains(list.get(0))) {
                                System.out.println("Calling invokeMethod for FIS");
                                invokeMethod((String) list.get(0), list.subList(1, list.size()), true);
                                messageProcessed = true;
                            }
                        }
                    } catch (Exception e) {
                        // Timeout is expected; continue
                    }
                }

                // Process messages from Drone sockets
                for (DatagramSocket drone_Socket : drone_Sockets) {
                    try {
                        drone_Socket.receive(packet);
                        lastRequestTime = System.currentTimeMillis();
                        ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                        ObjectInputStream objStream = new ObjectInputStream(byteStream);
                        Object obj = objStream.readObject();
                        if (obj instanceof List) {
                            List<Object> list = (List<Object>) obj;
                            if (knowndroneMethods.contains(list.get(0))) {
                                System.out.println("Calling invokeMethod for Drone");
                                invokeMethod((String) list.get(0), list.subList(1, list.size()), false);
                                messageProcessed = true;
                            }
                        }
                    } catch (Exception e) {
                        // Timeout is expected; continue
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception in scheduler run");
                throw new RuntimeException(e);
            }

            // If all events are finished, immediately respond to "getAdditionalFireEvent" requests with null.
            // (This is handled in invokeMethod; see the getAdditionalFireEvent branch.)

            // Check if no message has been processed for a prolonged period.
            if (isFinished && System.currentTimeMillis() - lastRequestTime > GRACE_PERIOD_MS) {
                break;
            }

            // Optional: Sleep a short period to avoid busy-waiting if no message was processed.
            if (!messageProcessed) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        System.out.println("Scheduler exited run loop");
        // Example: In your main simulation class after all threads finish.
        for (FireIncidentSubsystem fis : this.getZones().values()) {
            fis.shutdown();
        }

    }

    private Object invokeMethod(String methodName, List<Object> params, boolean from) {
        System.out.println("invokeMethod: " + methodName);
        if (methodName.equals("ADD_FIRE_EVENT")) {
            FireEvent event = (FireEvent) params.get(0);
            System.out.println("Received event: " + event);
            addFireEvent(event);
            if (from) {
                FISRPCSend("ACK:", (Integer) params.get(0)); // dummy value; adjust as needed
                FISRPCSend("SUCCESS", (Integer) params.get(0));
            } else {
                droneRPCSend("ACK:done", (Integer) params.get(1));
            }
        } else if (methodName.equals("getNextAssignedEvent")) {
            String droneId = (String) params.get(0);
            int currentX = (Integer) params.get(1);
            int currentY = (Integer) params.get(2);
            FireEvent event = getNextAssignedEvent(droneId, currentX, currentY);
            droneRPCSend(event, (Integer) params.get(3));
        } else if (methodName.equals("calculateDistanceToHomeBase")) {
            FireEvent event = (FireEvent) params.get(0);
            double distance = calculateDistanceToHomeBase(event);
            droneRPCSend(distance, (Integer) params.get(1));
        } else if (methodName.equals("getNextFireEvent")) {
            System.out.println("Sending drone the event");
            FireEvent event = getNextFireEvent();
            droneRPCSend(event, (Integer) params.get(0));
        } else if (methodName.equals("calculateTravelTime")) {
            int x = (Integer) params.get(0);
            int y = (Integer) params.get(1);
            FireEvent event = (FireEvent) params.get(2);
            double travelTime = calculateTravelTime(x, y, event);
            droneRPCSend(travelTime, (Integer) params.get(3));
        } else if (methodName.equals("updateFireStatus")) {
            FireEvent event = (FireEvent) params.get(0);
            int waterDropped = (Integer) params.get(1);
            updateFireStatus(event, waterDropped);
            droneRPCSend("ACK:done", (Integer) params.get(2));
        } else if (methodName.equals("getAdditionalFireEvent")) {
            FireEvent event = getAdditionalFireEvent((Double) params.get(0), (Integer) params.get(1), (Integer) params.get(2));
            droneRPCSend(event, (Integer) params.get(3));
        } else if (methodName.equals("SET_EVENTS_LOADED")) {
            setEventsLoaded();
            FISRPCSend("ACK:", (Integer) params.get(0));
            FISRPCSend("SUCCESS", (Integer) params.get(0));
        } else {
            System.out.println("Unknown method: " + methodName);
            return "FAILED";
        }
        return "???";
    }

    public synchronized void droneRPCSend(Object response, int idnum) {
        try {
            System.out.println("Sending response to drone " + idnum + ": " + response);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
            objStream.writeObject(response);
            objStream.flush();
            byte[] responseData = byteStream.toByteArray();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, InetAddress.getLocalHost(), DEFAULT_DRONE_PORT + idnum);
            dronesendSocket.send(responsePacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void FISRPCSend(Object message, int zone) {
        try {
            System.out.println("Sending FIS message to zone " + zone + ": " + message);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
            objStream.writeObject(message);
            objStream.flush();
            byte[] responseData = byteStream.toByteArray();
            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, InetAddress.getLocalHost(), DEFAULT_FIS_PORT + zone);
            FISsendSocket.send(responsePacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
