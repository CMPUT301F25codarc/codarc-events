package ca.ualberta.codarc.codarc_events.models;

/**
 * Organizer model - minimal, just deviceId and banned flag.
 * No profile info needed.
 */
public class Organizer {
    
    private String deviceId;
    private boolean banned;
    
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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }
}

