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

    /**
     * Marks the start time of the simulation.
     */
    public void markSimulationStart() {
        simulationStartTime = System.currentTimeMillis();
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

    /**
     * Exports the collected metrics to a file.
     *
     * @param filename the name of the file to which metrics are written
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
}