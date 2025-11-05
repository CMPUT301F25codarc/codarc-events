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

    public Entrant(String deviceId, String name, long createdAtUtc) {
        this.deviceId = deviceId;
        this.name = name;
        this.createdAtUtc = createdAtUtc;
        this.email = "";
        this.phone = "";
        this.isRegistered = false;
        this.banned = false;
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
    
    @PropertyName("banned")
    public boolean isBanned() {
        return banned;
    }
    
    @PropertyName("banned")
    public void setBanned(boolean banned) {
        this.banned = banned;
    }
}