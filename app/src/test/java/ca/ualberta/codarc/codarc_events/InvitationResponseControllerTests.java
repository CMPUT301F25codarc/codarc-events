package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.InvitationResponseController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        // After decline, automatic reselection is triggered
        // Mock empty replacement pool and waitlist to complete flow
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> poolCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getReplacementPool(eq("evt"), poolCap.capture());
        poolCap.getValue().onSuccess(new ArrayList<>());

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> waitlistCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("evt"), waitlistCap.capture());
        waitlistCap.getValue().onSuccess(new ArrayList<>());

        // Log decline (no replacement)
        ArgumentCaptor<EventDB.Callback<Void>> logCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).logDeclineReplacement(eq("evt"), eq("dev1"), isNull(), isNull(), eq(false), logCap.capture());
        logCap.getValue().onSuccess(null);

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

    // ---------- Automatic reselection tests ----------

    @Test
    public void declineInvitation_withReplacementPool_automaticallySelectsReplacement() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.declineInvitation("evt", "dev1", "notif1", cb);

        // Complete decline flow
        ArgumentCaptor<EventDB.Callback<Void>> enrollCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).setEnrolledStatus(eq("evt"), eq("dev1"), eq(false), enrollCap.capture());
        enrollCap.getValue().onSuccess(null);

        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap = ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).updateNotificationState(eq("dev1"), eq("notif1"), anyMap(), notifCap.capture());
        notifCap.getValue().onSuccess(null);

        // Mock replacement pool with entry
        List<Map<String, Object>> pool = new ArrayList<>();
        Map<String, Object> poolEntry = new HashMap<>();
        poolEntry.put("deviceId", "replacement1");
        pool.add(poolEntry);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> poolCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getReplacementPool(eq("evt"), poolCap.capture());
        poolCap.getValue().onSuccess(pool);

        // Verify markReplacement called
        ArgumentCaptor<EventDB.Callback<Void>> markCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).markReplacement(eq("evt"), eq("replacement1"), markCap.capture());
        markCap.getValue().onSuccess(null);

        // Verify notification sent
        ArgumentCaptor<EntrantDB.Callback<Void>> addNotifCap = ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).addNotification(eq("replacement1"), eq("evt"), anyString(), eq("winner"), addNotifCap.capture());
        addNotifCap.getValue().onSuccess(null);

        // Verify logging
        ArgumentCaptor<EventDB.Callback<Void>> logCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).logDeclineReplacement(eq("evt"), eq("dev1"), eq("replacement1"), eq("replacementPool"), eq(true), logCap.capture());
        logCap.getValue().onSuccess(null);

        verify(cb).onSuccess();
    }

    @Test
    public void declineInvitation_withEmptyPoolButWaitlist_selectsFromWaitlist() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.declineInvitation("evt", "dev1", "notif1", cb);

        // Complete decline flow
        ArgumentCaptor<EventDB.Callback<Void>> enrollCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).setEnrolledStatus(eq("evt"), eq("dev1"), eq(false), enrollCap.capture());
        enrollCap.getValue().onSuccess(null);

        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap = ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).updateNotificationState(eq("dev1"), eq("notif1"), anyMap(), notifCap.capture());
        notifCap.getValue().onSuccess(null);

        // Mock empty replacement pool
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> poolCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getReplacementPool(eq("evt"), poolCap.capture());
        poolCap.getValue().onSuccess(new ArrayList<>());

        // Mock waitlist with entry
        List<Map<String, Object>> waitlist = new ArrayList<>();
        Map<String, Object> waitlistEntry = new HashMap<>();
        waitlistEntry.put("deviceId", "waitlist1");
        waitlist.add(waitlistEntry);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> waitlistCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("evt"), waitlistCap.capture());
        waitlistCap.getValue().onSuccess(waitlist);

        // Verify promoteFromWaitlist called
        ArgumentCaptor<EventDB.Callback<Void>> promoteCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).promoteFromWaitlist(eq("evt"), eq("waitlist1"), promoteCap.capture());
        promoteCap.getValue().onSuccess(null);

        // Verify notification sent
        ArgumentCaptor<EntrantDB.Callback<Void>> addNotifCap = ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).addNotification(eq("waitlist1"), eq("evt"), anyString(), eq("winner"), addNotifCap.capture());
        addNotifCap.getValue().onSuccess(null);

        // Verify logging
        ArgumentCaptor<EventDB.Callback<Void>> logCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).logDeclineReplacement(eq("evt"), eq("dev1"), eq("waitlist1"), eq("waitlist"), eq(true), logCap.capture());
        logCap.getValue().onSuccess(null);

        verify(cb).onSuccess();
    }

    @Test
    public void declineInvitation_withNoReplacementAvailable_logsDeclineOnly() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.declineInvitation("evt", "dev1", "notif1", cb);

        // Complete decline flow
        ArgumentCaptor<EventDB.Callback<Void>> enrollCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).setEnrolledStatus(eq("evt"), eq("dev1"), eq(false), enrollCap.capture());
        enrollCap.getValue().onSuccess(null);

        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap = ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).updateNotificationState(eq("dev1"), eq("notif1"), anyMap(), notifCap.capture());
        notifCap.getValue().onSuccess(null);

        // Mock empty replacement pool
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> poolCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getReplacementPool(eq("evt"), poolCap.capture());
        poolCap.getValue().onSuccess(new ArrayList<>());

        // Mock empty waitlist
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> waitlistCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("evt"), waitlistCap.capture());
        waitlistCap.getValue().onSuccess(new ArrayList<>());

        // Verify logging (no replacement)
        ArgumentCaptor<EventDB.Callback<Void>> logCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).logDeclineReplacement(eq("evt"), eq("dev1"), isNull(), isNull(), eq(false), logCap.capture());
        logCap.getValue().onSuccess(null);

        // Verify no promotion or notification
        verify(mockEventDb, never()).markReplacement(anyString(), anyString(), any());
        verify(mockEventDb, never()).promoteFromWaitlist(anyString(), anyString(), any());
        verify(mockEntrantDb, never()).addNotification(anyString(), anyString(), anyString(), anyString(), any());

        verify(cb).onSuccess();
    }

    @Test
    public void declineInvitation_replacementSelectionFails_declineStillSucceeds() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.declineInvitation("evt", "dev1", "notif1", cb);

        // Complete decline flow
        ArgumentCaptor<EventDB.Callback<Void>> enrollCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).setEnrolledStatus(eq("evt"), eq("dev1"), eq(false), enrollCap.capture());
        enrollCap.getValue().onSuccess(null);

        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap = ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).updateNotificationState(eq("dev1"), eq("notif1"), anyMap(), notifCap.capture());
        notifCap.getValue().onSuccess(null);

        // Mock replacement pool query failure
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> poolCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getReplacementPool(eq("evt"), poolCap.capture());
        poolCap.getValue().onError(new RuntimeException("Pool query failed"));

        // Should fall back to waitlist
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> waitlistCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("evt"), waitlistCap.capture());
        waitlistCap.getValue().onSuccess(new ArrayList<>());

        // Verify logging
        ArgumentCaptor<EventDB.Callback<Void>> logCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).logDeclineReplacement(eq("evt"), eq("dev1"), isNull(), isNull(), eq(false), logCap.capture());
        logCap.getValue().onSuccess(null);

        verify(cb).onSuccess();
    }

    @Test
    public void declineInvitation_replacementPromotionFails_declineStillSucceeds() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.declineInvitation("evt", "dev1", "notif1", cb);

        // Complete decline flow
        ArgumentCaptor<EventDB.Callback<Void>> enrollCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).setEnrolledStatus(eq("evt"), eq("dev1"), eq(false), enrollCap.capture());
        enrollCap.getValue().onSuccess(null);

        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap = ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).updateNotificationState(eq("dev1"), eq("notif1"), anyMap(), notifCap.capture());
        notifCap.getValue().onSuccess(null);

        // Mock replacement pool with entry
        List<Map<String, Object>> pool = new ArrayList<>();
        Map<String, Object> poolEntry = new HashMap<>();
        poolEntry.put("deviceId", "replacement1");
        pool.add(poolEntry);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> poolCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getReplacementPool(eq("evt"), poolCap.capture());
        poolCap.getValue().onSuccess(pool);

        // Mock markReplacement failure
        ArgumentCaptor<EventDB.Callback<Void>> markCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).markReplacement(eq("evt"), eq("replacement1"), markCap.capture());
        markCap.getValue().onError(new RuntimeException("Promotion failed"));

        // Should still log the decline
        ArgumentCaptor<EventDB.Callback<Void>> logCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).logDeclineReplacement(eq("evt"), eq("dev1"), eq("replacement1"), eq("replacementPool"), eq(false), logCap.capture());
        logCap.getValue().onSuccess(null);

        verify(cb).onSuccess();
    }

    @Test
    public void declineInvitation_loggingFails_declineStillSucceeds() {
        InvitationResponseController.ResponseCallback cb = mock(InvitationResponseController.ResponseCallback.class);

        controller.declineInvitation("evt", "dev1", "notif1", cb);

        // Complete decline flow
        ArgumentCaptor<EventDB.Callback<Void>> enrollCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).setEnrolledStatus(eq("evt"), eq("dev1"), eq(false), enrollCap.capture());
        enrollCap.getValue().onSuccess(null);

        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap = ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).updateNotificationState(eq("dev1"), eq("notif1"), anyMap(), notifCap.capture());
        notifCap.getValue().onSuccess(null);

        // Mock empty replacement pool and waitlist
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> poolCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getReplacementPool(eq("evt"), poolCap.capture());
        poolCap.getValue().onSuccess(new ArrayList<>());

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> waitlistCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("evt"), waitlistCap.capture());
        waitlistCap.getValue().onSuccess(new ArrayList<>());

        // Mock logging failure
        ArgumentCaptor<EventDB.Callback<Void>> logCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).logDeclineReplacement(eq("evt"), eq("dev1"), isNull(), isNull(), eq(false), logCap.capture());
        logCap.getValue().onError(new RuntimeException("Logging failed"));

        // Decline should still succeed
        verify(cb).onSuccess();
    }
}
