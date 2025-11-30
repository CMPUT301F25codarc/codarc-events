package ca.ualberta.codarc.codarc_events.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents a single entry in an entrant's registration history.
 * Contains event information and selection status.
 */
public class RegistrationHistoryEntry {

    private String eventId;
    private String eventName;
    private String eventDate;
    private String selectionStatus;

    public RegistrationHistoryEntry() {
    }

    /**
     * Creates a new registration history entry.
     *
     * @param eventId         the ID of the event
     * @param eventName       the name of the event
     * @param eventDate       the event date in ISO format
     * @param selectionStatus the selection status
     */
    public RegistrationHistoryEntry(String eventId, String eventName, String eventDate, String selectionStatus) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.selectionStatus = selectionStatus;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventDate() {
        return eventDate;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public String getSelectionStatus() {
        return selectionStatus;
    }

    public void setSelectionStatus(String selectionStatus) {
        this.selectionStatus = selectionStatus;
    }

    /**
     * Checks if the event date is in the past.
     *
     * @return true if the event date has passed, false otherwise
     */
    public boolean isPastEvent() {
        if (eventDate == null || eventDate.isEmpty()) {
            return false;
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date eventDateTime = format.parse(eventDate);
            return eventDateTime != null && eventDateTime.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}


