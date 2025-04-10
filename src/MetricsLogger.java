import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.*;

public class MetricsLogger {
    private long simulationStartTime;
    private long simulationEndTime;

    private final Map<Integer, Double> zoneDistances = new HashMap<>();
    private final Map<Integer, Double> droneDistances = new HashMap<>();
    private final Map<Integer, Long> droneTimes = new HashMap<>();
    private final Map<String, List<FireEventMetrics>> eventMetrics = new HashMap<>();

    public void markSimulationStart() {
        simulationStartTime = System.currentTimeMillis();
    }

    public void markSimulationEnd() {
        simulationEndTime = System.currentTimeMillis();
    }

    public void recordFireDetected(FireEvent event) {
        String key = event.getTime() + "_" + event.getZoneId();
        FireEventMetrics m = new FireEventMetrics();
        m.detectedSimTimeMs = parseSimulatedTime(event);
        m.detectedWallTime = System.currentTimeMillis();
        m.zoneId = event.getZoneId();
        m.litresNeeded = event.getLitres();
        eventMetrics.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
    }

    public void recordFireDispatched(FireEvent event, int droneId) {
        String key = event.getTime() + "_" + event.getZoneId();
        List<FireEventMetrics> list = eventMetrics.get(key);
        if (list != null && !list.isEmpty()) {
            FireEventMetrics m = new FireEventMetrics();
            m.zoneId = event.getZoneId();
            m.litresNeeded = event.getLitres();
            m.droneId = droneId;
            m.detectedSimTimeMs = parseSimulatedTime(event);
            m.detectedWallTime = list.get(0).detectedWallTime; // from first detection
            m.dispatchedOffset = System.currentTimeMillis() - m.detectedWallTime;
            list.add(m);
        }
    }

    public void recordFireExtinguished(FireEvent event, int droneId) {
        String key = event.getTime() + "_" + event.getZoneId();
        List<FireEventMetrics> list = eventMetrics.get(key);
        if (list != null) {
            for (FireEventMetrics m : list) {
                if (m.droneId == droneId && m.extinguishedOffset == 0) {
                    m.extinguishedOffset = System.currentTimeMillis() - m.detectedWallTime;
                    break;
                }
            }
        }
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

    private long parseSimulatedTime(FireEvent event) {
        LocalTime baseTime = LocalTime.parse(event.getTime());
        return baseTime.toSecondOfDay() * 1000L;
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

            writer.write("\n--- Fire Event Metrics ---\n");
            for (var entry : eventMetrics.entrySet()) {
                for (FireEventMetrics m : entry.getValue()) {
                    String simTimeStr = String.format("%02d:%02d:%02d",
                            (m.detectedSimTimeMs / 3600000) % 24,
                            (m.detectedSimTimeMs / 60000) % 60,
                            (m.detectedSimTimeMs / 1000) % 60);

                    writer.write("Event [" + entry.getKey() + "]\n");
                    writer.write("  Zone: " + m.zoneId + "\n");
                    writer.write("  Drone: " + m.droneId + "\n");
                    writer.write("  Litres Needed: " + m.litresNeeded + "\n");
                    writer.write("  Detected: " + simTimeStr + "\n");
                    writer.write("  Dispatched: +" + m.dispatchedOffset + " ms\n");
                    writer.write("  Extinguished: +" + m.extinguishedOffset + " ms from detection\n\n");
                }
            }

            writer.write("\n--- Aggregate Drone Metrics ---\n");
            Map<Integer, List<FireEventMetrics>> byDrone = new HashMap<>();
            for (List<FireEventMetrics> metricsList : eventMetrics.values()) {
                for (FireEventMetrics m : metricsList) {
                    byDrone.computeIfAbsent(m.droneId, k -> new ArrayList<>()).add(m);
                }
            }

            for (var entry : byDrone.entrySet()) {
                int droneId = entry.getKey();
                List<FireEventMetrics> list = entry.getValue();
                long totalResponse = 0, totalExtinguish = 0, totalTotal = 0;
                for (FireEventMetrics m : list) {
                    totalResponse += m.dispatchedOffset;
                    totalExtinguish += (m.extinguishedOffset - m.dispatchedOffset);
                    totalTotal += m.extinguishedOffset;
                }
                int count = list.size();
                writer.write("Drone " + droneId + " handled " + count + " events\n");
                writer.write("  Avg Response Time (Detection ➔ Dispatch): " + (totalResponse / count) + " ms\n");
                writer.write("  Avg Extinguish Time (Dispatch ➔ Extinguish): " + (totalExtinguish / count) + " ms\n");
                writer.write("  Avg Total Time (Detection ➔ Extinguish): " + (totalTotal / count) + " ms\n\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class FireEventMetrics {
        long detectedSimTimeMs;
        long detectedWallTime;
        long dispatchedOffset;
        long extinguishedOffset;
        int litresNeeded;
        int zoneId;
        int droneId;
    }
}