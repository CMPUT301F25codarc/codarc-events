package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.Organizer;
import ca.ualberta.codarc.codarc_events.models.OrganizerWithEvents;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class OrganizerWithEventsModelTests {

    @Test
    public void constructor_setsFields() {
        Organizer organizer = new Organizer();
        organizer.setDeviceId("org-123");

        Event event1 = new Event();
        event1.setId("event-1");
        Event event2 = new Event();
        event2.setId("event-2");
        List<Event> events = Arrays.asList(event1, event2);

        OrganizerWithEvents owe = new OrganizerWithEvents(organizer, events);

        assertSame(organizer, owe.getOrganizer());
        assertSame(events, owe.getRecentEvents());
    }

    @Test
    public void setters_roundTripValues() {
        Organizer organizer1 = new Organizer();
        organizer1.setDeviceId("org-1");
        Organizer organizer2 = new Organizer();
        organizer2.setDeviceId("org-2");

        Event event1 = new Event();
        event1.setId("event-1");
        List<Event> events1 = Arrays.asList(event1);

        Event event2 = new Event();
        event2.setId("event-2");
        List<Event> events2 = Arrays.asList(event2);

        OrganizerWithEvents owe = new OrganizerWithEvents(organizer1, events1);

        owe.setOrganizer(organizer2);
        owe.setRecentEvents(events2);

        assertSame(organizer2, owe.getOrganizer());
        assertSame(events2, owe.getRecentEvents());
    }

    @Test
    public void constructor_emptyEventsList_allowed() {
        Organizer organizer = new Organizer();
        organizer.setDeviceId("org-123");

        OrganizerWithEvents owe = new OrganizerWithEvents(organizer, Collections.emptyList());

        assertSame(organizer, owe.getOrganizer());
        assertTrue(owe.getRecentEvents().isEmpty());
    }
}
