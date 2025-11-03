package ca.ualberta.codarc.codarc_events.models;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;

/**
 * Plain data holder for events.
 * Mirrors what we store in Firestore.
 * Date fields use Object type to support Firestore Timestamp while maintaining backwards compatibility.
 * Includes custom serialization to handle Timestamp objects when passing via Intent.
 */
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private transient Object eventDateTime;
    private transient Object registrationOpen;
    private transient Object registrationClose;
    private boolean open;
    private String organizerId;
    private String qrCode;
    private Integer maxCapacity;
    private String location;

    public Event() { }

    public Event(String id, String name, String description,
                 Object eventDateTime, Object registrationOpen,
                 Object registrationClose, boolean open, String organizerId, String qrCode) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.eventDateTime = eventDateTime;
        this.registrationOpen = registrationOpen;
        this.registrationClose = registrationClose;
        this.open = open;
        this.organizerId = organizerId;
        this.qrCode = qrCode;
    }


    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Object getEventDateTime() { return eventDateTime; }
    public Object getRegistrationOpen() { return registrationOpen; }
    public Object getRegistrationClose() { return registrationClose; }
    public boolean isOpen() { return open; }
    public String getOrganizerId() { return organizerId; }

    public String getQrCode() { return qrCode; }
    public Integer getMaxCapacity() { return maxCapacity; }
    public String getLocation() { return location; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setEventDateTime(Object eventDateTime) { this.eventDateTime = eventDateTime; }
    public void setRegistrationOpen(Object registrationOpen) { this.registrationOpen = registrationOpen; }
    public void setRegistrationClose(Object registrationClose) { this.registrationClose = registrationClose; }
    public void setOpen(boolean open) { this.open = open; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
    public void setLocation(String location) { this.location = location; }

    /**
     * Custom serialization to handle Timestamp objects.
     * Converts Timestamp to long milliseconds for serialization.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        
        // Convert Timestamp objects to Long for serialization
        long eventDateTimeMillis = convertToMillis(eventDateTime);
        long regOpenMillis = convertToMillis(registrationOpen);
        long regCloseMillis = convertToMillis(registrationClose);
        
        out.writeLong(eventDateTimeMillis);
        out.writeLong(regOpenMillis);
        out.writeLong(regCloseMillis);
    }

    /**
     * Custom deserialization to restore Timestamp objects.
     * Converts long milliseconds back to Date objects (Firestore will handle Timestamp on read).
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        
        // Read millis and convert to Date (which Firestore can convert to Timestamp)
        long eventDateTimeMillis = in.readLong();
        long regOpenMillis = in.readLong();
        long regCloseMillis = in.readLong();
        
        eventDateTime = eventDateTimeMillis != Long.MAX_VALUE ? new Date(eventDateTimeMillis) : null;
        registrationOpen = regOpenMillis != Long.MAX_VALUE ? new Date(regOpenMillis) : null;
        registrationClose = regCloseMillis != Long.MAX_VALUE ? new Date(regCloseMillis) : null;
    }

    /**
     * Converts a timestamp object to milliseconds.
     * Handles Timestamp, Date, Long, and String formats.
     */
    private long convertToMillis(Object timeObj) {
        if (timeObj == null) {
            return Long.MAX_VALUE;
        }
        if (timeObj instanceof Long) {
            return (Long) timeObj;
        }
        if (timeObj instanceof Date) {
            return ((Date) timeObj).getTime();
        }
        if (timeObj instanceof String) {
            try {
                java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                return isoFormat.parse((String) timeObj).getTime();
            } catch (Exception e) {
                return Long.MAX_VALUE;
            }
        }
        // Handle Firestore Timestamp using reflection
        try {
            String className = timeObj.getClass().getName();
            if (className.contains("Timestamp")) {
                Method toDateMethod = timeObj.getClass().getMethod("toDate");
                Date date = (Date) toDateMethod.invoke(timeObj);
                return date.getTime();
            }
        } catch (Exception e) {
            // Reflection failed
        }
        return Long.MAX_VALUE;
    }
}

