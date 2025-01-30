import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * The Scheduler class acts as a central system for handling fire events.
 * It manages incoming fire events and assigns tasks to drones
 *
 * @author Joey Andrwes
 * @author Grant Phillips
 *
 * @version 1.0
 */

public class Scheduler implements Runnable {

    // Queue to hold FireEvents
    private final Queue<FireEvent> queue = new LinkedList<>();
    // Something to store zones
    private final Map<Integer, FireIncidentSubsystem> zones = new HashMap<>();
    // File for zones
    private final String zoneFile;

    private ArrayList<Drone> idleDrones;


    private volatile boolean isFinished = false;

    public Scheduler (String zoneFile) {
       // Future location of drone & FIS objects
        this.zoneFile = zoneFile;
        readZoneFile(zoneFile);
        idleDrones = new ArrayList<Drone>();
    }

    // Add fire events to queue
    // Would FIS call: scheduler.addFireEvent(event);
    public synchronized void addFireEvent(FireEvent event) {
        queue.add(event);
        notifyAll();
    }

    /**
     *
     * @return The first FireEvent in the queue
     */
    // Would DS call: scheduler.getNextFireEvent();
    public synchronized FireEvent getNextFireEvent() {
        // get FireEvents from queue
        while (queue.isEmpty() && !isFinished) {
            try {
                wait();
            } catch (InterruptedException e) {}

        }
        // return first event. If fire isn't put out, send another drone.
        // Call FIS to see if the fire is out. If so, delete event from queue
        return queue.peek();
    }

    // The FIS could call this when fire is extinguished?
    // FIS has a count, counts number of litres dropped by drone?
    // When count == int severity return true?
    public synchronized void markFireExtinguished(FireEvent event) {
        queue.remove(event);
        System.out.println("Fire at Zone: " + event.getZoneId() + " Extinguished");
    }

    // Signal when all fires from input file are finished?
    public synchronized void finish() {
        isFinished = true;
        notifyAll();
    }

    public void readZoneFile(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            line=br.readLine(); // removes header from br before while loop
            while ((line = br.readLine()) != null) {
                System.out.println("creating zone");

                /*String[] tokens = line.split(",");
                if (tokens.length != 5) {
                    System.out.println("Invalid Line: " + line);
                    continue;
                }

                int zoneId = Integer.parseInt(tokens[0].trim());
                int x1 = Integer.parseInt(tokens[1].trim());
                int y1 = Integer.parseInt(tokens[2].trim());
                int x2 = Integer.parseInt(tokens[3].trim());
                int y2 = Integer.parseInt(tokens[4].trim());

                StringBuilder zoneCoordinates = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    zoneCoordinates.append(tokens[i]);
                }*/

                FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(this, line);
                //zones.put(zoneId, fireIncidentSubsystem);
                Thread thread = new Thread(fireIncidentSubsystem);
                thread.setName("Fire Incident Subsystem Zone: ");
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Error reading file: " + filename);
        }
    }

    // Called when thread is finished running
    public synchronized boolean isFinished() {
        return isFinished;
    }

    // Not utilised in iteration #1
    @Override
    public void run() {
        while (!isFinished) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public void droneRequestWork(Drone drone){
        idleDrones.add(drone);
    }
    public void droneSendWork(){
        Drone workingDrone = idleDrones.removeFirst();
        workingDrone.setJob(queue.poll());
    }
}
