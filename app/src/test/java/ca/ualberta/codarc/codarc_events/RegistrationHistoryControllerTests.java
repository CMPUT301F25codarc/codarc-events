package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.RegistrationHistoryController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.RegistrationHistoryEntry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RegistrationHistoryControllerTests {

    private EntrantDB mockEntrantDb;
    private EventDB mockEventDb;
    private RegistrationHistoryController controller;

    @Before
    public void setUp() {
        mockEntrantDb = mock(EntrantDB.class);
        mockEventDb = mock(EventDB.class);
        controller = new RegistrationHistoryController(mockEntrantDb, mockEventDb);
    }

    // ---------- validation ----------

    @Test
    public void loadRegistrationHistory_emptyDeviceId_failsFast() {
        RegistrationHistoryController.Callback cb =
                mock(RegistrationHistoryController.Callback.class);

        controller.loadRegistrationHistory("", cb);

        ArgumentCaptor<RegistrationHistoryController.HistoryResult> resCap =
                ArgumentCaptor.forClass(RegistrationHistoryController.HistoryResult.class);
        verify(cb).onResult(resCap.capture());

        RegistrationHistoryController.HistoryResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Device ID is required", res.getErrorMessage());
        assertNull(res.getEntries());

        verifyNoInteractions(mockEntrantDb, mockEventDb);
    }

    // ---------- empty history ----------

    @Test
    public void loadRegistrationHistory_emptyHistory_returnsEmptyList() {
        RegistrationHistoryController.Callback cb =
                mock(RegistrationHistoryController.Callback.class);

        controller.loadRegistrationHistory("dev1", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<String>>> histCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getRegistrationHistory(eq("dev1"), histCap.capture());

        histCap.getValue().onSuccess(Collections.emptyList());

        ArgumentCaptor<RegistrationHistoryController.HistoryResult> resCap =
                ArgumentCaptor.forClass(RegistrationHistoryController.HistoryResult.class);
        verify(cb).onResult(resCap.capture());

        RegistrationHistoryController.HistoryResult res = resCap.getValue();
        assertTrue(res.isSuccess());
        assertNotNull(res.getEntries());
        assertEquals(0, res.getEntries().size());

        verifyNoInteractions(mockEventDb);
    }

    // ---------- single event: Accepted ----------

    @Test
    public void loadRegistrationHistory_singleEventAccepted_statusAccepted() {
        RegistrationHistoryController.Callback cb =
                mock(RegistrationHistoryController.Callback.class);

        controller.loadRegistrationHistory("dev1", cb);

        // history -> ["E1"]
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<String>>> histCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getRegistrationHistory(eq("dev1"), histCap.capture());
        histCap.getValue().onSuccess(Collections.singletonList("E1"));

        // eventExists(E1) -> true
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> existsCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).eventExists(eq("E1"), existsCap.capture());
        existsCap.getValue().onSuccess(true);

        // getEvent(E1) -> event
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq("E1"), eventCap.capture());

        Event event = new Event();
        event.setId("E1");
        event.setName("Event 1");
        // future date so Not Selected logic is not used
        event.setEventDateTime("2099-01-01'T'10:00:00".replace("'T'", "T"));
        eventCap.getValue().onSuccess(event);

        // isEntrantAccepted(E1, dev1) -> true
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> accCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantAccepted(eq("E1"), eq("dev1"), accCap.capture());
        accCap.getValue().onSuccess(true);

        // final result
        ArgumentCaptor<RegistrationHistoryController.HistoryResult> resCap =
                ArgumentCaptor.forClass(RegistrationHistoryController.HistoryResult.class);
        verify(cb).onResult(resCap.capture());

        RegistrationHistoryController.HistoryResult res = resCap.getValue();
        assertTrue(res.isSuccess());
        assertNotNull(res.getEntries());
        assertEquals(1, res.getEntries().size());

        RegistrationHistoryEntry entry = res.getEntries().get(0);
        assertEquals("E1", entry.getEventId());
        assertEquals("Event 1", entry.getEventName());
        assertEquals("Accepted", entry.getSelectionStatus());
    }

    // ---------- single event: Not Selected vs Waitlisted ----------

    @Test
    public void loadRegistrationHistory_pastEventNotInAnyList_notSelected() {
        RegistrationHistoryController.Callback cb =
                mock(RegistrationHistoryController.Callback.class);

        controller.loadRegistrationHistory("dev1", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<String>>> histCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getRegistrationHistory(eq("dev1"), histCap.capture());
        histCap.getValue().onSuccess(Collections.singletonList("E1"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> existsCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).eventExists(eq("E1"), existsCap.capture());
        existsCap.getValue().onSuccess(true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq("E1"), eventCap.capture());

        Event event = new Event();
        event.setId("E1");
        event.setName("Old Event");
        // definitely in the past
        event.setEventDateTime("2000-01-01T10:00:00");
        eventCap.getValue().onSuccess(event);

        // accepted -> false
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> accCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantAccepted(eq("E1"), eq("dev1"), accCap.capture());
        accCap.getValue().onSuccess(false);

        // cancelled -> false
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> cancCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantCancelled(eq("E1"), eq("dev1"), cancCap.capture());
        cancCap.getValue().onSuccess(false);

        // winner -> false
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> winCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantWinner(eq("E1"), eq("dev1"), winCap.capture());
        winCap.getValue().onSuccess(false);

        // waitlist -> false
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> wlCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E1"), eq("dev1"), wlCap.capture());
        wlCap.getValue().onSuccess(false);

        ArgumentCaptor<RegistrationHistoryController.HistoryResult> resCap =
                ArgumentCaptor.forClass(RegistrationHistoryController.HistoryResult.class);
        verify(cb).onResult(resCap.capture());

        RegistrationHistoryController.HistoryResult res = resCap.getValue();
        assertTrue(res.isSuccess());
        assertEquals(1, res.getEntries().size());
        assertEquals("Not Selected", res.getEntries().get(0).getSelectionStatus());
    }

    @Test
    public void loadRegistrationHistory_futureEventNotInAnyList_waitlisted() {
        RegistrationHistoryController.Callback cb =
                mock(RegistrationHistoryController.Callback.class);

        controller.loadRegistrationHistory("dev1", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<String>>> histCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getRegistrationHistory(eq("dev1"), histCap.capture());
        histCap.getValue().onSuccess(Collections.singletonList("E1"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> existsCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).eventExists(eq("E1"), existsCap.capture());
        existsCap.getValue().onSuccess(true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq("E1"), eventCap.capture());

        Event event = new Event();
        event.setId("E1");
        event.setName("Future Event");
        // far in the future
        event.setEventDateTime("2099-01-01T10:00:00");
        eventCap.getValue().onSuccess(event);

        // accepted -> false
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> accCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantAccepted(eq("E1"), eq("dev1"), accCap.capture());
        accCap.getValue().onSuccess(false);

        // cancelled -> false
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> cancCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantCancelled(eq("E1"), eq("dev1"), cancCap.capture());
        cancCap.getValue().onSuccess(false);

        // winner -> false
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> winCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantWinner(eq("E1"), eq("dev1"), winCap.capture());
        winCap.getValue().onSuccess(false);

        // waitlist -> false
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> wlCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E1"), eq("dev1"), wlCap.capture());
        wlCap.getValue().onSuccess(false);

        ArgumentCaptor<RegistrationHistoryController.HistoryResult> resCap =
                ArgumentCaptor.forClass(RegistrationHistoryController.HistoryResult.class);
        verify(cb).onResult(resCap.capture());

        RegistrationHistoryController.HistoryResult res = resCap.getValue();
        assertTrue(res.isSuccess());
        assertEquals(1, res.getEntries().size());
        assertEquals("Waitlisted", res.getEntries().get(0).getSelectionStatus());
    }
    // ---------- sorting by event date (most recent first) ----------

    @Test
    public void loadRegistrationHistory_entriesSortedByDateDescending() {
        RegistrationHistoryController.Callback cb =
                mock(RegistrationHistoryController.Callback.class);

        controller.loadRegistrationHistory("dev1", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<String>>> histCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getRegistrationHistory(eq("dev1"), histCap.capture());
        histCap.getValue().onSuccess(Arrays.asList("E1", "E2"));

        // eventExists both true
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> existsCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb, times(2))
                .eventExists(anyString(), existsCap.capture());
        List<EventDB.Callback<Boolean>> existsCallbacks = existsCap.getAllValues();
        existsCallbacks.get(0).onSuccess(true); // E1
        existsCallbacks.get(1).onSuccess(true); // E2

        // getEvent for both E1, E2
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb, times(2))
                .getEvent(anyString(), eventCap.capture());
        List<EventDB.Callback<Event>> eventCallbacks = eventCap.getAllValues();

        // For deterministic mapping, assume first is E1, second is E2
        Event e1 = new Event();
        e1.setId("E1");
        e1.setName("Older Event");
        e1.setEventDateTime("2020-01-01T10:00:00");

        Event e2 = new Event();
        e2.setId("E2");
        e2.setName("Newer Event");
        e2.setEventDateTime("2025-01-01T10:00:00");

        eventCallbacks.get(0).onSuccess(e1);
        eventCallbacks.get(1).onSuccess(e2);

        // accepted -> true for both so status resolution is trivial
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Boolean>> accCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb, times(2))
                .isEntrantAccepted(anyString(), eq("dev1"), accCap.capture());
        List<EventDB.Callback<Boolean>> accCallbacks = accCap.getAllValues();
        accCallbacks.get(0).onSuccess(true); // E1
        accCallbacks.get(1).onSuccess(true); // E2

        ArgumentCaptor<RegistrationHistoryController.HistoryResult> resCap =
                ArgumentCaptor.forClass(RegistrationHistoryController.HistoryResult.class);
        verify(cb).onResult(resCap.capture());

        RegistrationHistoryController.HistoryResult res = resCap.getValue();
        assertTrue(res.isSuccess());
        assertEquals(2, res.getEntries().size());

        // Most recent first -> E2, then E1
        assertEquals("E2", res.getEntries().get(0).getEventId());
        assertEquals("E1", res.getEntries().get(1).getEventId());
    }
}
