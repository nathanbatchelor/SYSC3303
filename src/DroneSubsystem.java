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
    private volatile int currentX = 0;
    private volatile int currentY = 0;
    private volatile DroneState currentState;
    private DatagramSocket socket;
    private InetAddress schedulerAddress;
    private final int DEFAULT_DRONE_PORT = 5500;
    private boolean busy = false;
    private boolean hardFault = false;

    public boolean testingReturningState = false;
    public boolean arrivalFault = false;
    public boolean nozzleFault = false;
    public boolean packetlFault = false;
    public MapUI map;
    public MetricsLogger logger;


    private Timer travelTimer;
    private boolean arrivedAtFireZone = false;

    /**
     * Enumeration of possible drone states.
     */
    public enum DroneState {
        IDLE,
        ON_ROUTE,
        DROPPING_AGENT,
        RETURNING,
        FAULT
    }


    /**
     * Sends an RPC request to the scheduler.
     *
     * @param methodName the name of the method to invoke on the scheduler
     * @param parameters the parameters for the request
     * @return the response received from the scheduler, or an error message if communication fails
     */
    public Object sendRequest(String methodName, Object... parameters) {
        if (testingReturningState) return 15.0;

        try {
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
            System.out.println("Drone " + idNum + " request sent, waiting for response from: " + methodName);

            while (true) {
                try {
                    byte[] responseBuffer = new byte[4096]; // increased buffer size
                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                    socket.receive(responsePacket);

                    ObjectInputStream inputStream = new ObjectInputStream(
                            new ByteArrayInputStream(responsePacket.getData(), 0, responsePacket.getLength()));
                    Object response = inputStream.readObject();
                    if(response!=null){
                        System.out.printf("Drone %d recieved response: %s%n", idNum, response);
                    }
                    return response;
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
     * Constructs a new DroneSubsystem.
     *
     * @param scheduler the Scheduler instance for managing fire events
     * @param idNum the unique identifier for this drone
     * @param baseOffsetport an offset for the port number to avoid conflicts
     * @param map the MapUI instance for updating drone positions visually
     * @param logger the MetricsLogger instance for logging travel metrics
     */
    public DroneSubsystem(Scheduler scheduler, int idNum, int baseOffsetport, MapUI map, MetricsLogger logger) {
        try {
            socket = new DatagramSocket(DEFAULT_DRONE_PORT + idNum + baseOffsetport);
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
        this.map = map;
        this.logger = logger;
        map.updateDronePosition(idNum, currentX, currentY, DroneState.IDLE, remainingAgent, batteryLife);
    }

    public DroneState getState() {
        return currentState;
    }

    /**
     * Displays the current state of the drone by printing a message to the console.
     */
    public void displayState() {
        switch (currentState) {
            case IDLE: System.out.println("Drone " + idNum + " is currently idle.");
                break;
            case ON_ROUTE:
                System.out.println("Drone " + idNum + " is on route to fire.");
                break;
            case DROPPING_AGENT:
                System.out.println("Drone is " + idNum + "  dropping agent on fire.");
                break;
            case RETURNING:
                System.out.println("Drone " + idNum + " is returning to base.");
                break;
        }
    }

    /**
     * Simulates the drone takeoff by pausing execution to mimic reaching cruising altitude.
     */
    private void takeoff() {
        System.out.println(Thread.currentThread().getName() + " taking off to 20m altitude...");
        sleep((long) (5000 * takeoffSpeed));
        System.out.println(Thread.currentThread().getName() + " reached cruising altitude.");
    }

    /**
     * Simulates the drone's descent to the base.
     */
    private void descend() {
        System.out.println(Thread.currentThread().getName() + " descending to base...");
        sleep((long) (5000 * takeoffSpeed));
        System.out.println(Thread.currentThread().getName() + " reached ground station.");
    }

    private volatile FireEvent newEvent;
    private boolean doCheck = true;

    private Thread checkEventThread;
    private volatile boolean isCheckingForNewEvent = false;

    /**
     * Checks periodically for a new fire event while en route to the target event.
     *
     * @param currentFireEvent the current target fire event
     */
    private void checkForNewEvent(FireEvent currentFireEvent) {
        isCheckingForNewEvent = true;
        newEvent = null;
        checkEventThread = new Thread(() -> {
            while (isCheckingForNewEvent) {
                FireEvent checkEvent = (FireEvent) sendRequest("getNextAssignedEvent", Thread.currentThread().getName(), currentX, currentY);
                if (checkEvent != null && checkEvent.getZoneId() != currentFireEvent.getZoneId()) {
                    newEvent = checkEvent;
                    isCheckingForNewEvent = false;
                    break;
                }
                sleep(1000); // avoid flooding UDP
            }
        });
        checkEventThread.setDaemon(true);
        checkEventThread.start();
    }

    /**
     * Travels towards the center of the target fire zone, updating the drone's position incrementally.
     * If a new fire event is detected en route, the drone switches assignment.
     *
     * @param fullTravelTime the total travel time in seconds to the target zone
     * @param targetEvent the fire event that is the current target
     * @return the fire event to handle (either the original or a new event detected en route)
     */
    // This is broken, need to fix
    public synchronized FireEvent travelToZoneCenter(double fullTravelTime, FireEvent targetEvent) {
        // Compute the target zone center from the event.
        String[] zoneCoords = targetEvent.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");
        int destX = (Integer.parseInt(startCoords[0].trim()) + Integer.parseInt(endCoords[0].trim())) / 2;
        int destY = (Integer.parseInt(startCoords[1].trim()) + Integer.parseInt(endCoords[1].trim())) / 2;

        newEvent = null;

        int startX = currentX;
        int startY = currentY;

        int adjustment = 25; // Half of 50 (METERS_PER_CELL)
        if (destX % 2 == 1) {
            destX -= adjustment;
        }
        if (destY % 2 == 1) {
            destY -= adjustment;
        }

        // divide the travel into one-second increments.

        if (!isCheckingForNewEvent) {
            newEvent = null;
            checkForNewEvent(targetEvent);
        }

        int steps = (int) Math.ceil(fullTravelTime);
        currentState = DroneState.ON_ROUTE;
        for (int i = 1; i <= steps; i++) {
            double fraction = (double) i / steps;
            // Update position along the straight line from (startX, startY) to (destX, destY).
            synchronized (this) {
                currentX = startX + (int) ((destX - startX) * fraction);
                currentY = startY + (int) ((destY - startY) * fraction);
            }
            map.updateDronePosition(idNum, currentX, currentY, DroneState.ON_ROUTE,remainingAgent, batteryLife);

            sleep(1000);  // simulate one second of travel
            batteryLife -= 1; // decrement battery by 1 second

            if (newEvent != null && newEvent != targetEvent) {
                System.out.println(Thread.currentThread().getName() + " found on-route event at zone " + newEvent.getZoneId() +
                        " while en route to zone " + targetEvent.getZoneId() + ". Switching assignment.");
                sendRequest("ADD_FIRE_EVENT",targetEvent);
                return newEvent;
            }
        }
        // Completed travel to target zone center.
        currentX = destX;
        currentY = destY;
        map.updateDronePosition(idNum, currentX, currentY, DroneState.ON_ROUTE,remainingAgent, batteryLife);
        arrivedAtFireZone = true; // Prevent fault
        if (travelTimer != null) {
            travelTimer.cancel();
            travelTimer = null;
        }

        isCheckingForNewEvent = false;
        if (checkEventThread != null && checkEventThread.isAlive()) {
            checkEventThread.interrupt();  // optional
            checkEventThread = null;
        }
        return targetEvent;
    }

    /**
     * Stops checking for new fire events by interrupting the checking thread.
     */
    private synchronized void stopCheckingForNewEvents() {
        isCheckingForNewEvent = false;
        if (checkEventThread != null && checkEventThread.isAlive()) {
            checkEventThread.interrupt();
            checkEventThread = null;
        }
    }

    /**
     * Simulates extinguishing a fire by opening the nozzle, dropping firefighting agent,
     * and then closing the nozzle.
     *
     * @param amount the amount of firefighting agent (in liters) to drop
     */
    public synchronized void extinguishFire(int amount) {
        System.out.println("\n" + Thread.currentThread().getName() + " opening nozzle...");
        sleep(1000);
        batteryLife -= 1;
        currentState = DroneState.DROPPING_AGENT;
        map.updateDronePosition(idNum, currentX, currentY, currentState,remainingAgent, batteryLife);
        displayState();
        int timeToDrop = amount / nozzleFlowRate;
        System.out.println(Thread.currentThread().getName() + " dropping " + amount + "L of firefighting agent at " + nozzleFlowRate + "L/s.");
        sleep(timeToDrop * 1000);
        batteryLife -= timeToDrop;
        remainingAgent -= amount;
        System.out.println(Thread.currentThread().getName() + "Dispensed " + amount + "L. Remaining capacity: " + remainingAgent + "L.");
        System.out.println("\n" + Thread.currentThread().getName() + " closing nozzle...");
        sleep(1000);
        batteryLife -= 1;
        System.out.println(Thread.currentThread().getName() + " nozzle closed.\n");
    }

    /**
     * Returns the drone to its base after completing its task.
     * Updates the drone's position incrementally as it returns, then simulates landing.
     *
     * @param event the fire event that was being handled
     */
    public synchronized void returnToBase(FireEvent event) {
        currentState = DroneState.RETURNING;
        displayState();
        System.out.println("\n" + Thread.currentThread().getName() + " returning to base...\n");
        double distance = (double) sendRequest("calculateDistanceToHomeBase", event);

        int startX = currentX;
        int startY = currentY;
        int baseX = 0;
        int baseY = 0;

        logger.logDroneTravel(idNum, distance);
        logger.logZoneDistance(event.getZoneId(), distance);

        double travelTime = distance / cruiseSpeed;
        int steps = (int) Math.ceil(travelTime);
        for (int i = 1; i <= steps; i++) {
            double fraction = (double) i / steps;
            synchronized (this) {
                currentX = startX + (int) ((baseX - startX) * fraction);
                currentY = startY + (int) ((baseY - startY) * fraction);
            }
            map.updateDronePosition(idNum, currentX, currentY, currentState,remainingAgent, batteryLife);
            sleep(1000);
            batteryLife -= 1;
        }
        System.out.println();
        descend();
        System.out.println("----------------------------------------\n");
        currentX = 0;
        currentY = 0;
        map.updateDronePosition(idNum, currentX, currentY, DroneState.IDLE,remainingAgent, batteryLife);
    }

    /**
     * Makes the drone idle and recharges it after completing its task.
     *
     * @param lastEvent the last fire event that was handled
     */
    private synchronized void makeDroneIdleAndRecharge(FireEvent lastEvent) {
        stopCheckingForNewEvents();
        returnToBase(lastEvent);
        currentState = DroneState.IDLE;
        displayState();
        remainingAgent = capacity;
        batteryLife = 1800;
        map.updateDronePosition(idNum, currentX, currentY, currentState, remainingAgent, batteryLife);
    }

    /**
     * Pauses the execution for a specified duration.
     *
     * @param milliseconds the duration to sleep in milliseconds
     */
    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Starts a timer to detect if the drone fails to reach the fire zone within an expected timeframe.
     * If the drone does not arrive in time, a fault is triggered.
     *
     * @param travelTimeSeconds the expected travel time in seconds
     * @param event the fire event being targeted
     */
    private void startTravelFaultTimer(double travelTimeSeconds, FireEvent event) {
        long timeout = (long) (travelTimeSeconds * 1000 * 1.1); // 1.5x buffer
        travelTimer = new Timer();
        travelTimer.schedule(new TimerTask() {
            public void run() {
            if (!arrivedAtFireZone) {
                System.out.println("[Drone " + idNum + "] Fault detected: drone did not arrive in time.");
                sendRequest("handleDroneFault",event,"timeout",idNum);
                //currentState = DroneState.FAULT;
                map.updateDronePosition(idNum, currentX, currentY, DroneState.FAULT,remainingAgent, batteryLife);
                arrivalFault = false;
                makeDroneIdleAndRecharge(event);
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

                if ((boolean)sendRequest("STOP_?", idNum))break;

                FireEvent event = (FireEvent) sendRequest("getNextFireEvent");
                long taskStartTime = System.currentTimeMillis();
                if(event != null) {
                    logger.recordFireDispatched(event, idNum);

                }
                busy = true;
                if (event == null) {
                    busy = false;
                    continue;
                }
                // Process the received event.
                while (true) {
                    displayState();
                    if (currentX == 0 && currentY == 0) {
                        takeoff();
                    }

                    double travelTime = (double) sendRequest("calculateTravelTime", currentX, currentY, event);
                    System.out.println("[Drone " + idNum + "] Travel time: " + travelTime);

                    arrivedAtFireZone = false;
                    // HANDLE ARRIVAL FAULT
                    if (event.getFault().equalsIgnoreCase("ARRIVAL")) {
                        startTravelFaultTimer(travelTime, event); // event is the current FireEvent
                        System.out.println("\033[1;30m \033[43m[Drone " + idNum + "] ARRIVAL fault injected — drone will not move toward target.\033[0m");
                        arrivalFault = true;
                        // Drone stays put, timer will go off
                        sleep((long) (travelTime * 0.15 * 1000));  // simulate drone doing nothing
                    }

                    // HANDLE PACKET LOSS FAULT
                    if ("PACKET_LOSS".equalsIgnoreCase(event.getFault())) {
                        System.out.println("\033[1;30m \033[43m [Drone " + idNum + "] PACKET LOSS fault injected - Lost packets in communication. \033[0m");
                        sendRequest("handleDroneFault", event, "packet_loss", idNum);
                        map.updateDronePosition(idNum, currentX, currentY, DroneState.FAULT,remainingAgent, batteryLife);
                        packetlFault = true;
                        break;
                    }

                    FireEvent oldEvent = event;
                    System.out.println("I am here when I shouldnt be :) !!");
                    event = travelToZoneCenter(travelTime, event);

                    if(event != oldEvent){
                        System.out.println("Grants Debug");
                        continue;
                    }


                    int waterToDrop = Math.min(event.getLitres(), remainingAgent);

                    // HANDLE NOZZLE FAULT
                    if ("NOZZLE".equalsIgnoreCase(event.getFault())) {
                        System.out.println("\033[1;30m \033[43m [Drone " + idNum + "] NOZZLE fault injected — nozzle stuck CLOSED. \033[0m");
                        sendRequest("handleDroneFault", event, "nozzle", idNum); // or whatever your fault method is
                        map.updateDronePosition(idNum, currentX, currentY, DroneState.FAULT,remainingAgent, batteryLife);
                        hardFault = true;
                        nozzleFault = true;
                        break;
                    }

                    if(!arrivalFault) {
                        extinguishFire(waterToDrop);
                        sendRequest("updateFireStatus", event, waterToDrop);
                        logger.recordFireExtinguished(event, idNum);
                    }
                    FireEvent lastEvent = event;

                    // If the drone runs out of agent, return to base.
                    if (remainingAgent <= 0) {
                        System.out.println("Drone " + idNum + " has run out of agent. Returning to base.");
                        long taskDuration = System.currentTimeMillis() - taskStartTime;
                        logger.logDroneTaskTime(idNum, taskDuration);
                        makeDroneIdleAndRecharge(lastEvent);
                        break; // Break out of the inner loop.
                    }

                    FireEvent event2 = null;
                    // Request an additional event.
                    if(!arrivalFault) {
                        event2 = (FireEvent) sendRequest("getAdditionalFireEvent", batteryLife, currentX, currentY);
                    }
                    //FireEvent event2 = (FireEvent) sendRequest("getAdditionalFireEvent", batteryLife, currentX, currentY);
                    if (event2 == null) {
                        System.out.println("No additional event. Returning to base.");
                        long taskDuration = System.currentTimeMillis() - taskStartTime;
                        logger.logDroneTaskTime(idNum, taskDuration);
                        makeDroneIdleAndRecharge(lastEvent);
                        break; // Break out of the inner loop.
                    } else {
                        event = event2;
                    }
                }
                if(hardFault) {
                    returnToBase(event);
                    break;
                }
                // After finishing an event sequence, check again for a new event.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("DroneSubsystem " + idNum + " shutting down.");
        logger.markSimulationEnd();
        logger.exportToFile("simulation_metrics.txt");
    }
}
