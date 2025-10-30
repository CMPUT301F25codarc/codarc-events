package ca.ualberta.codarc.codarc_events.models;

public class Event {
    private String id;
    private String name;
    private String date;
    private boolean isOpen;

    // TODO: Add timestamp, location and registrationWindow later

    public Event() {} // Firestore needs this

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

