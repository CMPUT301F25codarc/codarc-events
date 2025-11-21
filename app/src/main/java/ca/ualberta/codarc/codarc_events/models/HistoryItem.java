package ca.ualberta.codarc.codarc_events.models;

public class HistoryItem {

    private String eventId;
    private String eventName;
    private String status;       // waitlisted, selected, accepted, rejected, not_selected
    private String dateJoined;   // timestamp string
    private String eventDate;    // from Event.startDate or similar
    private String posterUrl;

    public HistoryItem() {}

    public HistoryItem(String eventId, String eventName, String status,
                       String dateJoined, String eventDate, String posterUrl) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.status = status;
        this.dateJoined = dateJoined;
        this.eventDate = eventDate;
        this.posterUrl = posterUrl;
    }

    public String getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public String getStatus() { return status; }
    public String getDateJoined() { return dateJoined; }
    public String getEventDate() { return eventDate; }
    public String getPosterUrl() { return posterUrl; }

    public void setStatus(String status) { this.status = status; }
}
