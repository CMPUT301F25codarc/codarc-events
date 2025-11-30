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

    // ---------------- validateMessage ----------------

    @Test
    public void validateMessage_nullOrEmpty_fails() {
        // null
        NotifyCancelledController.ValidationResult res1 = controller.validateMessage(null);
        assertFalse(res1.isValid());
        assertEquals("Message cannot be empty", res1.getErrorMessage());

        // empty / whitespace
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

    // ---------------- notifyCancelled: basic validation ----------------

    @Test
    public void notifyCancelled_emptyEventId_errorsFast() {
        NotifyCancelledController.NotifyCancelledCallback cb =
                mock(NotifyCancelledController.NotifyCancelledCallback.class);

        controller.notifyCancelled("", "Hello", cb);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());
        assertTrue(exCap.getValue() instanceof IllegalArgumentException);
        assertEquals("eventId is empty", exCap.getValue().getMessage());

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

        // simulate no cancelled entrants
        cancelCap.getValue().onSuccess(Collections.emptyList());

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());

        assertTrue(exCap.getValue() instanceof RuntimeException);
        assertEquals("No cancelled entrants found", exCap.getValue().getMessage());
        verifyNoInteractions(mockEntrantDb);
    }

    // ---------------- notifyCancelled: happy path ----------------

    @Test
    public void notifyCancelled_allNotificationsSuccess_reportsCounts() {
        NotifyCancelledController.NotifyCancelledCallback cb =
                mock(NotifyCancelledController.NotifyCancelledCallback.class);

        controller.notifyCancelled("E1", "Event cancelled", cb);

        // 1) eventDB.getCancelled -> return two cancelled entries
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

        // 2) verify notifications are sent for each device
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

        // 3) simulate both notifications succeeding
        for (EntrantDB.Callback<Void> cbNotif : notifCap.getAllValues()) {
            cbNotif.onSuccess(null);
        }

        // 4) controller callback should report notifiedCount = 2, failedCount = 0
        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);

        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(2, notifiedCap.getValue().intValue());
        assertEquals(0, failedCap.getValue().intValue());
    }

    // ---------------- notifyCancelled: null deviceId entries ----------------

    @Test
    public void notifyCancelled_entriesWithoutDeviceId_areCountedAsFailed() {
        NotifyCancelledController.NotifyCancelledCallback cb =
                mock(NotifyCancelledController.NotifyCancelledCallback.class);

        controller.notifyCancelled("E1", "Event cancelled", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cancelCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getCancelled(eq("E1"), cancelCap.capture());

        // one entry missing deviceId, one valid
        List<Map<String, Object>> cancelled = new ArrayList<>();
        Map<String, Object> missingDevice = new HashMap<>();
        Map<String, Object> valid = new HashMap<>();
        valid.put("deviceId", "dev1");
        cancelled.add(missingDevice);
        cancelled.add(valid);

        cancelCap.getValue().onSuccess(cancelled);

        // the code immediately counts the null deviceId entry as failed and completed,
        // then proceeds to call addNotification for the valid one
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

        // we do not need to trigger the notif callback for this test,
        // because the controller already called NotifyCancelledCallback based on the null entry

        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);

        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        // according to the current implementation, the null entry increments both completed and failed
        assertEquals(1, notifiedCap.getValue().intValue());
        assertEquals(1, failedCap.getValue().intValue());
    }
}
