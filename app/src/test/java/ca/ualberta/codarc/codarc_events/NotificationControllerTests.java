package ca.ualberta.codarc.codarc_events;

import android.util.Log;
import ca.ualberta.codarc.codarc_events.controllers.NotificationController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class NotificationControllerTests {

    private EventDB mockEventDb;
    private EntrantDB mockEntrantDb;
    private NotificationController controller;
    private MockedStatic<Log> logMock;

    private static final String EVENT_ID = "event-123";
    private static final String MESSAGE = "Test notification message";

    @Before
    public void setUp() {
        logMock = Mockito.mockStatic(Log.class);
        mockEventDb = mock(EventDB.class);
        mockEntrantDb = mock(EntrantDB.class);
        controller = new NotificationController(mockEventDb, mockEntrantDb);
    }

    @After
    public void tearDown() {
        if (logMock != null) {
            logMock.close();
        }
    }

    @Test
    public void validateMessage_nullOrEmpty_fails() {
        NotificationController.ValidationResult res1 = controller.validateMessage(null);
        assertFalse(res1.isValid());
        assertEquals("Message cannot be empty", res1.getErrorMessage());

        NotificationController.ValidationResult res2 = controller.validateMessage("   ");
        assertFalse(res2.isValid());
        assertEquals("Message cannot be empty", res2.getErrorMessage());
    }

    @Test
    public void validateMessage_tooLong_fails() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            sb.append("x");
        }
        String longMsg = sb.toString();

        NotificationController.ValidationResult res = controller.validateMessage(longMsg);
        assertFalse(res.isValid());
        assertEquals("Message cannot exceed 500 characters", res.getErrorMessage());
    }

    @Test
    public void validateMessage_valid_succeeds() {
        NotificationController.ValidationResult res = controller.validateMessage("Valid message");
        assertTrue(res.isValid());
        assertNull(res.getErrorMessage());
    }

    @Test
    public void notifyUsers_emptyEventId_errorsFast() {
        NotificationController.NotificationCallback cb = mock(NotificationController.NotificationCallback.class);

        controller.notifyUsers("", MESSAGE, NotificationController.NotificationCategory.WAITLIST,
                "No waitlist", cb);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());
        assertTrue(exCap.getValue() instanceof IllegalArgumentException);
        assertEquals("eventId cannot be null or empty", exCap.getValue().getMessage());

        verifyNoInteractions(mockEventDb, mockEntrantDb);
    }

    @Test
    public void notifyUsers_invalidMessage_errorsFast() {
        NotificationController.NotificationCallback cb = mock(NotificationController.NotificationCallback.class);

        controller.notifyUsers(EVENT_ID, "   ", NotificationController.NotificationCategory.WAITLIST,
                "No waitlist", cb);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());
        assertTrue(exCap.getValue() instanceof IllegalArgumentException);
        assertEquals("Message cannot be empty", exCap.getValue().getMessage());

        verifyNoInteractions(mockEventDb, mockEntrantDb);
    }

    @Test
    public void notifyUsers_emptyList_errorsWithMessage() {
        NotificationController.NotificationCallback cb = mock(NotificationController.NotificationCallback.class);

        controller.notifyUsers(EVENT_ID, MESSAGE, NotificationController.NotificationCategory.WAITLIST,
                "No waitlist found", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq(EVENT_ID), cap.capture());
        cap.getValue().onSuccess(new ArrayList<>());

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());
        assertTrue(exCap.getValue() instanceof RuntimeException);
        assertEquals("No waitlist found", exCap.getValue().getMessage());
    }

    @Test
    public void notifyUsers_waitlistCategory_callsGetWaitlist() {
        NotificationController.NotificationCallback cb = mock(NotificationController.NotificationCallback.class);

        controller.notifyUsers(EVENT_ID, MESSAGE, NotificationController.NotificationCategory.WAITLIST,
                "No waitlist", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq(EVENT_ID), cap.capture());

        List<Map<String, Object>> entrants = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("deviceId", "dev1");
        entrants.add(entry);
        cap.getValue().onSuccess(entrants);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Boolean>> prefCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getNotificationPreference(eq("dev1"), prefCap.capture());
        prefCap.getValue().onSuccess(true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).addNotification(eq("dev1"), eq(EVENT_ID), eq(MESSAGE),
                eq("waitlist_broadcast"), notifCap.capture());
        notifCap.getValue().onSuccess(null);

        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);
        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(1, notifiedCap.getValue().intValue());
        assertEquals(0, failedCap.getValue().intValue());
    }

    @Test
    public void notifyUsers_cancelledCategory_callsGetCancelled() {
        NotificationController.NotificationCallback cb = mock(NotificationController.NotificationCallback.class);

        controller.notifyUsers(EVENT_ID, MESSAGE, NotificationController.NotificationCategory.CANCELLED,
                "No cancelled", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getCancelled(eq(EVENT_ID), cap.capture());

        List<Map<String, Object>> entrants = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("deviceId", "dev1");
        entrants.add(entry);
        cap.getValue().onSuccess(entrants);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Boolean>> prefCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getNotificationPreference(eq("dev1"), prefCap.capture());
        prefCap.getValue().onSuccess(true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).addNotification(eq("dev1"), eq(EVENT_ID), eq(MESSAGE),
                eq("cancelled_broadcast"), notifCap.capture());
        notifCap.getValue().onSuccess(null);

        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);
        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(1, notifiedCap.getValue().intValue());
    }

    @Test
    public void notifyUsers_winnersCategory_skipsPreferenceCheck() {
        NotificationController.NotificationCallback cb = mock(NotificationController.NotificationCallback.class);

        controller.notifyUsers(EVENT_ID, MESSAGE, NotificationController.NotificationCategory.WINNERS,
                "No winners", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWinners(eq(EVENT_ID), cap.capture());

        List<Map<String, Object>> entrants = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("deviceId", "dev1");
        entrants.add(entry);
        cap.getValue().onSuccess(entrants);

        verify(mockEntrantDb, never()).getNotificationPreference(anyString(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).addNotification(eq("dev1"), eq(EVENT_ID), eq(MESSAGE),
                eq("winners_broadcast"), notifCap.capture());
        notifCap.getValue().onSuccess(null);

        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);
        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(1, notifiedCap.getValue().intValue());
    }

    @Test
    public void notifyUsers_enrolledCategory_callsGetEnrolled() {
        NotificationController.NotificationCallback cb = mock(NotificationController.NotificationCallback.class);

        controller.notifyUsers(EVENT_ID, MESSAGE, NotificationController.NotificationCategory.ENROLLED,
                "No enrolled", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEnrolled(eq(EVENT_ID), cap.capture());

        List<Map<String, Object>> entrants = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("deviceId", "dev1");
        entrants.add(entry);
        cap.getValue().onSuccess(entrants);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Boolean>> prefCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getNotificationPreference(eq("dev1"), prefCap.capture());
        prefCap.getValue().onSuccess(true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).addNotification(eq("dev1"), eq(EVENT_ID), eq(MESSAGE),
                eq("enrolled_broadcast"), notifCap.capture());
        notifCap.getValue().onSuccess(null);

        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);
        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(1, notifiedCap.getValue().intValue());
    }

    @Test
    public void notifyUsers_preferenceDisabled_filtersOutEntrant() {
        NotificationController.NotificationCallback cb = mock(NotificationController.NotificationCallback.class);

        controller.notifyUsers(EVENT_ID, MESSAGE, NotificationController.NotificationCategory.WAITLIST,
                "No waitlist", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq(EVENT_ID), cap.capture());

        List<Map<String, Object>> entrants = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("deviceId", "dev1");
        entrants.add(entry);
        cap.getValue().onSuccess(entrants);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Boolean>> prefCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getNotificationPreference(eq("dev1"), prefCap.capture());
        prefCap.getValue().onSuccess(false);

        verify(mockEntrantDb, never()).addNotification(anyString(), anyString(), anyString(), anyString(), any());

        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);
        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(0, notifiedCap.getValue().intValue());
    }

    @Test
    public void notifyUsers_nullDeviceId_filtersOut() {
        NotificationController.NotificationCallback cb = mock(NotificationController.NotificationCallback.class);

        controller.notifyUsers(EVENT_ID, MESSAGE, NotificationController.NotificationCategory.WAITLIST,
                "No waitlist", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq(EVENT_ID), cap.capture());

        List<Map<String, Object>> entrants = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("deviceId", null);
        entrants.add(entry);
        cap.getValue().onSuccess(entrants);

        verify(mockEntrantDb, never()).getNotificationPreference(anyString(), any());
        verify(mockEntrantDb, never()).addNotification(anyString(), anyString(), anyString(), anyString(), any());

        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);
        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(0, notifiedCap.getValue().intValue());
    }

    @Test
    public void notifyUsers_notificationFailure_countsAsFailed() {
        NotificationController.NotificationCallback cb = mock(NotificationController.NotificationCallback.class);

        controller.notifyUsers(EVENT_ID, MESSAGE, NotificationController.NotificationCategory.WINNERS,
                "No winners", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWinners(eq(EVENT_ID), cap.capture());

        List<Map<String, Object>> entrants = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("deviceId", "dev1");
        entrants.add(entry);
        cap.getValue().onSuccess(entrants);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).addNotification(eq("dev1"), eq(EVENT_ID), eq(MESSAGE),
                eq("winners_broadcast"), notifCap.capture());
        notifCap.getValue().onError(new RuntimeException("DB error"));

        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);
        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(0, notifiedCap.getValue().intValue());
        assertEquals(1, failedCap.getValue().intValue());
    }

    @Test
    public void notifyUsers_getListError_propagates() {
        NotificationController.NotificationCallback cb = mock(NotificationController.NotificationCallback.class);

        controller.notifyUsers(EVENT_ID, MESSAGE, NotificationController.NotificationCategory.WAITLIST,
                "No waitlist", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq(EVENT_ID), cap.capture());

        Exception error = new RuntimeException("DB error");
        cap.getValue().onError(error);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());
        assertSame(error, exCap.getValue());
    }
}
