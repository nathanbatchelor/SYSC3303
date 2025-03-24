import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The DroneSubsystem class implements a subsystem that simulates a firefighting drone responding to fire events.
 * It calculates water needed, performs operations like takeoff, travel, extinguish fire, and returns to base.
 *
 * @author Ben Radley
 * @author Nathan Batchelor
 * @author Joey Andrews
 * @author Grant Phillips
 * @version 1.0
 *
 * @author Joey Andrews
 * @author Grant Phillips
 * @version 2.0
 */
public class DroneSubsystem implements Runnable {
    private final Scheduler scheduler;
    private final int capacity = 14;  // Max 14L per trip
    private final double cruiseSpeed = 18.0;  // 18 m/s
    private final double takeoffSpeed = 2.0;  // 2 m/s to 20m altitude
    private final int nozzleFlowRate = 2; // 2L per second
    private final int idNum;
    private double batteryLife = 1800; // Battery Life of Drone
    private double travelTimeToFire = 0;
    private int remainingAgent; // Amount of agent remaining
    private int currentX = 0; // Drones current X position
    private int currentY = 0; // Drones current Y position
    private DroneState currentState;
    private DatagramSocket socket;
    private InetAddress schedulerAddress;
    private final int DEFAULT_DRONE_PORT = 5500;


    public enum DroneState {
        IDLE,
        ON_ROUTE,
        DROPPING_AGENT,
        RETURNING
    }

    public Object sendRequest(String methodName, Object... parameters) {
        try {
            System.out.println(" Drone " + idNum + " sending request: " + methodName);

            // Store method name and parameters in a list
            List<Object> methodAndParameters = new ArrayList<>();
            methodAndParameters.add(methodName);
            methodAndParameters.addAll(Arrays.asList(parameters));
            methodAndParameters.add(idNum);  // Include drone ID for tracking

            // Create data to send
            ByteArrayOutputStream request = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(request);
            outputStream.writeObject(methodAndParameters);
            outputStream.flush();
            byte[] requestData = request.toByteArray();

            // Send request to the Scheduler
            DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, schedulerAddress, 6500 + idNum);
            socket.send(requestPacket);
            System.out.println(" Drone " + idNum + " request sent, waiting for response...");

            // Loop to keep waiting for a valid response
            while (true) {
                try {
                    byte[] responseBuffer = new byte[1024]; // Increase buffer size for large objects
                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                    socket.receive(responsePacket); // Blocking call, waits for response
                    System.out.println("Waiting for the response in the loop");
                    // Convert response to string
                    String message = new String(responseBuffer, 0, responsePacket.getLength());


                    // Deserialize the actual response
                    ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(responsePacket.getData(), 0, responsePacket.getLength()));
                    Object response = inputStream.readObject();

                    System.out.println(" Drone " + idNum + " received response: " + response);
                    return response; // Return the received data

                } catch (SocketTimeoutException e) {
                    System.out.println(" Drone " + idNum + " waiting for response...");
                    continue; // Keep retrying
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


    /**
     * Constructs a DroneSubsystem object with the specified scheduler.
     *
     * @param scheduler the Scheduler object responsible for handling fire events.
     */
    public DroneSubsystem(Scheduler scheduler,int idNum) {
        try {
            socket = new DatagramSocket(DEFAULT_DRONE_PORT+idNum);
            schedulerAddress = InetAddress.getLocalHost();
            System.out.println("DroneSubsystem is listening on random port.");
        } catch (SocketException se) {
            se.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.idNum=idNum;
        this.scheduler = scheduler;
        this.remainingAgent = capacity;
        this.currentState = DroneState.IDLE;
    }

    /**
     * Displays the current state of the Drone Subsystem
     */
    public void displayState() {
        switch(currentState) {
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


    public DroneState getState(){
        return currentState;
    }

    /**
     * Simulates the drone's takeoff to a cruising altitude of 20 meters.
     * The process takes 10 seconds.
     */
    private void takeoff() {
        System.out.println(Thread.currentThread().getName() + " taking off to 20m altitude...");
        sleep((long) (5000 * takeoffSpeed));
        System.out.println(Thread.currentThread().getName() + " reached cruising altitude.");
    }

    /**
     * Simulates the drone's landing at the base station.
     * The process takes 10 seconds.
     */
    private void descend() {
        System.out.println(Thread.currentThread().getName() + " descend to 20m altitude...");
        sleep((long) (5000 * takeoffSpeed));
        System.out.println(Thread.currentThread().getName() + " reached ground station.");
    }


    private double travelHomeCalculation(){
        return Math.sqrt(Math.pow(currentX, 2) + Math.pow(currentY, 2));
    }

    public void travelToZoneCenter(double travelTime, FireEvent event) {

        // Extract zone coordinates from the FireEvent
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");

        // Parse the coordinates
        int x1 = Integer.parseInt(startCoords[0].trim());
        int y1 = Integer.parseInt(startCoords[1].trim());
        int x2 = Integer.parseInt(endCoords[0].trim());
        int y2 = Integer.parseInt(endCoords[1].trim());

        // Calculate the center of the fire zone
        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;

        currentState = DroneState.ON_ROUTE;
        displayState();

        System.out.println(Thread.currentThread().getName() + ": traveling to Zone: " + event.getZoneId() + " with fire at (" + centerX + "," + centerY + ")...");
        sleep((long) (travelTime * 1000));
        batteryLife -= travelTime;
        System.out.println("Battery Life is now: " + batteryLife);
        System.out.println(Thread.currentThread().getName() + ": arrived at fire center at Zone: " + event.getZoneId());

        currentX = centerX;
        currentY = centerY;

    }

    /**
     * Simulates the drone traveling to the center of the fire zone.
     *
     * @param event the FireEvent object containing details about the fire zone.
     */
//    private FireEvent travelToZoneCenter(double fullTravelTime, FireEvent targetEvent) {
//        // Compute the target zone center from the event.
//        String[] zoneCoords = targetEvent.getZoneDetails().replaceAll("[()]", "").split(" to ");
//        String[] startCoords = zoneCoords[0].split(",");
//        String[] endCoords = zoneCoords[1].split(",");
//        int destX = (Integer.parseInt(startCoords[0].trim()) + Integer.parseInt(endCoords[0].trim())) / 2;
//        int destY = (Integer.parseInt(startCoords[1].trim()) + Integer.parseInt(endCoords[1].trim())) / 2;
//
//        int startX = currentX;
//        int startY = currentY;
//        // We'll divide the travel into one-second increments.
//        int steps = (int) Math.ceil(fullTravelTime);
//        for (int i = 1; i <= steps; i++) {
//            double fraction = (double) i / steps;
//            // Update position along the straight line from (startX, startY) to (destX, destY).
//            currentX = startX + (int) ((destX - startX) * fraction);
//            currentY = startY + (int) ((destY - startY) * fraction);
//            sleep(1000);  // simulate one second of travel
//            batteryLife -= 1; // decrement battery by 1 second
//
//            // At each step, check if there is an on-route event.
//            // The scheduler returns an event if one is within a predefined threshold.
//            FireEvent newEvent = scheduler.getNextAssignedEvent(Thread.currentThread().getName(), currentX, currentY);
//            // If a new event is found and it is different from the one we’re already targeting...
//            if (newEvent != null && newEvent != targetEvent) {
//                System.out.println(Thread.currentThread().getName() + " found on-route event at zone " + newEvent.getZoneId() +
//                        " while en route to zone " + targetEvent.getZoneId() + ". Switching assignment.");
//                // Re-add the original event back to the queue.
//                scheduler.addFireEvent(targetEvent);
//                return newEvent;
//            }
//        }
//        // Completed travel to target zone center.
//        currentX = destX;
//        currentY = destY;
//        return targetEvent;
//    }





    /**
     * Simulates the process of extinguishing a fire by dropping water.
     *
     * @param amount the amount of water to drop in liters.
     */
    public void extinguishFire(int amount) {
        System.out.println("\n" + Thread.currentThread().getName() + " opening nozzle...");
        sleep(1000); // Takes 1 second to open the nozzle
        batteryLife -= 1;

        currentState = DroneState.DROPPING_AGENT;
        displayState();

        int timeToDrop = amount / nozzleFlowRate; // Time in seconds to drop water
        System.out.println(Thread.currentThread().getName() + " dropping " + amount + "L of firefighting agent at " + nozzleFlowRate + "L/s.");
        sleep(timeToDrop * 1000);  // Time to drop all water
        batteryLife -= timeToDrop;

        //int remainingAgent = capacity - amount;
        remainingAgent -= amount;

        System.out.println( Thread.currentThread().getName() + " Dispensed " + amount + "L. Remaining capacity: " + remainingAgent + "L.");
        System.out.println("\n" + Thread.currentThread().getName() + " closing nozzle...");
        sleep(1000); // Takes 1 second to close the nozzle
        batteryLife -= 1;
        System.out.println(Thread.currentThread().getName() + " nozzle closed.\n");
    }

    /**
     * Simulates the drone's return to base and its landing process.
     * The time to base is calculated during travel, and landing takes 10 seconds.
     */
    public void returnToBase(FireEvent event) {
        currentState = DroneState.RETURNING;
        displayState();
        System.out.println("\n" +Thread.currentThread().getName() + " returning to base...\n");
        double distance = (double) sendRequest("calculateDistanceToHomeBase", event.toString());
        sleep((long) ((distance/18) * 1000));  // Use stored travel time //0,0 to zone 1, zone1 to zone2
        System.out.println();
        descend();
        System.out.println("----------------------------------------\n");
        currentX = 0;
        currentY = 0;
        currentState = DroneState.IDLE;
        displayState();
    }


    /**
     * Helper function change the drone state to idle so it can refuel and recharge.
     *
     * @param lastEvent is the last event of the drone.
     */
    private void makeDroneIdleAndRecharge(FireEvent lastEvent) {
        returnToBase(lastEvent); // Ensure the drone returns to base when out of firefighting agent
        currentState = DroneState.IDLE;
        displayState();
        remainingAgent = capacity; // Refuel agent
        batteryLife = 1800; // Recharge battery
    }


    /**
     * Helper method to pause the execution of a thread for a specified amount of time.
     * Used for simulating drones traveling
     *
     * @param milliseconds the time to sleep in milliseconds.
     */
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * The main execution method for the drone subsystem.
     * It continuously retrieves fire events, determines the water needed, and performs the sequence
     * of operations to respond to and extinguish the fire.
     */
    @Override
    public void run() {
        System.out.println("running drone------------------------------------------------------------------");
        try {
            while (true) {

                String example = (String) sendRequest("getNextFireEvent");
                System.out.println("DRONE TEST: This is my current fire Event before recration: " + example);
                FireEvent event = new FireEvent(example, scheduler.getZones());
                if (event == null) {
                    System.out.println("No event found.");
                    break;
                }

                System.out.println("DRONE TEST: This is my current fire Event after recration: " + event);

                System.out.println(Thread.currentThread().getName() + " responding to event: " + event);

                while (event != null) {
                    displayState();
                    if (currentX == 0 && currentY == 0) {
                        takeoff();
                    }
                    double travelTime = (double) sendRequest("calculateTravelTime", currentX, currentY, event.toString());


                    travelToZoneCenter(travelTime, event);

                    int waterToDrop = Math.min(event.getLitres(), remainingAgent);
                    extinguishFire(waterToDrop);
                    String newStr = (String) sendRequest("updateFireStatus", event.toString(), waterToDrop);
                    System.out.println("Might be stuck here: Sending Request: " + newStr);
                    event = new FireEvent(newStr,scheduler.getZones());
                    FireEvent lastEvent = event;

                    if (remainingAgent <= 0) {
                        System.out.println(Thread.currentThread().getName() + " has run out of agent. Returning to base.");
                        makeDroneIdleAndRecharge(lastEvent);
                        break; // Exit the loop and check for the next fire event
                    }

                    // Check for leftover agent and battery life
                    if (remainingAgent > 0 && batteryLife > 0) {
                        synchronized (scheduler) {
                            String request = (String) sendRequest("getAdditionalFireEvent", batteryLife, currentX, currentY);
                            System.out.println("Might be broken here. Request:" + request);
                            FireEvent event2 = new FireEvent(request, scheduler.getZones());
                            if (event2 == null) {
                                System.out.println("Returning to base.");
                                makeDroneIdleAndRecharge(lastEvent);
                                break;
                            }
                        }
                    } else {
                        System.out.println("Returning to base.");
                        makeDroneIdleAndRecharge(lastEvent);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}