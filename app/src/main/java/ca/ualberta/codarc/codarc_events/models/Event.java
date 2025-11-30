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
    public String getId() { return id; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public String getEventDateTime() { return eventDateTime; }

    public String getRegistrationOpen() { return registrationOpen; }

    public String getRegistrationClose() { return registrationClose; }

    public boolean isOpen() { return open; }

    public String getOrganizerId() { return organizerId; }

    public String getQrCode() { return qrCode; }

    public Integer getMaxCapacity() { return maxCapacity; }

    public String getLocation() { return location; }

    public List<String> getTags() { return tags; }

    public String getPosterUrl() { return posterUrl; }

    /** @return true if entrants must share their location when joining */
    public boolean isRequireGeolocation() { return requireGeolocation; }

    // Setters
    public void setId(String id) { this.id = id; }

    public void setName(String name) { this.name = name; }

    public void setDescription(String description) { this.description = description; }

    public void setEventDateTime(String eventDateTime) { this.eventDateTime = eventDateTime; }

    public void setRegistrationOpen(String registrationOpen) { this.registrationOpen = registrationOpen; }

    public void setRegistrationClose(String registrationClose) { this.registrationClose = registrationClose; }

    public void setOpen(boolean open) { this.open = open; }

    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public void setQrCode(String qrCode) { this.qrCode = qrCode; }

    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

    public void setLocation(String location) { this.location = location; }

    public void setTags(List<String> tags) { this.tags = tags; }

    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    /** @param requireGeolocation sets whether geolocation is required */
    public void setRequireGeolocation(boolean requireGeolocation) { this.requireGeolocation = requireGeolocation; }
}
