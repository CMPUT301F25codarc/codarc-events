package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.InvitationResponseController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InvitationResponseController.
 * No Android dependencies, no static mocking.
 */
public class InvitationResponseControllerTests {

    private EventDB mockEventDb;
    private EntrantDB mockEntrantDb;
    private InvitationResponseController controller;

    @Before
    public void setUp() {
        mockEventDb = mock(EventDB.class);
        mockEntrantDb = mock(EntrantDB.class);
        controller = new InvitationResponseController(mockEventDb, mockEntrantDb);
    }

    // ---------- Validation ----------

    @Test
    public void acceptInvitation_emptyEventId_yieldsErrorAndSkipsDb() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.acceptInvitation("", "dev1", "notif1", cb);

        verify(cb).onError(isA(IllegalArgumentException.class));
        verifyNoInteractions(mockEventDb);
        verifyNoInteractions(mockEntrantDb);
    }

    @Test
    public void acceptInvitation_emptyDeviceId_yieldsErrorAndSkipsDb() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.acceptInvitation("evt", "", "notif1", cb);

        verify(cb).onError(isA(IllegalArgumentException.class));
        verifyNoInteractions(mockEventDb);
        verifyNoInteractions(mockEntrantDb);
    }

    @Test
    public void acceptInvitation_emptyNotificationId_yieldsErrorAndSkipsDb() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.acceptInvitation("evt", "dev1", "", cb);

        verify(cb).onError(isA(IllegalArgumentException.class));
        verifyNoInteractions(mockEventDb);
        verifyNoInteractions(mockEntrantDb);
    }

    // ---------- Happy paths ----------

    @Test
    public void acceptInvitation_success_flowsThroughAndUpdatesNotification() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.acceptInvitation("evt", "dev1", "notif1", cb);

        // capture setEnrolledStatus callback
        ArgumentCaptor<EventDB.Callback<Void>> enrollCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).setEnrolledStatus(eq("evt"), eq("dev1"), eq(true), enrollCap.capture());

        // signal event DB success
        enrollCap.getValue().onSuccess(null);

        // capture updateNotificationState
        ArgumentCaptor<Map<String, Object>> updatesCap = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap = ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).updateNotificationState(eq("dev1"), eq("notif1"), updatesCap.capture(), notifCap.capture());

        Map<String, Object> updates = updatesCap.getValue();
        // expected keys
        assertEquals(true, updates.get("read"));
        assertEquals("accepted", updates.get("response"));
        assertNotNull(updates.get("respondedAt"));
        assertTrue(updates.get("respondedAt") instanceof Long);
        assertTrue(((Long) updates.get("respondedAt")) > 0L);

        // signal entrant DB success
        notifCap.getValue().onSuccess(null);

        verify(cb).onSuccess();
        verify(cb, never()).onError(any());
    }

    @Test
    public void declineInvitation_success_flowsThroughAndUpdatesNotification() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.declineInvitation("evt", "dev1", "notif1", cb);

        // capture setEnrolledStatus callback
        ArgumentCaptor<EventDB.Callback<Void>> enrollCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).setEnrolledStatus(eq("evt"), eq("dev1"), eq(false), enrollCap.capture());

        enrollCap.getValue().onSuccess(null);

        // capture updateNotificationState
        ArgumentCaptor<Map<String, Object>> updatesCap = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap = ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).updateNotificationState(eq("dev1"), eq("notif1"), updatesCap.capture(), notifCap.capture());

        Map<String, Object> updates = updatesCap.getValue();
        assertEquals(true, updates.get("read"));
        assertEquals("declined", updates.get("response"));
        assertNotNull(updates.get("respondedAt"));
        assertTrue(updates.get("respondedAt") instanceof Long);
        assertTrue(((Long) updates.get("respondedAt")) > 0L);

        notifCap.getValue().onSuccess(null);

        verify(cb).onSuccess();
        verify(cb, never()).onError(any());
    }

    // ---------- Failure propagation ----------

    @Test
    public void acceptInvitation_eventDbError_propagates() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.acceptInvitation("evt", "dev1", "notif1", cb);

        ArgumentCaptor<EventDB.Callback<Void>> enrollCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).setEnrolledStatus(eq("evt"), eq("dev1"), eq(true), enrollCap.capture());

        Exception boom = new RuntimeException("setEnrolledStatus failed");
        enrollCap.getValue().onError(boom);

        verify(cb).onError(boom);
        verify(mockEntrantDb, never()).updateNotificationState(anyString(), anyString(), anyMap(), any());
    }

    @Test
    public void acceptInvitation_updateNotificationError_propagates() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.acceptInvitation("evt", "dev1", "notif1", cb);

        ArgumentCaptor<EventDB.Callback<Void>> enrollCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).setEnrolledStatus(eq("evt"), eq("dev1"), eq(true), enrollCap.capture());
        enrollCap.getValue().onSuccess(null);

        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap = ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).updateNotificationState(eq("dev1"), eq("notif1"), anyMap(), notifCap.capture());

        Exception boom = new RuntimeException("updateNotificationState failed");
        notifCap.getValue().onError(boom);

        verify(cb).onError(boom);
        verify(cb, never()).onSuccess();
    }
}
