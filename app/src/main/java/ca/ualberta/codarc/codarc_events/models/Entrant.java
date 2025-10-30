package ca.ualberta.codarc.codarc_events.models;

/**
 * Represents an Entrant user identified by their device ID.
 * Used for Firestore storage and local profile management.
 */
public class Entrant {

    private String deviceId;
    private String name;
    private long createdAt;

    // Firestore needs an empty constructor
    public Entrant() { }

    // Add joined event id's, invitations, etc. in later stories

    public Entrant(String deviceId, String name, long createdAt) {
        this.deviceId = deviceId;
        this.name = name;
        this.createdAt = createdAt;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
