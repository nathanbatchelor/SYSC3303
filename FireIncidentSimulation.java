public class FireIncidentSimulation {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java FireIncidentSimulation <FireIncidentFile> <ZoneFile>");
        }

        String fireIncidentFile = args[0];
        String zoneFile = args[1];

        Scheduler scheduler = new Scheduler();
        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        Thread fireIncidentSubsystem = new Thread(new FireIncidentSubsystem(scheduler, file?));
        fireIncidentSubsystem.setName("Fire Incident Subsystem");
        fireIncidentSubsystem.start();

        Thread droneSubsystem = new Thread(new DroneSubsystem(scheduler));
        droneSubsystem.setName("Drone Subsystem");
        droneSubsystem.start();
    }
}
