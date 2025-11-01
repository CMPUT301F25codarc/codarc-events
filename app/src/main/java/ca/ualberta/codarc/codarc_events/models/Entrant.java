package ca.ualberta.codarc.codarc_events.models;

import com.google.firebase.firestore.PropertyName;

/**
 * Represents an Entrant user identified by their device ID.
 * Used for Firestore storage and local profile management.
 */
public class Entrant {

    private String deviceId;
    private String name;
    private long createdAtUtc;
    private String email;
    private String phone;
    private boolean isRegistered;
    private String isOrganizer;
    private boolean isAdmin;

    /**
     * Empty constructor required by Firestore for deserialization.
     */
    public Entrant() { }

    /**
     * Constructs an Entrant with the given device id, name, and timestamp.
     * Profile fields (email, phone, flags) are initialized to defaults.
     */
    public Entrant(String deviceId, String name, long createdAtUtc) {
        this.deviceId = deviceId;
        this.name = name;
        this.createdAtUtc = createdAtUtc;
        this.email = "";
        this.phone = "";
        this.isRegistered = false;
        this.isOrganizer = "";
        this.isAdmin = false;
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

    // kept for test compatibility
    public long getCreatedAt() {
        return createdAtUtc;
    }

    public void setCreatedAt(long createdAtUtc) {
        this.createdAtUtc = createdAtUtc;
    }

    // Firestore field name is createdAtUtc
    @PropertyName("createdAtUtc")
    public long getCreatedAtUtc() {
        return createdAtUtc;
    }

    @PropertyName("createdAtUtc")
    public void setCreatedAtUtc(long createdAtUtc) {
        this.createdAtUtc = createdAtUtc;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    // Firestore uses snake_case
    @PropertyName("is_registered")
    public boolean getIsRegistered() {
        return isRegistered;
    }

    @PropertyName("is_registered")
    public void setIsRegistered(boolean isRegistered) {
        this.isRegistered = isRegistered;
    }

    @PropertyName("is_organizer")
    public String getIsOrganizer() {
        return isOrganizer;
    }

    @PropertyName("is_organizer")
    public void setIsOrganizer(String isOrganizer) {
        this.isOrganizer = isOrganizer;
    }

    @PropertyName("is_admin")
    public boolean getIsAdmin() {
        return isAdmin;
    }

    @PropertyName("is_admin")
    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
}