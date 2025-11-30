package ca.ualberta.codarc.codarc_events.models;

import java.util.List;

/**
 * Helper class to hold organizer and their recent events.
 */
public class OrganizerWithEvents {
    private Organizer organizer;
    private List<Event> recentEvents;

    public OrganizerWithEvents(Organizer organizer, List<Event> recentEvents) {
        this.organizer = organizer;
        this.recentEvents = recentEvents;
    }

    public Organizer getOrganizer() {
        return organizer;
    }

    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    public List<Event> getRecentEvents() {
        return recentEvents;
    }

    public void setRecentEvents(List<Event> recentEvents) {
        this.recentEvents = recentEvents;
    }
}
