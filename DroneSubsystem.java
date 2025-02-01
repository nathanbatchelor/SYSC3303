/**
 * The DroneSubsystem class implements a subsystem that simulates a firefighting drone responding to fire events.
 * It calculates water needed, performs operations like takeoff, travel, extinguish fire, and returns to base.
 */
public class DroneSubsystem implements Runnable {
    private final Scheduler scheduler;
    private final int capacity = 14;  // Max 14L per trip
    private final double cruiseSpeed = 18.0;  // 18 m/s
    private final double takeoffSpeed = 2.0;  // 2 m/s to 20m altitude
    private final int nozzleFlowRate = 2; // 2L per second
    private double travelTimeToFire = 0;

    /**
     * Constructs a DroneSubsystem object with the specified scheduler.
     *
     * @param scheduler the Scheduler object responsible for handling fire events.
     */
    public DroneSubsystem(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Calculates the amount of water needed to extinguish a fire based on fire severity.
     *
     * @param severity the severity level of the fire (e.g., "low", "moderate", "high").
     * @return the amount of water needed in liters.
     */
    private int calculateWaterNeeded(String severity) {
        return switch (severity.toLowerCase()) {
            case "low" -> 10;
            case "moderate" -> 20;
            case "high" -> 30;
            default -> 0;
        };
    }

    /**
     * Simulates the drone's takeoff to a cruising altitude of 20 meters.
     * The process takes 10 seconds.
     */
    private void takeoff() {
        System.out.println(Thread.currentThread().getName() + " taking off to 20m altitude...");
        sleep(10000);
        System.out.println(Thread.currentThread().getName() + " reached cruising altitude.");
    }

    /**
     * Simulates the drone's landing at the base station.
     * The process takes 10 seconds.
     */
    private void descend() {
        System.out.println(Thread.currentThread().getName() + " descend to 20m altitude...");
        sleep(10000);
        System.out.println(Thread.currentThread().getName() + " reached ground station.");
    }

    /**
     * Simulates the drone traveling to the center of the fire zone.
     *
     * @param event the FireEvent object containing details about the fire zone.
     */
    private void travelToZoneCenter(FireEvent event) {
        String[] zoneCoords = event.getZoneDetails().replaceAll("[()]", "").split(" to ");
        String[] startCoords = zoneCoords[0].split(",");
        String[] endCoords = zoneCoords[1].split(",");

        int x1 = Integer.parseInt(startCoords[0].trim());
        int y1 = Integer.parseInt(startCoords[1].trim());
        int x2 = Integer.parseInt(endCoords[0].trim());
        int y2 = Integer.parseInt(endCoords[1].trim());

        int centerX = (x1 + x2) / 2;
        int centerY = (y1 + y2) / 2;

        double distance = Math.sqrt(Math.pow(centerX - x1, 2) + Math.pow(centerY - y1, 2)); // Euclidean distance
        double travelTime = distance / cruiseSpeed;
        travelTimeToFire = travelTime;

        System.out.println(Thread.currentThread().getName() + ": traveling to Zone: " + event.getZoneId() + " with fire at (" + centerX + "," + centerY + ")...");
        sleep((long) (travelTime * 1000));
        System.out.println(Thread.currentThread().getName() + ": arrived at fire center at Zone: " + event.getZoneId());
    }

    /**
     * Simulates the process of extinguishing a fire by dropping water.
     *
     * @param amount the amount of water to drop in liters.
     */
    private void extinguishFire(int amount) {
        System.out.println(Thread.currentThread().getName() + " opening nozzle...");
        sleep(1000); // Takes 1 second to open the nozzle

        int timeToDrop = amount / nozzleFlowRate; // Time in seconds to drop water
        System.out.println(Thread.currentThread().getName() + " dropping " + amount + "L of firefighting agent at " + nozzleFlowRate + "L/s.");
        sleep(timeToDrop * 1000);  // Time to drop all water

        int remainingCapacity = capacity - amount;
        System.out.println("Dispensed " + amount + "L. Remaining capacity: " + remainingCapacity + "L.");
        System.out.println(Thread.currentThread().getName() + " nozzle closed.");
    }

    /**
     * Simulates the drone's return to base and its landing process.
     * The time to base is calculated during travel, and landing takes 10 seconds.
     */
    private void returnToBase() {
        System.out.println(Thread.currentThread().getName() + " returning to base...");
        sleep((long) (travelTimeToFire * 1000));  // Use stored travel time
        descend();
    }


    /**
     * Helper method to pause the execution of a thread for a specified amount of time.
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

                int totalWaterNeeded = calculateWaterNeeded(event.getSeverity());
                event.setLitres(totalWaterNeeded);

                while (event.getLitres() > 0) {
                    takeoff();
                    travelToZoneCenter(event);
                    int waterToDrop = Math.min(event.getLitres(), capacity);
                    extinguishFire(waterToDrop);
                    scheduler.editFireEvent(event, waterToDrop);
                    returnToBase();
                }
                scheduler.markFireExtinguished(event);
                System.out.println("Fire Extinguished");
            }
        } catch (Exception e) {}
    }
}
