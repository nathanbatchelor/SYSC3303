import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * MetricsLogger collects and logs various metrics during the simulation,
 * including simulation duration, distances traveled by zones and drones,
 * and task execution times for drones.
 * It provides functionality to export these metrics to a file or print a summary.
 */
public class MetricsLogger {
    private long simulationStartTime;
    private long simulationEndTime;

    private final Map<Integer, Double> zoneDistances = new HashMap<>();
    private final Map<Integer, Double> droneDistances = new HashMap<>();
    private final Map<Integer, Long> droneTimes = new HashMap<>();
    private final Map<String, FireEventMetrics> eventMetrics = new HashMap<>();

    /**
     * Marks the start time of the simulation.
     */
    public void markSimulationStart() {
        simulationStartTime = System.currentTimeMillis();
    }

    public void recordFireDetected(FireEvent event) {
        FireEventMetrics m = new FireEventMetrics();
        m.zoneId = event.getZoneId();
        m.litresNeeded = event.getLitres();

        // Parse event.getTime() as LocalTime to get a fixed logical time
        m.detectedTime = parseTimeToMillis(event.getTime());  // NEW LINE

        eventMetrics.put(event.getTime() + "_" + event.getZoneId(), m);
    }

    public void recordFireExtinguished(FireEvent event) {
        String key = event.getTime() + "_" + event.getZoneId();
        FireEventMetrics m = eventMetrics.get(key);
        if (m != null) {
            m.extinguishedTime = System.currentTimeMillis();
        }
    }

    private long parseTimeToMillis(String timeString) {
        try {
            // Converts "13:03:15" to milliseconds since start of day
            java.time.LocalTime t = java.time.LocalTime.parse(timeString);
            return t.toSecondOfDay() * 1000L;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String formatTime(long millis) {
        return new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date(millis));
    }



    public void recordFireDispatched(FireEvent event, int droneId) {
        String key = event.getTime() + "_" + event.getZoneId();
        FireEventMetrics m = eventMetrics.get(key);
        if (m != null) {
            m.dispatchedTime = System.currentTimeMillis();
            m.droneId = droneId;
        }
    }

    /**
     * Marks the end time of the simulation.
     */
    public void markSimulationEnd() {
        simulationEndTime = System.currentTimeMillis();
    }

    /**
     * Logs the distance covered in a specific zone.
     *
     * @param zoneId   the identifier of the zone
     * @param distance the distance (in meters) covered in the zone
     */
    public void logZoneDistance(int zoneId, double distance) {
        zoneDistances.put(zoneId, distance);
    }

    /**
     * Logs the travel distance for a specific drone.
     *
     * @param droneId  the identifier of the drone
     * @param distance the distance (in meters) traveled by the drone
     */
    public void logDroneTravel(int droneId, double distance) {
        droneDistances.merge(droneId, distance, Double::sum);
    }

    /**
     * Logs the task execution time for a specific drone.
     *
     * @param droneId       the identifier of the drone
     * @param durationMillis the duration (in milliseconds) of the task
     */
    public void logDroneTaskTime(int droneId, long durationMillis) {
        droneTimes.merge(droneId, durationMillis, Long::sum);
    }


    private final List<String> faults = new ArrayList<>();
    public void logFault(int droneId, String type, FireEvent event) {
        faults.add("Drone " + droneId + " fault [" + type + "] on fire at Zone " + event.getZoneId());
    }

    /**
     * Exports the collected metrics to a file.
     *
     * @param filename the name of the file to which metrics are written to.
     */
    public void exportToFile(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("=== METRICS SUMMARY ===\n");
            writer.write("Simulation Duration: " + (simulationEndTime - simulationStartTime) + " ms\n");

            writer.write("\n--- Zone Distances ---\n");
            for (var entry : zoneDistances.entrySet()) {
                writer.write("Zone " + entry.getKey() + ": " + entry.getValue() + " meters\n");
            }

            writer.write("\n--- Drone Distances ---\n");
            for (var entry : droneDistances.entrySet()) {
                writer.write("Drone " + entry.getKey() + ": " + entry.getValue() + " meters\n");
            }

            writer.write("\n--- Drone Times ---\n");
            for (var entry : droneTimes.entrySet()) {
                writer.write("Drone " + entry.getKey() + ": " + entry.getValue() + " ms\n");
            }

            writer.write("\n--- Fire Event Metrics ---\n");
            for (var entry : eventMetrics.entrySet()) {
                FireEventMetrics m = entry.getValue();
                writer.write("Event [" + entry.getKey() + "]\n");
                writer.write("  Zone: " + m.zoneId + "\n");
                writer.write("  Drone: " + m.droneId + "\n");
                writer.write("  Litres Needed: " + m.litresNeeded + "\n");
                writer.write("  Detected: " + formatTime(m.detectedTime) + "\n");
                writer.write("  Dispatched: " + formatTime(m.dispatchedTime) + " (+" + (m.dispatchedTime - m.detectedTime) + " ms)\n");
                writer.write("  Extinguished: " + formatTime(m.extinguishedTime) + " (+" + (m.extinguishedTime - m.dispatchedTime) + " ms from dispatch)\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints a summary of the collected metrics to the console.
     */
    public void printSummary() {
        System.out.println("=== METRICS SUMMARY ===");
        System.out.println("Simulation Duration: " + (simulationEndTime - simulationStartTime) + " ms");

        System.out.println("\n--- Zone Distances ---");
        zoneDistances.forEach((id, dist) -> System.out.println("Zone " + id + ": " + dist + " meters"));

        System.out.println("\n--- Drone Distances ---");
        droneDistances.forEach((id, dist) -> System.out.println("Drone " + id + ": " + dist + " meters"));

        System.out.println("\n--- Drone Times ---");
        droneTimes.forEach((id, time) -> System.out.println("Drone " + id + ": " + time + " ms"));
    }


    private static class FireEventMetrics {
        long detectedTime;
        long dispatchedTime;
        long extinguishedTime;
        int litresNeeded;
        int zoneId;
        int droneId;
    }

}