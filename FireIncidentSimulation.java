public class FireIncidentSimulation {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java FireIncidentSimulation <FireIncidentFile> <ZoneFile>");
        }

        String fireIncidentFile = args[0];
        String zoneFile = args[1];

        Scheduler scheduler = new Scheduler(zoneFile);
        Thread schedulerThread = new Thread(scheduler);

        schedulerThread.setName("Scheduler");
        schedulerThread.start();

        Thread fireIncidentSubsystem = new Thread(new FireIncidentSubsystem(scheduler, zoneFile));
        fireIncidentSubsystem.setName("Fire Incident Subsystem");
        fireIncidentSubsystem.start();

        Thread drone = new Thread(new Drone(scheduler));
        drone.setName("Drone Subsystem");
        drone.start();
    }
}
