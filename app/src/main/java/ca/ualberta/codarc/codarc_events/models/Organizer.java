package ca.ualberta.codarc.codarc_events.models;

/**
 * Organizer model - minimal, just deviceId and banned flag.
 * No profile info needed.
 */
public class Organizer {
    
    private String deviceId;
    private boolean banned;
    
    // Firestore needs empty constructor
    /** Empty constructor required by Firestore. */
    public Organizer() {
        this.banned = false;
    }

    /**
     * Creates a new organizer with the given device ID.
     * @param deviceId the organizer's device ID
     */
    public Organizer(String deviceId) {
        this.deviceId = deviceId;
        this.banned = false;
    }

    /** @return the organizer's device ID */
    public String getDeviceId() {
        return deviceId;
    }

    /** @param deviceId sets the organizer's device ID */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /** @return true if the organizer is banned */
    public boolean isBanned() {
        return banned;
    }

    /** @param banned sets whether the organizer is banned */
    public void setBanned(boolean banned) {
        this.banned = banned;
    }
}

