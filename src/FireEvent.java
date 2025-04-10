import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Represents a fire event in the simulation.
 * Contains details such as the time of occurrence, zone ID, event type, severity,
 * required firefighting agent, fault information, and zone coordinates.
 */
public class FireEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String time;
    private final int zoneId;
    private final String eventType;
    private final String severity;
    private int litresNeeded;
    public String fault;
    private FireEventState currentState = FireEventState.INACTIVE;

    /**
     * Enumeration of possible states for a fire event.
     */
    public enum FireEventState {
        ACTIVE,
        INACTIVE,
    }

    // New field to hold zone coordinates so that they survive serialization.
    private final String zoneDetails;

    // Removed transient fireIncidentSubsystem from what we rely on for zone details.
    // (You can still keep a transient reference if needed for runtime purposes, but it’s not used in getZoneDetails().)
    private transient FireIncidentSubsystem fireIncidentSubsystem;

    /**
     * Constructs a new FireEvent.
     *
     * @param time the time at which the event occurred (in HH:mm:ss format)
     * @param zoneId the ID of the zone where the event occurred
     * @param eventType the type of fire event
     * @param severity the severity of the fire event
     * @param fault fault information related to the event, if any
     * @param fireIncidentSubsystem the FireIncidentSubsystem instance to capture zone coordinates
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
        this.currentState = FireEventState.ACTIVE;
    }

    /**
     * Constructs a new FireEvent as a copy of an existing event.
     *
     * @param other the FireEvent to copy
     */
    public FireEvent(FireEvent other) {
        this.time = other.time;
        this.zoneId = other.zoneId;
        this.eventType = other.eventType;
        this.severity = other.severity;
        this.litresNeeded = other.litresNeeded;
        this.fault = other.fault;  // You can also set this to "NONE" if you're resetting
        this.zoneDetails = other.zoneDetails;
        this.currentState = other.currentState;
        this.fireIncidentSubsystem = null; // Don't carry over transient references
    }

    /**
     * Parses the time string and returns it as a LocalTime object.
     *
     * @return the event time as a LocalTime, or LocalTime.MIDNIGHT if parsing fails
     */
    public LocalTime getTimeAsLocalTime() {
        try {
            return LocalTime.parse(this.time, DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (DateTimeParseException e) {
            System.err.println("Invalid time format in FireEvent: " + this.time);
            return LocalTime.MIDNIGHT; // fallback
        }
    }


    /**
     * Returns the current state of the fire event.
     *
     * @return the current state (ACTIVE or INACTIVE)
     */
    public FireEventState getCurrentState() {
        return currentState;
    }

    /**
     * Sets the current state of the fire event.
     *
     * @param currentState the new state of the fire event
     */
    public void setCurrentState(FireEventState currentState) {
        this.currentState = currentState;
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

    /**
     * Returns the time of the fire event.
     *
     * @return a string representing the event time in HH:mm:ss format
     */
    public String getTime() { return time; }

    /**
     * Returns the zone ID where the fire event occurred.
     *
     * @return the zone ID
     */
    public int getZoneId() { return zoneId; }

    /**
     * Returns the type of the fire event.
     *
     * @return the event type
     */
    public String getEventType() { return eventType; }

    /**
     * Returns the severity of the fire event.
     *
     * @return a string representing the severity
     */
    public String getSeverity() { return severity; }

    /**
     * Sets the required amount of firefighting agent for the event.
     *
     * @param litres the amount of agent needed in liters
     * @return the updated required amount
     */
    public int setLitres(int litres) { litresNeeded = litres; return litresNeeded; }

    /**
     * Decreases the required firefighting agent by a specified amount.
     *
     * @param litres the amount of agent to remove
     * @return the updated required amount
     */
    public int removeLitres(int litres) { this.litresNeeded -= litres; return litresNeeded; }

    /**
     * Returns the required amount of firefighting agent for the event.
     *
     * @return the amount in liters
     */
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

    /**
     * Returns the fault information associated with the fire event.
     *
     * @return a string representing the fault information
     */
    public String getFault() {
        return fault;
    }

    /**
     * Removes the fault associated with the fire event.
     */
    public void remFault() {
        fault="NONE";
    }

}
