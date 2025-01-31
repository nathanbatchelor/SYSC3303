public class FireEvent {
    private final String time;
    private final int zoneId;
    private final String eventType;
    private final String severity;

    private int litresNeeded;
    private final FireIncidentSubsystem fireIncidentSubsystem;

    public FireEvent(String time, int zoneId, String eventType, String severity, FireIncidentSubsystem fireIncidentSubsystem) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
        this.fireIncidentSubsystem = fireIncidentSubsystem;
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

    public String getZoneDetails() {return fireIncidentSubsystem.getZoneCoordinates();}

    public int setLitres(int litres) {
        litresNeeded = litres;
        return litresNeeded;
    }

    public int removeLitres(int lires){
        this.litresNeeded -=  lires;
        return litresNeeded;
    }

    public int getLitres() {
        return litresNeeded;
    }

    @Override
    public String toString() {
        return "Time = " + time + ", zoneId=" + zoneId + ", EventType=" + eventType + ", Severity=" + severity;
    }
}
