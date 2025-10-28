package ca.ualberta.codarc.codarc_events.model;

import java.util.List;

/**
 * Entrant model class representing a user who can join events.
 * 
 * Responsibilities:
 * - Join a waiting list for an event
 * - Leave a waiting list for an event
 * - Manage entrant profile and update information
 * - Accept an invite
 * - Decline an invite
 * - Receive notifications
 * - Track device ID for authentication
 * - Scan event QR codes to join or view event
 * 
 * Collaborators:
 * - WaitingList (event participation)
 * - Event (event management)
 * - ProfileRepository (profile management)
 * - Invite (invitation handling)
 * - Registration (confirmation process)
 * - Notification (communication)
 * - AuthenticationDeviceIdProvider (device identification)
 * - QRCodeService (QR scanning)
 */
public class Entrant {
    
    // User identification
    private String entrantId;
    private String name;
    private String email;
    private String deviceId;
    
    // Profile information
    private String displayName;
    private String phoneNumber;
    private String profileImageUrl;
    
    // Event participation
    private List<String> joinedEvents; // List of event IDs
    private List<String> waitingListEvents; // Events on waiting list
    
    // Notification preferences
    private boolean notificationsEnabled;
    private String pushToken;

    /**
     * Default constructor
     */
    public Entrant() {
        this.notificationsEnabled = true;
    }

    /**
     * Constructor with basic information
     */
    public Entrant(String entrantId, String name, String email, String deviceId) {
        this();
        this.entrantId = entrantId;
        this.name = name;
        this.email = email;
        this.deviceId = deviceId;
    }

    // Getters and Setters
    public String getEntrantId() {
        return entrantId;
    }

    public void setEntrantId(String entrantId) {
        this.entrantId = entrantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public List<String> getJoinedEvents() {
        return joinedEvents;
    }

    public void setJoinedEvents(List<String> joinedEvents) {
        this.joinedEvents = joinedEvents;
    }

    public List<String> getWaitingListEvents() {
        return waitingListEvents;
    }

    public void setWaitingListEvents(List<String> waitingListEvents) {
        this.waitingListEvents = waitingListEvents;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    /**
     * Joins a waiting list for an event
     */
    public void joinWaitingList(Event event) {
        // Implementation would add event to waiting list
        // This is a placeholder for the actual logic
    }

    /**
     * Leaves a waiting list for an event
     */
    public void leaveWaitingList(Event event) {
        // Implementation would remove event from waiting list
        // This is a placeholder for the actual logic
    }

    /**
     * Updates profile information
     */
    public void updateProfile(java.util.Map<String, Object> info) {
        // Implementation would update profile fields
        // This is a placeholder for the actual logic
    }

    /**
     * Accepts an invitation
     */
    public void acceptInvite(Invite invite) {
        // Implementation would handle invitation acceptance
        // This is a placeholder for the actual logic
    }

    /**
     * Declines an invitation
     */
    public void declineInvite(Invite invite) {
        // Implementation would handle invitation decline
        // This is a placeholder for the actual logic
    }
}
