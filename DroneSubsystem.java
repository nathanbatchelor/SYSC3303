import static java.lang.Thread.sleep;

public class DroneSubsystem implements Runnable {
    private final Scheduler scheduler;
    private final int capacity = 14;  // Max 14L per trip
    private final double cruiseSpeed = 18.0;  // 18 m/s
    private final double takeoffSpeed = 2.0;  // 2 m/s to 20m altitude
    private final int nozzleFlowRate = 2; // 2L per second\
    private double travelTimeToFire = 0;

    public DroneSubsystem(Scheduler scheduler) {
        this.scheduler = scheduler;
    }


    // Method to calculate amount of water needed
    private int calculateWaterNeeded(String severity){
        return switch (severity.toLowerCase()){
            case "low" -> 10;
            case "moderate" -> 20;
            case "high" -> 30;
            default -> 0;
        };
    }

    // Method to simulate drone takeoff and landing (sleep 10 seconds)
    private void takeoff() {
        System.out.println(Thread.currentThread().getName() + " taking off to 20m altitude...");
        sleep(10000);  // Takes 10s to reach altitude at 2m/s
        System.out.println(Thread.currentThread().getName() + " reached cruising altitude.");
    }


    // Method to simulate traveling to center of zone to put out fire
    // Need to calculate for middle of zone here
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

        System.out.println(Thread.currentThread().getName() + ": traveling to Zone: " + event.getZoneId() + " with fire at (" + centerX + "," + centerY + ")...");
        sleep((long) (travelTime * 1000));
        System.out.println(Thread.currentThread().getName() + ": arrived at fire center at Zone: " + event.getZoneId());
    }


    // Method to extinguish fire
    private void extinguishFire(int amount) {
        System.out.println(Thread.currentThread().getName() + " opening nozzle...");
        sleep(1000); // Takes 1 second to open the nozzle

        int timeToDrop = amount / nozzleFlowRate; // **Time in seconds to drop water**
        System.out.println(Thread.currentThread().getName() + " dropping " + amount + "L of firefighting agent at " + nozzleFlowRate + "L/s.");
        sleep(timeToDrop * 1000);  // Time to drop all water

        System.out.println(Thread.currentThread().getName() + " nozzle closed.");
    }


    //Method to return back to base?
    private void returnToBase() {
        System.out.println(Thread.currentThread().getName() + " returning to base...");
        sleep((long) (travelTimeToFire * 1000));  // Use stored travel time

        // Simulate landing
        System.out.println(Thread.currentThread().getName() + " descending to ground...");
        sleep(10000);  // Takes 10s to descend at 2m/s
        System.out.println(Thread.currentThread().getName() + " landed safely.");
    }


    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void run() {
        while(true) {
            System.out.println("Inside the drone RUN");
            FireEvent event = scheduler.getNextFireEvent();

            if (event != null) {
                System.out.println(Thread.currentThread().getName() + " responding to event: " + event);
            } else {
                break;
            }
            // Determine water needed

            int totalWaterNeeded = calculateWaterNeeded(event.getSeverity());
            event.setLitres(totalWaterNeeded);
            while(event.getLitres() > 0){
                takeoff();
                travelToZoneCenter(event);
                extinguishFire(Math.min(totalWaterNeeded, capacity));
                scheduler.editFireEvent(event, 10);
                returnToBase();
            }

            scheduler.markFireExtinguished(event);
            System.out.println("Fire Extinguished");



        }
        System.out.println(Thread.currentThread().getName() + " Drone thread has stopped");
    }
}
