public class FireEvent {
    private final String time;
    private final int zoneId;
    private final String eventType;
    private final String severity;

    public FireEvent(String time, int zoneId, String eventType, String severity) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
    }

    public String getTime() {
        return time;
    }

    public int getZoneId() {
        return zoneId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSeverity() {
        return severity;
    }

    @Override
    public String toString() {
        return "Time = " + time + ", zoneId=" + zoneId + ", EventType=" + eventType + ", Severity=" + severity;
    }
}
