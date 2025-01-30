import static java.lang.Thread.sleep;

public class DroneSubsystem {
    private final Scheduler scheduler;
    private final int capacity = 14;  // Max 14L per trip
    private final double cruiseSpeed = 18.0;  // 18 m/s
    private final double takeoffSpeed = 2.0;  // 2 m/s to 20m altitude
    private final int nozzleFlowRate = 2; // 2L per second

    public DroneSubsystem(Scheduler scheduler) {
        this.scheduler = scheduler;
    }



    // Method to calculate amount of water needed

    // Method to simulate drone takeoff and landing (sleep 10 seconds)


    // Method to simulate traveling to center of zone to put out fire
    // Need to calculate for middle of zone here


    // Method to extinguish fire


    //Method to return back to base?




}
