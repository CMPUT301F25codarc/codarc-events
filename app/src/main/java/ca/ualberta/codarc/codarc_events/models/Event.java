package ca.ualberta.codarc.codarc_events.models;

/**
 * Plain data holder for events. The fields mirror what we store in Firestore
 * and are intentionally minimal at this stage of the project.
 */
public class Event {
    private String id;
    private String name;
    private String date;
    private boolean isOpen;

    // Additional fields like location and registrationWindow can be added later

    /** Required by Firestore deserializer. */
    public Event() {}

    public Event(String id, String name, String date, boolean isOpen) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.isOpen = isOpen;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDate() { return date; }
    public boolean getIsOpen() { return isOpen; }
}

