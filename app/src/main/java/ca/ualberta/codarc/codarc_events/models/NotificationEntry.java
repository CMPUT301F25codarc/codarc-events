package ca.ualberta.codarc.codarc_events.models;

import java.io.Serializable;

/**
 * Represents a single notification sent to an entrant.
 *
 * <p>The entry mirrors the structure stored under
 * {@code entrants/<deviceId>/notifications} in Firestore and carries
 * a couple of transient flags used by the UI layer (such as the
 * {@code processing} flag).</p>
 */
public class NotificationEntry implements Serializable {

    private String id;
    private String eventId;
    private String eventName;
    private String message;
    private String category;
    private long createdAt;
    private boolean read;
    private String response;
    private long respondedAt;

    // Transient state used by RecyclerView rows while an action is pending.
    private transient boolean processing;

    /** @return the notification ID */
    public String getId() {
        return id;
    }

    /** @param id sets the notification ID */
    public void setId(String id) {
        this.id = id;
    }

    /** @return the related event ID */
    public String getEventId() {
        return eventId;
    }

    /** @param eventId sets the related event ID */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /** @return the name of the related event */
    public String getEventName() {
        return eventName;
    }

    /** @param eventName sets the event name */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /** @return the message content */
    public String getMessage() {
        return message;
    }

    /** @param message sets the message content */
    public void setMessage(String message) {
        this.message = message;
    }

    /** @return the notification category */
    public String getCategory() {
        return category;
    }

    /** @param category sets the notification category */
    public void setCategory(String category) {
        this.category = category;
    }

    /** @return the creation timestamp */
    public long getCreatedAt() {
        return createdAt;
    }

    /** @param createdAt sets the creation timestamp */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /** @return true if the notification has been read */
    public boolean isRead() {
        return read;
    }

    /** @param read sets whether the notification has been read */
    public void setRead(boolean read) {
        this.read = read;
    }

    /** @return the entrant's response, if any */
    public String getResponse() {
        return response;
    }

    /** @param response sets the entrant's response */
    public void setResponse(String response) {
        this.response = response;
    }

    /** @return when the entrant responded */
    public long getRespondedAt() {
        return respondedAt;
    }

    /** @param respondedAt sets when the entrant responded */
    public void setRespondedAt(long respondedAt) {
        this.respondedAt = respondedAt;
    }

    /** @return true if the notification is being processed in the UI */
    public boolean isProcessing() {
        return processing;
    }

    /** @param processing sets whether the notification is being processed */
    public void setProcessing(boolean processing) {
        this.processing = processing;
    }
}