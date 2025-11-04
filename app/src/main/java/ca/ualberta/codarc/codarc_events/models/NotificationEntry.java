package ca.ualberta.codarc.codarc_events.models;

import java.io.Serializable;

/**
 * Represents a single notification sent to an entrant.
 *
 * <p>The entry mirrors the structure stored under
 * {@code profiles/<deviceId>/notifications} in Firestore and carries
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public long getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(long respondedAt) {
        this.respondedAt = respondedAt;
    }

    public boolean isProcessing() {
        return processing;
    }

    public void setProcessing(boolean processing) {
        this.processing = processing;
    }
}