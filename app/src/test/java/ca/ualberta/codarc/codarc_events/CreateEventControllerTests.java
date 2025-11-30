package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.CreateEventController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;

public class CreateEventControllerTests {

    private EventDB mockDb;
    private CreateEventController controller;

    // Valid baseline inputs
    private static final String NAME = " Launch Party ";
    private static final String DESC = "  Free snacks  ";
    private static final String LOCATION = "  CSC Atrium  ";
    private static final String EVENT_AT = "2025-12-01T18:00";
    private static final String REG_OPEN = "2025-11-20T09:00";
    private static final String REG_CLOSE = "2025-11-29T23:59";
    private static final String ORGANIZER = "organizer-xyz";

    @Before
    public void setUp() {
        mockDb = mock(EventDB.class);
        controller = new CreateEventController(mockDb, ORGANIZER);
    }

    // ---------- validateAndCreateEvent: happy path ----------

    @Test
    public void validateAndCreateEvent_success_trimsFields_setsGeneratedIdAndQr_openTrue() {
        var res = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, REG_OPEN, REG_CLOSE, "", Collections.emptyList(), "", false
        );

        assertTrue(res.isValid());
        assertNull(res.getErrorMessage());
        Event e = res.getEvent();
        assertNotNull(e);

        // Trimmed fields
        assertEquals("Launch Party", e.getName());
        assertEquals("Free snacks", e.getDescription());
        assertEquals("CSC Atrium", e.getLocation());

        // Required passthroughs
        assertEquals(EVENT_AT, e.getEventDateTime());
        assertEquals(REG_OPEN, e.getRegistrationOpen());
        assertEquals(REG_CLOSE, e.getRegistrationClose());

        // Organizer + generated identifiers
        assertEquals(ORGANIZER, e.getOrganizerId());
        assertNotNull(e.getId());
        assertFalse(e.getId().trim().isEmpty());
        assertNotNull(e.getQrCode());
        assertTrue(e.getQrCode().startsWith("event:"));

        // Flags and capacity
        assertTrue(e.isOpen());
        assertNull(e.getMaxCapacity());
    }

    // ---------- validateAndCreateEvent: required fields ----------

    @Test
    public void validateAndCreateEvent_missingName_fails() {
        var r = controller.validateAndCreateEvent(
                null, DESC, EVENT_AT, LOCATION, REG_OPEN, REG_CLOSE, "", Collections.emptyList(), "", false
        );
        assertFalse(r.isValid());
        assertEquals("Event name is required", r.getErrorMessage());
        assertNull(r.getEvent());
    }

    @Test
    public void validateAndCreateEvent_emptyName_fails() {
        var r = controller.validateAndCreateEvent(
                "  ", DESC, EVENT_AT, LOCATION, REG_OPEN, REG_CLOSE, "", Collections.emptyList(), "", false
        );
        assertFalse(r.isValid());
        assertEquals("Event name is required", r.getErrorMessage());
        assertNull(r.getEvent());
    }

    @Test
    public void validateAndCreateEvent_missingEventDate_fails() {
        var r = controller.validateAndCreateEvent(
                NAME, DESC, null, LOCATION, REG_OPEN, REG_CLOSE, "", Collections.emptyList(), "", false
        );
        assertFalse(r.isValid());
        assertEquals("Event date/time is required", r.getErrorMessage());
        assertNull(r.getEvent());
    }

    @Test
    public void validateAndCreateEvent_emptyEventDate_fails() {
        var r = controller.validateAndCreateEvent(
                NAME, DESC, "  ", LOCATION, REG_OPEN, REG_CLOSE, "", Collections.emptyList(), "", false
        );
        assertFalse(r.isValid());
        assertEquals("Event date/time is required", r.getErrorMessage());
        assertNull(r.getEvent());
    }

    @Test
    public void validateAndCreateEvent_missingRegOpen_fails() {
        var r = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, null, REG_CLOSE, "", Collections.emptyList(), "", false
        );
        assertFalse(r.isValid());
        assertEquals("Registration open date is required", r.getErrorMessage());
        assertNull(r.getEvent());
    }

    @Test
    public void validateAndCreateEvent_emptyRegOpen_fails() {
        var r = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, "  ", REG_CLOSE, "", Collections.emptyList(), "", false
        );
        assertFalse(r.isValid());
        assertEquals("Registration open date is required", r.getErrorMessage());
        assertNull(r.getEvent());
    }

    @Test
    public void validateAndCreateEvent_missingRegClose_fails() {
        var r = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, REG_OPEN, null, "", Collections.emptyList(), "", false
        );
        assertFalse(r.isValid());
        assertEquals("Registration close date is required", r.getErrorMessage());
        assertNull(r.getEvent());
    }

    @Test
    public void validateAndCreateEvent_emptyRegClose_fails() {
        var r = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, REG_OPEN, "  ", "", Collections.emptyList(), "", false
        );
        assertFalse(r.isValid());
        assertEquals("Registration close date is required", r.getErrorMessage());
        assertNull(r.getEvent());
    }

    // ---------- validateAndCreateEvent: capacity behavior (as-implemented) ----------

    @Test
    public void validateAndCreateEvent_capacity_parsesPositiveInt() {
        var r = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, REG_OPEN, REG_CLOSE, "42", Collections.emptyList(), "", false
        );
        assertTrue(r.isValid());
        assertEquals(Integer.valueOf(42), r.getEvent().getMaxCapacity());
    }

    @Test
    public void validateAndCreateEvent_capacity_zeroBecomesNull() {
        var r = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, REG_OPEN, REG_CLOSE, "0", Collections.emptyList(), "", false
        );
        assertTrue(r.isValid());
        assertNull(r.getEvent().getMaxCapacity());
    }

    @Test
    public void validateAndCreateEvent_capacity_negativeBecomesNull() {
        var r = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, REG_OPEN, REG_CLOSE, "-5", Collections.emptyList(), "", false
        );
        assertTrue(r.isValid());
        assertNull(r.getEvent().getMaxCapacity());
    }

    @Test
    public void validateAndCreateEvent_capacity_badStringBecomesNull() {
        var r = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, REG_OPEN, REG_CLOSE, "abc", Collections.emptyList(), "", false
        );
        assertTrue(r.isValid());
        assertNull(r.getEvent().getMaxCapacity());
    }

    // ---------- validateAndCreateEvent: tags and poster ----------

    @Test
    public void validateAndCreateEvent_withTags_setsTags() {
        var tags = Arrays.asList("music", "outdoor");
        var r = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, REG_OPEN, REG_CLOSE, "", tags, "", false
        );
        assertTrue(r.isValid());
        assertEquals(tags, r.getEvent().getTags());
    }

    @Test
    public void validateAndCreateEvent_withPosterUrl_setsPosterUrl() {
        String posterUrl = "https://example.com/poster.jpg";
        var r = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, REG_OPEN, REG_CLOSE, "", Collections.emptyList(), posterUrl, false
        );
        assertTrue(r.isValid());
        assertEquals(posterUrl, r.getEvent().getPosterUrl());
    }

    // ---------- persistEvent ----------

    @Test
    public void persistEvent_happyPath_forwardsToDbAddEvent() {
        var res = controller.validateAndCreateEvent(
                NAME, DESC, EVENT_AT, LOCATION, REG_OPEN, REG_CLOSE, "", Collections.emptyList(), "", false
        );
        assertTrue(res.isValid());
        Event e = res.getEvent();

        @SuppressWarnings("unchecked")
        EventDB.Callback<Void> cb = mock(EventDB.Callback.class);

        controller.persistEvent(e, cb);

        ArgumentCaptor<Event> eventCap = ArgumentCaptor.forClass(Event.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Void>> cbCap = ArgumentCaptor.forClass(EventDB.Callback.class);

        verify(mockDb, times(1)).addEvent(eventCap.capture(), cbCap.capture());
        assertEquals(e.getId(), eventCap.getValue().getId());
        assertSame(cb, cbCap.getValue());
    }

    @Test
    public void persistEvent_nullEvent_callsOnError_andDoesNotTouchDb() {
        @SuppressWarnings("unchecked")
        EventDB.Callback<Void> cb = mock(EventDB.Callback.class);

        controller.persistEvent(null, cb);

        verify(mockDb, never()).addEvent(any(), any());
        verify(cb, times(1)).onError(any(IllegalArgumentException.class));
    }

    // ---------- canAddTag ----------

    @Test
    public void canAddTag_validNewTag_returnsTrue() {
        assertTrue(controller.canAddTag("sports", Arrays.asList("music", "outdoor")));
    }

    @Test
    public void canAddTag_duplicateTag_returnsFalse() {
        assertFalse(controller.canAddTag("music", Arrays.asList("music", "outdoor")));
    }

    @Test
    public void canAddTag_duplicateCaseInsensitive_returnsFalse() {
        assertFalse(controller.canAddTag("MUSIC", Arrays.asList("music", "outdoor")));
    }

    @Test
    public void canAddTag_nullTag_returnsFalse() {
        assertFalse(controller.canAddTag(null, Arrays.asList("music")));
    }

    @Test
    public void canAddTag_emptyTag_returnsFalse() {
        assertFalse(controller.canAddTag("  ", Arrays.asList("music")));
    }

    @Test
    public void canAddTag_emptyList_returnsTrue() {
        assertTrue(controller.canAddTag("sports", Collections.emptyList()));
    }

    @Test
    public void canAddTag_nullList_returnsTrue() {
        assertTrue(controller.canAddTag("sports", null));
    }
}