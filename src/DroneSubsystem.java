import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DroneSubsystem implements Runnable {
    private final Scheduler scheduler;
    private final int capacity = 14;  // Max 14L per trip
    private final double cruiseSpeed = 18.0;
    private final double takeoffSpeed = 2.0;
    private final int nozzleFlowRate = 2; // 2L per second
    private final int idNum;
    private double batteryLife = 1800; // Battery Life of Drone
    private int remainingAgent;
    private int currentX = 0;
    private int currentY = 0;
    private DroneState currentState;
    private DatagramSocket socket;
    private InetAddress schedulerAddress;
    private final int DEFAULT_DRONE_PORT = 5500;
    private boolean busy = false;

    private Timer travelTimer;
    private boolean arrivedAtFireZone = false;

    public enum DroneState {
        IDLE,
        ON_ROUTE,
        DROPPING_AGENT,
        RETURNING
    }

    // Sends an RPC request as a serialized list: [methodName, param1, param2, …, droneId]
    public Object sendRequest(String methodName, Object... parameters) {
        try {
            System.out.println("Drone " + idNum + " sending request: " + methodName);
            List<Object> requestList = new ArrayList<>();
            requestList.add(methodName);
            requestList.addAll(Arrays.asList(parameters));
            requestList.add(idNum);  // Include drone ID

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
            objStream.writeObject(requestList);
            objStream.flush();
            byte[] requestData = byteStream.toByteArray();

            // Send to scheduler on port 6500+idNum
            DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, schedulerAddress, 6500 + idNum);
            socket.send(requestPacket);
            System.out.println("Drone " + idNum + " request sent, waiting for response...");

            while (true) {
                try {
                    byte[] responseBuffer = new byte[4096]; // increased buffer size
                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                    socket.receive(responsePacket);
                    ObjectInputStream inputStream = new ObjectInputStream(
                            new ByteArrayInputStream(responsePacket.getData(), 0, responsePacket.getLength()));
                    Object response = inputStream.readObject();
                    System.out.println("Drone " + idNum + " received response: " + response);
                    return response;
                } catch (SocketTimeoutException e) {
                    System.out.println("Drone " + idNum + " waiting for response...");
                    continue;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    return "ERROR: Failed to receive response.";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR: IOException.";
        }
    }

    public DroneSubsystem(Scheduler scheduler, int idNum) {
        try {
            socket = new DatagramSocket(DEFAULT_DRONE_PORT + idNum);
            schedulerAddress = InetAddress.getLocalHost();
            System.out.println("DroneSubsystem " + idNum + " is listening on port " + (DEFAULT_DRONE_PORT + idNum));
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
        this.idNum = idNum;
        this.scheduler = scheduler;
        this.remainingAgent = capacity;
        this.currentState = DroneState.IDLE;
    }

    public void displayState() {
        switch (currentState) {
            case IDLE:
                System.out.println("Drone is currently idle.");
                break;
            case ON_ROUTE:
                System.out.println("Drone is on route to fire.");
                break;
            case DROPPING_AGENT:
                System.out.println("Drone is dropping agent on fire.");
                break;
            case RETURNING:
                System.out.println("Drone is returning to base.");
                break;
        }
    }

    private void takeoff() {
        System.out.println(Thread.currentThread().getName() + " taking off to 20m altitude...");
        sleep((long) (5000 * takeoffSpeed));
        System.out.println(Thread.currentThread().getName() + " reached cruising altitude.");
    }

    private void descend() {
        System.out.println(Thread.currentThread().getName() + " descending to base...");
        sleep((long) (5000 * takeoffSpeed));
        System.out.println(Thread.currentThread().getName() + " reached ground station.");
    }



    // This is broken, need to fix
    private FireEvent travelToZoneCenter(double fullTravelTime, FireEvent targetEvent) {
        // Compute the target zone center from the event.
        String[] zoneCoords = targetEvent.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");
        int destX = (Integer.parseInt(startCoords[0].trim()) + Integer.parseInt(endCoords[0].trim())) / 2;
        int destY = (Integer.parseInt(startCoords[1].trim()) + Integer.parseInt(endCoords[1].trim())) / 2;

        int startX = currentX;
        int startY = currentY;

        if ("ARRIVAL_TIMEOUT".equalsIgnoreCase(targetEvent.fault)) {
            System.out.println("[Drone " + idNum + "] FAULT injected: Simulating stuck drone en route to zone " + targetEvent.getZoneId());
            // Simulate drone doing nothing, letting the timer expire
            while (true) {
                sleep(1000);  // Stay stuck in place
                batteryLife -= 1;
            }
        }

        // divide the travel into one-second increments.
        int steps = (int) Math.ceil(fullTravelTime);
        for (int i = 1; i <= steps; i++) {
            double fraction = (double) i / steps;
            // Update position along the straight line from (startX, startY) to (destX, destY).
            currentX = startX + (int) ((destX - startX) * fraction);
            currentY = startY + (int) ((destY - startY) * fraction);
            sleep(1000);  // simulate one second of travel
            batteryLife -= 1; // decrement battery by 1 second

            // At each step, check if there is an on-route event.
            // The scheduler returns an event if one is within a predefined threshold.

            FireEvent newEvent = (FireEvent) sendRequest("getNextAssignedEvent",Thread.currentThread().getName(),currentX,currentY);

            // If a new event is found and it is different from the one we’re already targeting...
            if (newEvent != null && newEvent != targetEvent) {
                System.out.println(Thread.currentThread().getName() + " found on-route event at zone " + newEvent.getZoneId() +
                        " while en route to zone " + targetEvent.getZoneId() + ". Switching assignment.");
                // Re-add the original event back to the queue.
                scheduler.addFireEvent(targetEvent);
                return newEvent;
            }
        }
        // Completed travel to target zone center.
        currentX = destX;
        currentY = destY;
        arrivedAtFireZone = true; // Prevent fault
        if (travelTimer != null) {
            travelTimer.cancel();
            travelTimer = null;
        }
        return targetEvent;
    }

    public void extinguishFire(int amount) {
        System.out.println("\n" + Thread.currentThread().getName() + " opening nozzle...");
        sleep(1000);
        batteryLife -= 1;
        currentState = DroneState.DROPPING_AGENT;
        displayState();
        int timeToDrop = amount / nozzleFlowRate;
        System.out.println(Thread.currentThread().getName() + " dropping " + amount + "L of firefighting agent at " + nozzleFlowRate + "L/s.");
        sleep(timeToDrop * 1000);
        batteryLife -= timeToDrop;
        remainingAgent -= amount;
        System.out.println(Thread.currentThread().getName() + " Dispensed " + amount + "L. Remaining capacity: " + remainingAgent + "L.");
        System.out.println("\n" + Thread.currentThread().getName() + " closing nozzle...");
        sleep(1000);
        batteryLife -= 1;
        System.out.println(Thread.currentThread().getName() + " nozzle closed.\n");
    }

    public void returnToBase(FireEvent event) {
        currentState = DroneState.RETURNING;
        displayState();
        System.out.println("\n" + Thread.currentThread().getName() + " returning to base...\n");
        double distance = (double) sendRequest("calculateDistanceToHomeBase", event);
        sleep((long) ((distance / cruiseSpeed) * 1000));
        System.out.println();
        descend();
        System.out.println("----------------------------------------\n");
        currentX = 0;
        currentY = 0;
        currentState = DroneState.IDLE;
        displayState();
    }

    private void makeDroneIdleAndRecharge(FireEvent lastEvent) {
        returnToBase(lastEvent);
        currentState = DroneState.IDLE;
        displayState();
        remainingAgent = capacity;
        batteryLife = 1800;
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void startTravelFaultTimer(double travelTimeSeconds, FireEvent event) {
        long timeout = (long) (travelTimeSeconds * 1000 * 1.1); // 1.5x buffer
        travelTimer = new Timer();
        travelTimer.schedule(new TimerTask() {
            public void run() {
            if (!arrivedAtFireZone && currentState == DroneState.ON_ROUTE) {
                System.out.println("[Drone " + idNum + "] Fault detected: drone did not arrive in time.");
                sendRequest("handleDroneFault",event,"timeout",idNum);
                //sendRequest("FAULT", idNum, "ARRIVAL_TIMEOUT", event);  // This will need to change, just here as an example, replace FAULT with the new method
                // You could optionally shut down the drone here:
                System.out.println("[Drone " + idNum + "] Initiating shutdown due to fault.");
                makeDroneIdleAndRecharge(event);
                //exit(1); // simulate drone failure this is temporary
            }
            }
        }, timeout);
    }


    @Override
    public void run() {
        System.out.println("Running DroneSubsystem " + idNum);
        try {
            // Outer loop: keep checking for new fire events.
            while (true) {
                FireEvent event = (FireEvent) sendRequest("getNextFireEvent");
                busy=true;
                if (event == null) {
                    busy=false;
                    System.out.println("No event found. Shutting down drone " + idNum + ".");
                    break; // Exit the loop when no event is available.
                }
                // Process the received event.
                while (event != null) {
                    displayState();
                    if (currentX == 0 && currentY == 0) {
                        takeoff();
                    }
                    double travelTime = (double) sendRequest("calculateTravelTime", currentX, currentY, event);
                    System.out.println("[Drone " + idNum + "] Travel time: " + travelTime);
                    arrivedAtFireZone = false;
                    startTravelFaultTimer(travelTime, event); // event is the current FireEvent
                    if ("ARRIVAL".equalsIgnoreCase(event.getFault())) {
                        System.out.println("[Drone " + idNum + "] ARRIVAL fault injected — drone will not move toward target.");
                        // Drone stays put, timer will go off
                        sleep((long) (travelTime * 1000 * 2));  // simulate drone doing nothing
                    }

                    travelToZoneCenter(travelTime, event);
                    int waterToDrop = Math.min(event.getLitres(), remainingAgent);
                    extinguishFire(waterToDrop);
                    System.out.println("FAULT" + event.getFault());
                    if ("NOZZLE".equalsIgnoreCase(event.getFault())) {
                        System.out.println("[Drone " + idNum + "] NOZZLE fault injected — nozzle stuck open.");
                        sendRequest("handleDroneFault", event, "nozzle", idNum); // or whatever your fault method is
                        System.exit(1); // hard fault shutdown
                    }
                    sendRequest("updateFireStatus", event, waterToDrop);
                    FireEvent lastEvent = event;

                    // If the drone runs out of agent, return to base.
                    if (remainingAgent <= 0) {
                        System.out.println("Drone " + idNum + " has run out of agent. Returning to base.");
                        makeDroneIdleAndRecharge(lastEvent);
                        break; // Break out of the inner loop.
                    }

                    // Request an additional event.
                    FireEvent event2 = (FireEvent) sendRequest("getAdditionalFireEvent", batteryLife, currentX, currentY);
                    if (event2 == null) {
                        System.out.println("No additional event. Returning to base.");
                        makeDroneIdleAndRecharge(lastEvent);
                        break; // Break out of the inner loop.
                    } else {
                        event = event2;
                    }
                } // end inner loop

                // After finishing an event sequence, check again for a new event.
            } // end outer loop
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("DroneSubsystem " + idNum + " shutting down.");
    }
}
