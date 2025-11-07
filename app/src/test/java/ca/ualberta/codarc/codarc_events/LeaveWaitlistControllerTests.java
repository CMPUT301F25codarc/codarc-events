package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.LeaveWaitlistController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class LeaveWaitlistControllerTests {

    private EventDB mockEventDb;
    private LeaveWaitlistController controller;

    @Before
    public void setUp() {
        mockEventDb = mock(EventDB.class);
        controller = new LeaveWaitlistController(mockEventDb);
    }

    private static Event event(String id) {
        Event e = new Event();
        e.setId(id);
        return e;
    }

    // ---------- validation ----------

    @Test
    public void leaveWaitlist_nullEvent_failsFast() {
        LeaveWaitlistController.Callback cb = mock(LeaveWaitlistController.Callback.class);

        controller.leaveWaitlist(null, "dev1", cb);

        ArgumentCaptor<LeaveWaitlistController.LeaveResult> resCap =
                ArgumentCaptor.forClass(LeaveWaitlistController.LeaveResult.class);
        verify(cb).onResult(resCap.capture());

        LeaveWaitlistController.LeaveResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Event is required", res.getMessage());

        verifyNoInteractions(mockEventDb);
    }

    @Test
    public void leaveWaitlist_emptyDeviceId_failsFast() {
        LeaveWaitlistController.Callback cb = mock(LeaveWaitlistController.Callback.class);

        controller.leaveWaitlist(event("E1"), "", cb);

        ArgumentCaptor<LeaveWaitlistController.LeaveResult> resCap =
                ArgumentCaptor.forClass(LeaveWaitlistController.LeaveResult.class);
        verify(cb).onResult(resCap.capture());

        LeaveWaitlistController.LeaveResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Device ID is required", res.getMessage());

        verifyNoInteractions(mockEventDb);
    }

    // ---------- isEntrantOnWaitlist -> false ----------

    @Test
    public void leaveWaitlist_notOnWaitlist_reportsFailure() {
        LeaveWaitlistController.Callback cb = mock(LeaveWaitlistController.Callback.class);

        controller.leaveWaitlist(event("E1"), "dev1", cb);

        ArgumentCaptor<EventDB.Callback<Boolean>> onCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E1"), eq("dev1"), onCap.capture());

        // simulate DB: not on waitlist
        onCap.getValue().onSuccess(false);

        ArgumentCaptor<LeaveWaitlistController.LeaveResult> resCap =
                ArgumentCaptor.forClass(LeaveWaitlistController.LeaveResult.class);
        verify(cb).onResult(resCap.capture());

        LeaveWaitlistController.LeaveResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("You are not registered for this event", res.getMessage());

        verify(mockEventDb, never()).leaveWaitlist(anyString(), anyString(), any());
    }

    // ---------- isEntrantOnWaitlist -> error ----------

    @Test
    public void leaveWaitlist_statusCheckError_reportsFailure() {
        LeaveWaitlistController.Callback cb = mock(LeaveWaitlistController.Callback.class);

        controller.leaveWaitlist(event("E1"), "dev1", cb);

        ArgumentCaptor<EventDB.Callback<Boolean>> onCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E1"), eq("dev1"), onCap.capture());

        onCap.getValue().onError(new RuntimeException("status down"));

        ArgumentCaptor<LeaveWaitlistController.LeaveResult> resCap =
                ArgumentCaptor.forClass(LeaveWaitlistController.LeaveResult.class);
        verify(cb).onResult(resCap.capture());

        LeaveWaitlistController.LeaveResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Failed to check status. Please try again.", res.getMessage());

        verify(mockEventDb, never()).leaveWaitlist(anyString(), anyString(), any());
    }

    // ---------- happy path ----------

    @Test
    public void leaveWaitlist_successFlow_callsDbAndReportsSuccess() {
        LeaveWaitlistController.Callback cb = mock(LeaveWaitlistController.Callback.class);

        controller.leaveWaitlist(event("E1"), "dev1", cb);

        // 1) status check -> true
        ArgumentCaptor<EventDB.Callback<Boolean>> onCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E1"), eq("dev1"), onCap.capture());
        onCap.getValue().onSuccess(true);

        // 2) leaveWaitlist -> success
        ArgumentCaptor<EventDB.Callback<Void>> leaveCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).leaveWaitlist(eq("E1"), eq("dev1"), leaveCap.capture());
        leaveCap.getValue().onSuccess(null);

        // 3) result
        ArgumentCaptor<LeaveWaitlistController.LeaveResult> resCap =
                ArgumentCaptor.forClass(LeaveWaitlistController.LeaveResult.class);
        verify(cb).onResult(resCap.capture());

        LeaveWaitlistController.LeaveResult res = resCap.getValue();
        assertTrue(res.isSuccess());
        assertEquals("You have left this event", res.getMessage());
    }

    // ---------- leave DB error ----------

    @Test
    public void leaveWaitlist_leaveDbError_reportsGenericFailure() {
        LeaveWaitlistController.Callback cb = mock(LeaveWaitlistController.Callback.class);

        controller.leaveWaitlist(event("E1"), "dev1", cb);

        // status check -> true
        ArgumentCaptor<EventDB.Callback<Boolean>> onCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E1"), eq("dev1"), onCap.capture());
        onCap.getValue().onSuccess(true);

        // leave DB -> error
        ArgumentCaptor<EventDB.Callback<Void>> leaveCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).leaveWaitlist(eq("E1"), eq("dev1"), leaveCap.capture());
        leaveCap.getValue().onError(new RuntimeException("leave failed"));

        // result
        ArgumentCaptor<LeaveWaitlistController.LeaveResult> resCap =
                ArgumentCaptor.forClass(LeaveWaitlistController.LeaveResult.class);
        verify(cb).onResult(resCap.capture());

        LeaveWaitlistController.LeaveResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Failed to leave. Please try again.", res.getMessage());
    }
}
