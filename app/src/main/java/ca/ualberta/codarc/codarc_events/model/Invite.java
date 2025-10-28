package ca.ualberta.codarc.codarc_events.model;

import com.google.firebase.Timestamp;

// Invite model for invitation data
public class Invite {
    
    // Invitation identification
    private String inviteId;
    private String entrantId;
    private String eventId;
    
    // Invitation details
    private InviteStatus status;
    private Timestamp invitedAt;
    private Timestamp expiry;
    private Timestamp respondedAt;
    
    // Notification tracking
    private boolean notified;
    private Timestamp notifiedAt;

    public Invite() {
        this.status = InviteStatus.PENDING;
        this.invitedAt = Timestamp.now();
        this.notified = false;
    }

    public Invite(String inviteId, String entrantId, String eventId) {
        this();
        this.inviteId = inviteId;
        this.entrantId = entrantId;
        this.eventId = eventId;
    }

    // Getters and Setters
    public String getInviteId() {
        return inviteId;
    }

    public void setInviteId(String inviteId) {
        this.inviteId = inviteId;
    }

    public String getEntrantId() {
        return entrantId;
    }

    public void setEntrantId(String entrantId) {
        this.entrantId = entrantId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public InviteStatus getStatus() {
        return status;
    }

    public void setStatus(InviteStatus status) {
        this.status = status;
    }

    public Timestamp getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(Timestamp invitedAt) {
        this.invitedAt = invitedAt;
    }

    public Timestamp getExpiry() {
        return expiry;
    }

    public void setExpiry(Timestamp expiry) {
        this.expiry = expiry;
    }

    public Timestamp getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Timestamp respondedAt) {
        this.respondedAt = respondedAt;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public Timestamp getNotifiedAt() {
        return notifiedAt;
    }

    public void setNotifiedAt(Timestamp notifiedAt) {
        this.notifiedAt = notifiedAt;
    }

    public void acceptInvitation() {
        this.status = InviteStatus.ACCEPTED;
        this.respondedAt = Timestamp.now();
    }

    public void declineInvitation() {
        this.status = InviteStatus.DECLINED;
        this.respondedAt = Timestamp.now();
    }

    public boolean isExpired() {
        if (expiry == null) {
            return false;
        }
        return Timestamp.now().compareTo(expiry) > 0;
    }

    public void markAsNotified() {
        this.notified = true;
        this.notifiedAt = Timestamp.now();
    }

    public enum InviteStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED
    }
}
