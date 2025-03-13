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
    private double batteryLife = 1800; // Battery Life of Drone
    private double travelTimeToFire = 0;
    private int remainingAgent; // Amount of agent remaining
    private int currentX = 0; // Drones current X position
    private int currentY = 0; // Drones current Y position
    private DroneState currentState;

    public enum DroneState {
        IDLE,
        ON_ROUTE,
        DROPPING_AGENT,
        RETURNING
    }


    /**
     * Constructs a DroneSubsystem object with the specified scheduler.
     *
     * @param scheduler the Scheduler object responsible for handling fire events.
     */
    public DroneSubsystem(Scheduler scheduler) {

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

    private double distanceFromPointToLine(int px, int py, int x1, int y1, int x2, int y2) {
        double numerator = Math.abs((y2 - y1) * px - (x2 - x1) * py + x2 * y1 - y2 * x1);
        double denominator = Math.sqrt(Math.pow(y2 - y1, 2) + Math.pow(x2 - x1, 2));
        return numerator / denominator;
    }


    private FireEvent travelToZoneCenter(double fullTravelTime, FireEvent targetEvent) {
        // Compute the target zone center from the event.
        int[] targetCenter = calculateZoneCenter(targetEvent);
        int destX = targetCenter[0];
        int destY = targetCenter[1];

        int startX = currentX;
        int startY = currentY;
        int steps = (int) Math.ceil(fullTravelTime);
        // Compute the vector from start to target
        int vX = destX - startX;
        int vY = destY - startY;
        double vLengthSquared = vX * vX + vY * vY;

        for (int i = 1; i <= steps; i++) {
            double fraction = (double) i / steps;
            currentX = startX + (int) (vX * fraction);
            currentY = startY + (int) (vY * fraction);
            sleep(1000);  // simulate one second of travel
            batteryLife -= 1; // decrement battery

            // Check for an on-route event.
            FireEvent newEvent = scheduler.getNextAssignedEvent(Thread.currentThread().getName(), currentX, currentY);
            if (newEvent != null && newEvent != targetEvent) {
                // Calculate centers for the target and new event.
                int[] newEventCenter = calculateZoneCenter(newEvent);

                // Compute vector w from start to new event center.
                int wX = newEventCenter[0] - startX;
                int wY = newEventCenter[1] - startY;
                double dot = vX * wX + vY * wY;

                // Only consider newEvent if it's between the start and target:
                if (dot >= 0 && dot <= vLengthSquared) {
                    // Compute perpendicular distance from new event center to line from start to target.
                    double perpendicularDistance = distanceFromPointToLine(newEventCenter[0], newEventCenter[1],
                            startX, startY, destX, destY);
                    double onRouteThreshold = 50.0;  // adjust if necessary
                    if (perpendicularDistance <= onRouteThreshold) {
                        System.out.println(Thread.currentThread().getName() +
                                " found on-route event at zone " + newEvent.getZoneId() +
                                " while en route to zone " + targetEvent.getZoneId() +
                                ". Switching assignment.");
                        // Re-add the original target back to the queue.
                        scheduler.addFireEvent(targetEvent);
                        return newEvent;
                    }
                }
            }
        }
        // Arrived at the target zone center.
        currentX = destX;
        currentY = destY;
        return targetEvent;
    }

    // Helper to calculate zone center from an event.
    private int[] calculateZoneCenter(FireEvent event) {
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");
        int centerX = (Integer.parseInt(startCoords[0].trim()) + Integer.parseInt(endCoords[0].trim())) / 2;
        int centerY = (Integer.parseInt(startCoords[1].trim()) + Integer.parseInt(endCoords[1].trim())) / 2;
        return new int[]{centerX, centerY};
    }


    /**
     * Simulates the drone traveling to the center of the fire zone.
     *
     * @param event the FireEvent object containing details about the fire zone.
     */
//    public double travelToZoneCenter(double travelTime, FireEvent event) {
//
//        // Extract zone coordinates from the FireEvent
//        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
//        String[] startCoords = zoneCoords[0].split(",");
//        String[] endCoords = zoneCoords[1].split(",");
//
//        // Parse the coordinates
//        int x1 = Integer.parseInt(startCoords[0].trim());
//        int y1 = Integer.parseInt(startCoords[1].trim());
//        int x2 = Integer.parseInt(endCoords[0].trim());
//        int y2 = Integer.parseInt(endCoords[1].trim());
//
//        // Calculate the center of the fire zone
//        int centerX = (x1 + x2) / 2;
//        int centerY = (y1 + y2) / 2;
//
//        currentState = DroneState.ON_ROUTE;
//        displayState();
//
//        System.out.println(Thread.currentThread().getName() + ": traveling to Zone: " + event.getZoneId() + " with fire at (" + centerX + "," + centerY + ")...");
//        sleep((long) (travelTime * 1000)); // Drone flying
//
//
//        int steps = 10;
//        double stepTime = travelTime / 10;
//        double stepX = (centerX - currentX) / (double) steps; // X increment per step
//        double stepY = (centerY - currentY) / (double) steps; // Y increment per step
//
//        for (int i = 0; i < steps; i++) {
//            currentX += stepX;
//            currentY += stepY;
//            // Notify the scheduler about drone's new position
//            //scheduler.updateDronePosition(this, currentX, currentY); // need to make this still
//            // Simulate flight with small sleep duration
//            sleep((long) (stepTime * 1000));
//        }
//
//
//        batteryLife -= travelTime;
//        System.out.println("Battery Life is now: " + batteryLife);
//        System.out.println(Thread.currentThread().getName() + ": arrived at fire center at Zone: " + event.getZoneId());
//
//        currentX = centerX;
//        currentY = centerY;
//
//        //scheduler.updateDronePosition(this, currentX, currentY); // need to make this still
//
//        return travelTime;
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
        sleep((long) ((scheduler.calculateDistanceToHomeBase(event)/18) * 1000));  // Use stored travel time //0,0 to zone 1, zone1 to zone2
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
    public synchronized void run() {
        try {
            while (true) {
                FireEvent event;

                synchronized (scheduler) {
                    event = scheduler.getNextFireEvent();
                    if (event == null) {
                        System.out.println("No event found.");
                        break;
                    }
                }
                System.out.println(Thread.currentThread().getName() + " responding to event: " + event);

                while (event != null) {
                    displayState();
                    if (currentX == 0 && currentY == 0) {
                        takeoff();
                    }
                    double travelTime = scheduler.calculateTravelTime(currentX, currentY, event);

                    event = travelToZoneCenter(travelTime, event);

                    int waterToDrop = Math.min(event.getLitres(), remainingAgent);
                    extinguishFire(waterToDrop);
                    scheduler.updateFireStatus(event, waterToDrop);
                    FireEvent lastEvent = event;

                    if (remainingAgent <= 0) {
                        System.out.println(Thread.currentThread().getName() + " has run out of agent. Returning to base.");
                        makeDroneIdleAndRecharge(lastEvent);
                        break; // Exit the loop and check for the next fire event
                    }

                    // Check for leftover agent and battery life
                    if (remainingAgent > 0 && batteryLife > 0) {
                        synchronized (scheduler) {
                            event = scheduler.getAdditionalFireEvent(batteryLife, currentX, currentY);
                            if (event == null) {
                                makeDroneIdleAndRecharge(lastEvent);
                                break;
                            }
                        }
                    } else {
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