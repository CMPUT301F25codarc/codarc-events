package ca.ualberta.codarc.codarc_events.model;

import android.graphics.Bitmap;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.Map;

// Event model for storing event data
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

    public Event() {
        this.status = EventStatus.DRAFT;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

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

    public void openRegistration() {
        this.status = EventStatus.OPEN;
        this.updatedAt = Timestamp.now();
    }

    public void closeRegistration() {
        this.status = EventStatus.CLOSED;
        this.updatedAt = Timestamp.now();
    }

    public boolean isRegistrationOpen() {
        if (status != EventStatus.OPEN) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        long openTime = regOpenAt != null ? regOpenAt.toDate().getTime() : 0;
        long closeTime = regCloseAt != null ? regCloseAt.toDate().getTime() : Long.MAX_VALUE;
        
        return now >= openTime && now <= closeTime;
    }

    public boolean hasCapacity() {
        return capacity == null || capacity > 0;
    }

    public Map<String, Object> toMap() {
        // TODO: implement Firestore conversion
        return null;
    }

    public static Event fromSnapshot(Object snapshot) {
        // TODO: implement Firestore parsing
        return null;
    }

    public enum EventStatus {
        DRAFT,
        OPEN,
        CLOSED,
        CANCELLED
    }
}
