package ca.ualberta.codarc.codarc_events;

import org.junit.Test;
import static org.junit.Assert.*;

import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Unit tests for the Event model.
 */
public class EventTest {

    @Test
    public void eventConstructor_initializesFieldsCorrectly() {
        Event event = new Event(
                "E1",
                "Sample Event",
                "Description text",
                "2025-11-10 10:00 AM",
                "2025-11-01 09:00 AM",
                "2025-11-09 09:00 PM",
                true,
                "123456",
                "123456"
        );

        assertEquals("E1", event.getId());
        assertEquals("Sample Event", event.getName());
        assertEquals("Description text", event.getDescription());
        assertEquals("2025-11-10 10:00 AM", event.getEventDateTime());
        assertEquals("2025-11-01 09:00 AM", event.getRegistrationOpen());
        assertEquals("2025-11-09 09:00 PM", event.getRegistrationClose());
        assertTrue(event.isOpen());
        assertEquals("123456", event.getOrganizerId());
        assertEquals("123456", event.getQrCode());
    }

    @Test
    public void setters_updateFieldsCorrectly() {
        Event event = new Event();
        event.setId("E2");
        event.setName("New Event");
        event.setDescription("Updated description");
        event.setEventDateTime("2025-12-01 07:00 PM");
        event.setRegistrationOpen("2025-11-20 08:00 AM");
        event.setRegistrationClose("2025-11-30 10:00 PM");
        event.setOpen(false);
        event.setOrganizerId("ORG123");

        assertEquals("E2", event.getId());
        assertEquals("New Event", event.getName());
        assertEquals("Updated description", event.getDescription());
        assertEquals("2025-12-01 07:00 PM", event.getEventDateTime());
        assertEquals("2025-11-20 08:00 AM", event.getRegistrationOpen());
        assertEquals("2025-11-30 10:00 PM", event.getRegistrationClose());
        assertFalse(event.isOpen());
        assertEquals("ORG123", event.getOrganizerId());
    }

    @Test
    public void eventCanToggleOpenStatus() {
        Event event = new Event();
        event.setOpen(true);
        assertTrue(event.isOpen());

        event.setOpen(false);
        assertFalse(event.isOpen());
    }

    @Test
    public void eventAllowsNullDescription() {
        Event event = new Event();
        event.setDescription(null);
        assertNull(event.getDescription());
    }
}