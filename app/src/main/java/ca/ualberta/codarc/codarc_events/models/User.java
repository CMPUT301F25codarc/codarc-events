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
    
    // Firestore needs empty constructor
    /** Empty constructor required by Firestore. */
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

    /** @return the user's device ID */
    public String getDeviceId() {
        return deviceId;
    }

    /** @param deviceId sets the user's device ID */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /** @return true if the user is an entrant */
    @PropertyName("isEntrant")
    public boolean isEntrant() {
        return isEntrant;
    }

    /** @param entrant sets whether the user is an entrant */
    @PropertyName("isEntrant")
    public void setEntrant(boolean entrant) {
        isEntrant = entrant;
    }

    /** @return true if the user is an organizer */
    @PropertyName("isOrganizer")
    public boolean isOrganizer() {
        return isOrganizer;
    }


    /** @param organizer sets whether the user is an organizer */
    @PropertyName("isOrganizer")
    public void setOrganizer(boolean organizer) {
        isOrganizer = organizer;
    }

    /** @return true if the user is an admin */
    @PropertyName("isAdmin")
    public boolean isAdmin() {
        return isAdmin;
    }

    /** @param admin sets whether the user is an admin */
    @PropertyName("isAdmin")
    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}

