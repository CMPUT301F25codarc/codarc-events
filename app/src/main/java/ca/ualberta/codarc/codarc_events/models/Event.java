package ca.ualberta.codarc.codarc_events.models;

/**
 * Plain data holder for events.
 * Mirrors what we store in Firestore.
 */
public class Event {

    private String id;
    private String name;
    private String description;
    private String eventDateTime;       // combined field
    private String registrationOpen;
    private String registrationClose;
    private boolean open;

    public Event() { }

    public Event(String id, String name, String description,
                 String eventDateTime, String registrationOpen,
                 String registrationClose, boolean open) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.eventDateTime = eventDateTime;
        this.registrationOpen = registrationOpen;
        this.registrationClose = registrationClose;
        this.open = open;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getEventDateTime() { return eventDateTime; }
    public String getRegistrationOpen() { return registrationOpen; }
    public String getRegistrationClose() { return registrationClose; }
    public boolean isOpen() { return open; }

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setEventDateTime(String eventDateTime) { this.eventDateTime = eventDateTime; }
    public void setRegistrationOpen(String registrationOpen) { this.registrationOpen = registrationOpen; }
    public void setRegistrationClose(String registrationClose) { this.registrationClose = registrationClose; }
    public void setOpen(boolean open) { this.open = open; }
}

