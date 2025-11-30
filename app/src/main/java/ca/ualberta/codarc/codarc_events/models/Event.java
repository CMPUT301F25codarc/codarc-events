package ca.ualberta.codarc.codarc_events.models;

import java.io.Serializable;
import java.util.List;

/**
 * Plain data holder for events.
 * Mirrors what we store in Firestore.
 */
public class Event implements Serializable {

    private String id;
    private String name;
    private String description;
    private String eventDateTime;
    private String registrationOpen;
    private String registrationClose;
    private boolean open;
    private String organizerId;
    private String qrCode;
    private Integer maxCapacity;
    private String location;
    private List<String> tags;
    private String posterUrl;
    private boolean requireGeolocation;

    public Event() { }


    /**
     * Creates an Event with the given details.
     *
     * @param id event ID
     * @param name event name
     * @param description event description
     * @param eventDateTime date and time of the event
     * @param registrationOpen when registration opens
     * @param registrationClose when registration closes
     * @param open true if the event is currently open
     * @param organizerId ID of the event organizer
     * @param qrCode QR code string for the event
     */
    public Event(String id, String name, String description,
                 String eventDateTime, String registrationOpen,
                 String registrationClose, boolean open, String organizerId, String qrCode) {
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
    /** @return the event ID */
    public String getId() { return id; }

    /** @return the event name */
    public String getName() { return name; }

    /** @return the event description */
    public String getDescription() { return description; }

    /** @return the date and time of the event */
    public String getEventDateTime() { return eventDateTime; }

    /** @return when registration opens */
    public String getRegistrationOpen() { return registrationOpen; }

    /** @return when registration closes */
    public String getRegistrationClose() { return registrationClose; }

    /** @return true if the event is open */
    public boolean isOpen() { return open; }

    /** @return the organizer's ID */
    public String getOrganizerId() { return organizerId; }

    /** @return the event QR code */
    public String getQrCode() { return qrCode; }

    /** @return the maximum capacity */
    public Integer getMaxCapacity() { return maxCapacity; }

    /** @return the event location */
    public String getLocation() { return location; }

    /** @return the list of tags associated with this event */
    public List<String> getTags() { return tags; }

    /** @return the poster image URL */
    public String getPosterUrl() { return posterUrl; }

    /** @return true if entrants must share their location when joining */
    public boolean isRequireGeolocation() { return requireGeolocation; }

    // Setters

    /** @param id sets the event ID */
    public void setId(String id) { this.id = id; }
    /** @param name sets the event name */
    public void setName(String name) { this.name = name; }
    /** @param description sets the event description */
    public void setDescription(String description) { this.description = description; }

    /** @param eventDateTime sets the date and time of the event */
    public void setEventDateTime(String eventDateTime) { this.eventDateTime = eventDateTime; }

    /** @param registrationOpen sets when registration opens */
    public void setRegistrationOpen(String registrationOpen) { this.registrationOpen = registrationOpen; }

    /** @param registrationClose sets when registration closes */
    public void setRegistrationClose(String registrationClose) { this.registrationClose = registrationClose; }

    /** @param open sets whether the event is open */
    public void setOpen(boolean open) { this.open = open; }
    /** @param organizerId sets the organizer ID */
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }
    /** @param qrCode sets the event QR code */
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    /** @param maxCapacity sets the maximum capacity */
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

    /** @param location sets the event location */
    public void setLocation(String location) { this.location = location; }

    /** @param tags sets the list of tags associated with this event */
    public void setTags(List<String> tags) { this.tags = tags; }

    /** @param posterUrl sets the poster image URL */
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    /** @param requireGeolocation sets whether geolocation is required */
    public void setRequireGeolocation(boolean requireGeolocation) { this.requireGeolocation = requireGeolocation; }
}
