import java.io.Serializable;

/**
 * This class represents a fire event with specific details such as time, zone, event type, severity,
 * and the associated subsystem that processes the event.
 */
public class FireEvent implements Serializable {
    private static final long serialVersionUID = 1L; // Ensure version consistency

    private final String time;
    private final int zoneId;
    private final String eventType;
    private final String severity;
    private int litresNeeded;

    private transient final FireIncidentSubsystem fireIncidentSubsystem; // Prevents serialization issues

    /**
     * Constructs a FireEvent object.
     *
     * @param time                  The time of the event.
     * @param zoneId                The ID of the zone where the event occurred.
     * @param eventType             The type of fire-related event.
     * @param severity              The severity level of the event.
     * @param fireIncidentSubsystem The subsystem associated with the event.
     */
    public FireEvent(String time, int zoneId, String eventType, String severity, FireIncidentSubsystem fireIncidentSubsystem) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
        this.fireIncidentSubsystem = fireIncidentSubsystem;
    }

    public String getTime() { return time; }

    public int getZoneId() { return zoneId; }

    public String getEventType() { return eventType; }

    public String getSeverity() { return severity; }

    public int setLitres(int litres) {
        litresNeeded = litres;
        return litresNeeded;
    }

    public int removeLitres(int litres) {
        this.litresNeeded -= litres;
        return litresNeeded;
    }

    public int getLitres() { return litresNeeded; }

    /**
     * Returns zone details. If `fireIncidentSubsystem` is null (after deserialization), return "Unknown".
     */
    public String getZoneDetails() {
        return fireIncidentSubsystem != null ? fireIncidentSubsystem.getZoneCoordinates() : "Unknown";
    }

    @Override
    public String toString() {
        return "Time = " + time + ", zoneId=" + zoneId + ", EventType=" + eventType + ", Severity=" + severity;
    }
}
