import java.io.Serializable;
import java.util.Map;

public class FireEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String time;
    private final int zoneId;
    private final String eventType;
    private final String severity;
    private int litresNeeded;
    public String fault;

    // New field to hold zone coordinates so that they survive serialization.
    private final String zoneDetails;

    // Removed transient fireIncidentSubsystem from what we rely on for zone details.
    // (You can still keep a transient reference if needed for runtime purposes, but it’s not used in getZoneDetails().)
    private transient FireIncidentSubsystem fireIncidentSubsystem;

    /**
     * Constructs a FireEvent and captures the zone details from the provided subsystem.
     */
    public FireEvent(String time, int zoneId, String eventType, String severity, String fault, FireIncidentSubsystem fireIncidentSubsystem) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
        this.fireIncidentSubsystem = fireIncidentSubsystem;
        this.litresNeeded = 0;  // Default value, will be set by Scheduler
        this.fault = fault;
        // Capture the zone coordinates for later use.
        this.zoneDetails = fireIncidentSubsystem != null ? fireIncidentSubsystem.getZoneCoordinates() : "Unknown";
    }

    // This constructor is retained for backward compatibility if needed.
//    public FireEvent(String params, Map<Integer, FireIncidentSubsystem> zones) {
//        String[] elements = params.split(",");
//        if (elements.length < 4) {
//            throw new IllegalArgumentException("Invalid FireEvent format: " + params);
//        }
//        this.time = elements[0].substring(7);
//        this.zoneId = Integer.parseInt(elements[1].substring(8));
//        this.eventType = elements[2].substring(11);
//        this.severity = elements[3].substring(10);
//        this.litresNeeded = Integer.parseInt(elements[4].substring(14));
//        this.fireIncidentSubsystem = zones.getOrDefault(this.zoneId, null);
//        this.zoneDetails = fireIncidentSubsystem != null ? fireIncidentSubsystem.getZoneCoordinates() : "Unknown";
//    }

    public String getTime() { return time; }
    public int getZoneId() { return zoneId; }
    public String getEventType() { return eventType; }
    public String getSeverity() { return severity; }
    public int setLitres(int litres) { litresNeeded = litres; return litresNeeded; }
    public int removeLitres(int litres) { this.litresNeeded -= litres; return litresNeeded; }
    public int getLitres() { return litresNeeded; }

    /**
     * Returns the zone details that were captured during construction.
     */
    public String getZoneDetails() {
        return zoneDetails;
    }

    @Override
    public String toString() {
        return "Time = " + time + ", zoneId=" + zoneId + ", EventType=" + eventType +
                ", Severity=" + severity + ", LitresNeeded=" + litresNeeded;
    }

    public String getFault() {
        return fault;
    }
}
