package ca.ualberta.codarc.codarc_events.models;

import com.google.firebase.firestore.PropertyName;

/**
 * Entrant model - profile info for users who join waitlists.
 * Created when user joins first waitlist and provides info.
 */
public class Entrant {

    private String deviceId;
    private String name;
    private long createdAtUtc;
    private String email;
    private String phone;
    private boolean isRegistered;
    private boolean banned;

    // Firestore needs empty constructor
    public Entrant() { }

    /**
     * Creates a new Entrant with the specified device ID, name, and creation time.
     *
     * @param deviceId     unique ID for the device
     * @param name         entrant's name
     * @param createdAtUtc time of creation in UTC milliseconds
     */
    public Entrant(String deviceId, String name, long createdAtUtc) {
        this.deviceId = deviceId;
        this.name = name;
        this.createdAtUtc = createdAtUtc;
        this.email = "";
        this.phone = "";
        this.isRegistered = false;
        this.banned = false;
    }

    /** @return the unique device ID for this entrant */
    public String getDeviceId() {
        return deviceId;
    }

    /** @param deviceId the unique device ID to set */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /** @return the entrant's name */
    public String getName() {
        return name;
    }

    /** @param name the entrant's name to set */
    public void setName(String name) {
        this.name = name;
    }

    // kept for test compatibility
    public long getCreatedAt() {
        return createdAtUtc;
    }

    /** @param createdAtUtc the UTC creation timestamp */
    public void setCreatedAt(long createdAtUtc) {
        this.createdAtUtc = createdAtUtc;
    }

    // Firestore field name is createdAtUtc
    /**
     * @return the UTC creation timestamp of this entrant
     */
    @PropertyName("createdAtUtc")
    public long getCreatedAtUtc() {
        return createdAtUtc;
    }

    /**
     * Sets the UTC creation timestamp of this entrant.
     *
     * @param createdAtUtc UTC timestamp to set
     */
    @PropertyName("createdAtUtc")
    public void setCreatedAtUtc(long createdAtUtc) {
        this.createdAtUtc = createdAtUtc;
    }

    /** @return the entrant's email address */
    public String getEmail() {
        return email;
    }

    /** @param email the email address to set */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return the entrant's phone number */
    public String getPhone() {
        return phone;
    }

    /** @param phone the phone number to set */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    // Firestore uses snake_case
    /** @return true if the entrant has registered an account */
    @PropertyName("is_registered")
    public boolean getIsRegistered() {
        return isRegistered;
    }

    /**
     * Sets whether the entrant has registered an account.
     *
     * @param isRegistered true if the entrant is registered
     */
    @PropertyName("is_registered")
    public void setIsRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }


    /** @return true if the entrant is banned */
    @PropertyName("banned")
    public boolean isBanned() {
        return banned;
    }


    /**
     * Sets whether the entrant is banned.
     *
     * @param banned true if the entrant is banned
     */
    @PropertyName("banned")
    public void setBanned(boolean banned) {
        this.banned = banned;
    }
}