package ca.ualberta.codarc.codarc_events.models;

import com.google.firebase.firestore.PropertyName;

/**
 * User model - base identity with role flags.
 * All flags start as false, get set when user does actions.
 * Banned status is in Entrants/Organizers, not here.
 */
public class User {
    
    private String deviceId;
    private boolean isEntrant;
    private boolean isOrganizer;
    private boolean isAdmin;
    
    public User() {
        this.isEntrant = false;
        this.isOrganizer = false;
        this.isAdmin = false;
    }

    /**
     * Creates a user with the given device ID.
     * @param deviceId the user's device ID
     */
    public User(String deviceId) {
        this.deviceId = deviceId;
        this.isEntrant = false;
        this.isOrganizer = false;
        this.isAdmin = false;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @PropertyName("isEntrant")
    public boolean isEntrant() {
        return isEntrant;
    }

    @PropertyName("isEntrant")
    public void setEntrant(boolean entrant) {
        isEntrant = entrant;
    }

    @PropertyName("isOrganizer")
    public boolean isOrganizer() {
        return isOrganizer;
    }

    @PropertyName("isOrganizer")
    public void setOrganizer(boolean organizer) {
        isOrganizer = organizer;
    }

    @PropertyName("isAdmin")
    public boolean isAdmin() {
        return isAdmin;
    }

    @PropertyName("isAdmin")
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}

