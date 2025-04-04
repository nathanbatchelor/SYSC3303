import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class MetricsLogger {
    private long simulationStartTime;
    private long simulationEndTime;

    private final Map<Integer, Double> zoneDistances = new HashMap<>();
    private final Map<Integer, Double> droneDistances = new HashMap<>();
    private final Map<Integer, Long> droneTimes = new HashMap<>();

    public void markSimulationStart() {
        simulationStartTime = System.currentTimeMillis();
    }

    public void markSimulationEnd() {
        simulationEndTime = System.currentTimeMillis();
    }

    public void logZoneDistance(int zoneId, double distance) {
        zoneDistances.put(zoneId, distance);
    }

    public void logDroneTravel(int droneId, double distance) {
        droneDistances.merge(droneId, distance, Double::sum);
    }

    public void logDroneTaskTime(int droneId, long durationMillis) {
        droneTimes.merge(droneId, durationMillis, Long::sum);
    }

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
