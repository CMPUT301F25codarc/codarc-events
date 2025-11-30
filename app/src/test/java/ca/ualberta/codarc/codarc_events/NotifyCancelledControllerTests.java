package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.NotifyCancelledController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class NotifyCancelledControllerTests {

    private EventDB mockEventDb;
    private EntrantDB mockEntrantDb;
    private NotifyCancelledController controller;

    @Before
    public void setUp() {
        mockEventDb = mock(EventDB.class);
        mockEntrantDb = mock(EntrantDB.class);
        controller = new NotifyCancelledController(mockEventDb, mockEntrantDb);
    }

    @Test
    public void validateMessage_nullOrEmpty_fails() {
        NotifyCancelledController.ValidationResult res1 = controller.validateMessage(null);
        assertFalse(res1.isValid());
        assertEquals("Message cannot be empty", res1.getErrorMessage());

        NotifyCancelledController.ValidationResult res2 = controller.validateMessage("   ");
        assertFalse(res2.isValid());
        assertEquals("Message cannot be empty", res2.getErrorMessage());
    }

    @Test
    public void validateMessage_tooLong_fails() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            sb.append("a");
        }
        String longMessage = sb.toString();

        NotifyCancelledController.ValidationResult res = controller.validateMessage(longMessage);
        assertFalse(res.isValid());
        assertEquals("Message cannot exceed 500 characters", res.getErrorMessage());
    }

    @Test
    public void validateMessage_valid_succeeds() {
        NotifyCancelledController.ValidationResult res = controller.validateMessage("All good");
        assertTrue(res.isValid());
        assertNull(res.getErrorMessage());
    }

    @Test
    public void notifyCancelled_emptyEventId_errorsFast() {
        NotifyCancelledController.NotifyCancelledCallback cb =
                mock(NotifyCancelledController.NotifyCancelledCallback.class);

        controller.notifyCancelled("", "Hello", cb);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());
        assertTrue(exCap.getValue() instanceof IllegalArgumentException);
        assertEquals("eventId cannot be null or empty", exCap.getValue().getMessage());

        verifyNoInteractions(mockEventDb, mockEntrantDb);
    }

    @Test
    public void notifyCancelled_invalidMessage_usesValidationError() {
        NotifyCancelledController.NotifyCancelledCallback cb =
                mock(NotifyCancelledController.NotifyCancelledCallback.class);

        controller.notifyCancelled("E1", "   ", cb);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());
        assertTrue(exCap.getValue() instanceof IllegalArgumentException);
        assertEquals("Message cannot be empty", exCap.getValue().getMessage());

        verifyNoInteractions(mockEventDb, mockEntrantDb);
    }

    // ---------------- notifyCancelled: getCancelled failures ----------------

    @Test
    public void notifyCancelled_getCancelledError_bubblesToCallback() {
        NotifyCancelledController.NotifyCancelledCallback cb =
                mock(NotifyCancelledController.NotifyCancelledCallback.class);

        controller.notifyCancelled("E1", "Hello", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cancelCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getCancelled(eq("E1"), cancelCap.capture());

        Exception boom = new RuntimeException("db down");
        cancelCap.getValue().onError(boom);

        verify(cb).onError(boom);
        verifyNoInteractions(mockEntrantDb);
    }

    @Test
    public void notifyCancelled_noCancelledEntrants_reportsError() {
        NotifyCancelledController.NotifyCancelledCallback cb =
                mock(NotifyCancelledController.NotifyCancelledCallback.class);

        controller.notifyCancelled("E1", "Hello", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cancelCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getCancelled(eq("E1"), cancelCap.capture());

        cancelCap.getValue().onSuccess(Collections.emptyList());

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());

        assertTrue(exCap.getValue() instanceof RuntimeException);
        assertEquals("No cancelled entrants found", exCap.getValue().getMessage());
        verifyNoInteractions(mockEntrantDb);
    }

    @Test
    public void notifyCancelled_allNotificationsSuccess_reportsCounts() {
        NotifyCancelledController.NotifyCancelledCallback cb =
                mock(NotifyCancelledController.NotifyCancelledCallback.class);

        controller.notifyCancelled("E1", "Event cancelled", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cancelCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getCancelled(eq("E1"), cancelCap.capture());

        List<Map<String, Object>> cancelled = new ArrayList<>();
        Map<String, Object> c1 = new HashMap<>();
        c1.put("deviceId", "dev1");
        Map<String, Object> c2 = new HashMap<>();
        c2.put("deviceId", "dev2");
        cancelled.add(c1);
        cancelled.add(c2);

        cancelCap.getValue().onSuccess(cancelled);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Boolean>> prefCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb, times(2)).getNotificationPreference(
                anyString(), prefCap.capture()
        );
        
        for (EntrantDB.Callback<Boolean> prefCb : prefCap.getAllValues()) {
            prefCb.onSuccess(true);
        }

        ArgumentCaptor<String> deviceIdCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);

        verify(mockEntrantDb, times(2)).addNotification(
                deviceIdCap.capture(),
                eq("E1"),
                eq("Event cancelled"),
                eq("cancelled_broadcast"),
                notifCap.capture()
        );

        List<String> deviceIds = deviceIdCap.getAllValues();
        assertTrue(deviceIds.contains("dev1"));
        assertTrue(deviceIds.contains("dev2"));

        for (EntrantDB.Callback<Void> cbNotif : notifCap.getAllValues()) {
            cbNotif.onSuccess(null);
        }

        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);

        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(2, notifiedCap.getValue().intValue());
        assertEquals(0, failedCap.getValue().intValue());
    }

    @Test
    public void notifyCancelled_entriesWithoutDeviceId_areCountedAsFailed() {
        NotifyCancelledController.NotifyCancelledCallback cb =
                mock(NotifyCancelledController.NotifyCancelledCallback.class);

        controller.notifyCancelled("E1", "Event cancelled", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cancelCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getCancelled(eq("E1"), cancelCap.capture());

        List<Map<String, Object>> cancelled = new ArrayList<>();
        Map<String, Object> missingDevice = new HashMap<>();
        Map<String, Object> valid = new HashMap<>();
        valid.put("deviceId", "dev1");
        cancelled.add(missingDevice);
        cancelled.add(valid);

        cancelCap.getValue().onSuccess(cancelled);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Boolean>> prefCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getNotificationPreference(
                eq("dev1"), prefCap.capture()
        );
        
        prefCap.getValue().onSuccess(true);

        ArgumentCaptor<String> deviceIdCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);

        verify(mockEntrantDb).addNotification(
                deviceIdCap.capture(),
                eq("E1"),
                eq("Event cancelled"),
                eq("cancelled_broadcast"),
                notifCap.capture()
        );
        assertEquals("dev1", deviceIdCap.getValue());
        notifCap.getValue().onSuccess(null);

        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);

        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(1, notifiedCap.getValue().intValue());
        assertEquals(0, failedCap.getValue().intValue());
    }
}
