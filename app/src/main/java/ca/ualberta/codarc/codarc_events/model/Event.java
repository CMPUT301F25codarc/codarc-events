package ca.ualberta.codarc.codarc_events.model;

import android.graphics.Bitmap;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.Map;

/**
 * Event model class representing an event in the system.
 * 
 * Responsibilities:
 * - Store event details and metadata
 * - Maintain poster for event pages
 * - Maintain waiting list reference
 * - Validate registration window
 * - Manage event lifecycle (open or close registration)
 * - Provide geolocation checks for entry rules
 * - Generate and provide QR code for sharing
 * - Participate in selection rounds
 * 
 * Collaborators:
 * - EventRepository (data persistence)
 * - ImageStorage (poster management)
 * - WaitingList (participant management)
 * - Schedule (registration window validation)
 * - Organizer (event management)
 * - GeolocationService (location validation)
 * - QRCodeService (QR code generation)
 * - SelectionRound (lottery participation)
 */
public class Event {
    
    // Event identification
    private String eventId;
    private String name;
    private String description;
    
    // Event details
    private Timestamp dateTime;
    private GeoPoint location;
    private String locationText;
    
    // Capacity and pricing
    private Integer capacity; // null means unlimited
    private Double price;
    
    // Event status
    private EventStatus status;
    
    // Media
    private String posterUrl;
    private Bitmap qrCode;
    
    // Registration window
    private Timestamp regOpenAt;
    private Timestamp regCloseAt;
    
    // Metadata
    private String organizerId;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    // QR payload for deep linking
    private String qrPayload;

    /**
     * Default constructor
     */
    public Event() {
        this.status = EventStatus.DRAFT;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    /**
     * Constructor with basic event details
     */
    public Event(String name, String description, Timestamp dateTime, String organizerId) {
        this();
        this.name = name;
        this.description = description;
        this.dateTime = dateTime;
        this.organizerId = organizerId;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getDateTime() {
        return dateTime;
    }

    public void setDateTime(Timestamp dateTime) {
        this.dateTime = dateTime;
    }

    public GeoPoint getLocation() {
        return location;
    }

    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    public String getLocationText() {
        return locationText;
    }

    public void setLocationText(String locationText) {
        this.locationText = locationText;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public Bitmap getQrCode() {
        return qrCode;
    }

    public void setQrCode(Bitmap qrCode) {
        this.qrCode = qrCode;
    }

    public Timestamp getRegOpenAt() {
        return regOpenAt;
    }

    public void setRegOpenAt(Timestamp regOpenAt) {
        this.regOpenAt = regOpenAt;
    }

    public Timestamp getRegCloseAt() {
        return regCloseAt;
    }

    public void setRegCloseAt(Timestamp regCloseAt) {
        this.regCloseAt = regCloseAt;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }

    /**
     * Opens registration for this event
     */
    public void openRegistration() {
        this.status = EventStatus.OPEN;
        this.updatedAt = Timestamp.now();
    }

    /**
     * Closes registration for this event
     */
    public void closeRegistration() {
        this.status = EventStatus.CLOSED;
        this.updatedAt = Timestamp.now();
    }

    /**
     * Checks if registration is currently open
     */
    public boolean isRegistrationOpen() {
        if (status != EventStatus.OPEN) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        long openTime = regOpenAt != null ? regOpenAt.toDate().getTime() : 0;
        long closeTime = regCloseAt != null ? regCloseAt.toDate().getTime() : Long.MAX_VALUE;
        
        return now >= openTime && now <= closeTime;
    }

    /**
     * Checks if event has capacity available
     */
    public boolean hasCapacity() {
        return capacity == null || capacity > 0; // Assuming we track current count elsewhere
    }

    /**
     * Converts Event to Map for Firestore storage
     */
    public Map<String, Object> toMap() {
        // This would be implemented to convert the event to a Map for Firestore
        // For now, returning null as a placeholder
        return null;
    }

    /**
     * Creates Event from Firestore DocumentSnapshot
     */
    public static Event fromSnapshot(Object snapshot) {
        // This would be implemented to create an Event from Firestore data
        // For now, returning null as a placeholder
        return null;
    }

    /**
     * Event status enumeration
     */
    public enum EventStatus {
        DRAFT,
        OPEN,
        CLOSED,
        CANCELLED
    }
}
