package ca.ualberta.codarc.codarc_events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ca.ualberta.codarc.codarc_events.models.RegistrationHistoryEntry;

/**
 * Unit tests for {@link RegistrationHistoryEntry}.
 */
public class RegistrationHistoryEntryTests {

    @Test
    public void parameterizedConstructor_setsAllFieldsCorrectly() {
        String eventId = "E123";
        String eventName = "Test Event";
        String eventDate = "2025-12-01T18:00:00";
        String selectionStatus = "Accepted";

        RegistrationHistoryEntry entry =
                new RegistrationHistoryEntry(eventId, eventName, eventDate, selectionStatus);

        assertEquals(eventId, entry.getEventId());
        assertEquals(eventName, entry.getEventName());
        assertEquals(eventDate, entry.getEventDate());
        assertEquals(selectionStatus, entry.getSelectionStatus());
    }

    @Test
    public void setters_updateFieldsCorrectly() {
        RegistrationHistoryEntry entry = new RegistrationHistoryEntry();

        entry.setEventId("E999");
        entry.setEventName("Cool Event");
        entry.setEventDate("2030-05-10T09:30:00");
        entry.setSelectionStatus("Waitlisted");

        assertEquals("E999", entry.getEventId());
        assertEquals("Cool Event", entry.getEventName());
        assertEquals("2030-05-10T09:30:00", entry.getEventDate());
        assertEquals("Waitlisted", entry.getSelectionStatus());
    }

    @Test
    public void isPastEvent_returnsTrueForPastDate() {
        RegistrationHistoryEntry entry = new RegistrationHistoryEntry();
        // Very old date so this stays in the past forever
        entry.setEventDate("2000-01-01T10:00:00");

        assertTrue(entry.isPastEvent());
    }

    @Test
    public void isPastEvent_returnsFalseForFutureDate() {
        RegistrationHistoryEntry entry = new RegistrationHistoryEntry();
        // Far future date so it stays in the future
        entry.setEventDate("2099-12-31T23:59:59");

        assertFalse(entry.isPastEvent());
    }

    @Test
    public void isPastEvent_returnsFalseWhenEventDateIsNull() {
        RegistrationHistoryEntry entry = new RegistrationHistoryEntry();
        entry.setEventDate(null);

        assertFalse(entry.isPastEvent());
    }

    @Test
    public void isPastEvent_returnsFalseWhenEventDateIsEmpty() {
        RegistrationHistoryEntry entry = new RegistrationHistoryEntry();
        entry.setEventDate("");

        assertFalse(entry.isPastEvent());
    }

    @Test
    public void isPastEvent_returnsFalseWhenEventDateMalformed() {
        RegistrationHistoryEntry entry = new RegistrationHistoryEntry();
        entry.setEventDate("not-a-date");

        assertFalse(entry.isPastEvent());
    }
}
