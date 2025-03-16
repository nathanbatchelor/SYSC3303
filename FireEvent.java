import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;

/**
 * This class represents a fire event with specific details such as time, zone, event type, severity,
 * and the associated subsystem that processes the event.
 */
public class FireEvent implements Serializable {
    private final String time;
    private final int zoneId;
    private final String eventType;
    private final String severity;

    private int litresNeeded;
    transient private FireIncidentSubsystem fireIncidentSubsystem;

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

    @Serial
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject(); // Deserialize non-transient fields

        // Reinitialize transient field manually (e.g., fetch from a singleton or factory)
        this.fireIncidentSubsystem = null; // Replace with actual initialization logic
    }

    /**
     * Gets the time of the fire event.
     *
     * @return The time of the event.
     */
    public String getTime() {
        return time;
    }

    /**
     * Gets the zone ID where the fire event occurred.
     *
     * @return The zone ID of the event.
     */
    public int getZoneId() {
        return zoneId;
    }

    /**
     * Gets the type of the fire event.
     *
     * @return The type of the event.
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Gets the severity of the fire event.
     *
     * @return The severity level of the event.
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Gets the zone coordinates associated with the fire event.
     *
     * @return A string representing the zone coordinates in the format "(x1, y1) to (x2, y2)".
     */
    public String getZoneDetails() {
        return fireIncidentSubsystem.getZoneCoordinates();
    }

    /**
     * Sets the amount of water (in litres) needed to handle the fire event.
     *
     * @param litres The number of litres required.
     * @return The updated number of litres required.
     */
    public int setLitres(int litres) {
        litresNeeded = litres;
        return litresNeeded;
    }

    /**
     * Removes a specified amount of water (in litres) used for the fire event.
     *
     * @param lires The number of litres to be removed.
     * @return The updated number of remaining litres required.
     */
    public int removeLitres(int lires) {
        this.litresNeeded -= lires;
        return litresNeeded;
    }

    /**
     * Gets the amount of water (in litres) still needed for the fire event.
     *
     * @return The number of remaining litres required.
     */
    public int getLitres() {
        return litresNeeded;
    }

    /**
     * Returns a string representation of the fire event.
     *
     * @return A string that includes the time, zone ID, event type, and severity.
     */
    @Override
    public String toString() {
        return "Time = " + time + ", zoneId=" + zoneId + ", EventType=" + eventType + ", Severity=" + severity;
    }
}
