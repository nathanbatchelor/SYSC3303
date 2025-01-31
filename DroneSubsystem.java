import static java.lang.Thread.sleep;

public class DroneSubsystem implements Runnable {
    private final Scheduler scheduler;
    private final int capacity = 14;  // Max 14L per trip
    private final double cruiseSpeed = 18.0;  // 18 m/s
    private final double takeoffSpeed = 2.0;  // 2 m/s to 20m altitude
    private final int nozzleFlowRate = 2; // 2L per second
    private volatile boolean isRunning;

    //ADD ID IN LATER ITERATIONS

    public DroneSubsystem(Scheduler scheduler) {
        isRunning = true;
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        System.out.println("DroneSubsystem started and is waiting for tasks");

        while (isRunning) {
            try {
                FireEvent task = scheduler.getNextFireEvent();

                if (task == null) {
                    System.out.println("DroneSubsystem got no fire event");
                    break;
                }
                System.out.println("DroneSubsystem responding to fire event: " + task);
                Thread.sleep(10000); // temp value for flight time

                scheduler.markFireExtinguished(task);
                System.out.println("Drone completed task at Zone: " + task.getZoneId());
            }catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void stop() {
        isRunning = false;
    }


    // Method to calculate amount of water needed

    // Method to simulate drone takeoff and landing (sleep 10 seconds)


    // Method to simulate traveling to center of zone to put out fire
    // Need to calculate for middle of zone here


    // Method to extinguish fire


    //Method to return back to base?


}
