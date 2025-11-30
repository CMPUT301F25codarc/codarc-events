package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.NotifyWaitlistController;
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

public class NotifyWaitlistControllerTests {

    private EventDB mockEventDb;
    private EntrantDB mockEntrantDb;
    private NotifyWaitlistController controller;

    @Before
    public void setUp() {
        mockEventDb = mock(EventDB.class);
        mockEntrantDb = mock(EntrantDB.class);
        controller = new NotifyWaitlistController(mockEventDb, mockEntrantDb);
    }

    // ---------------- validateMessage ----------------

    @Test
    public void validateMessage_nullOrEmpty_fails() {
        // null
        NotifyWaitlistController.ValidationResult res1 = controller.validateMessage(null);
        assertFalse(res1.isValid());
        assertEquals("Message cannot be empty", res1.getErrorMessage());

        // empty / whitespace
        NotifyWaitlistController.ValidationResult res2 = controller.validateMessage("   ");
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

        NotifyWaitlistController.ValidationResult res = controller.validateMessage(longMessage);
        assertFalse(res.isValid());
        assertEquals("Message cannot exceed 500 characters", res.getErrorMessage());
    }

    @Test
    public void validateMessage_valid_succeeds() {
        NotifyWaitlistController.ValidationResult res = controller.validateMessage("All good");
        assertTrue(res.isValid());
        assertNull(res.getErrorMessage());
    }

    // ---------------- notifyWaitlist: basic validation ----------------

    @Test
    public void notifyWaitlist_emptyEventId_errorsFast() {
        NotifyWaitlistController.NotifyWaitlistCallback cb =
                mock(NotifyWaitlistController.NotifyWaitlistCallback.class);

        controller.notifyWaitlist("", "Hello", cb);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());
        assertTrue(exCap.getValue() instanceof IllegalArgumentException);
        assertEquals("eventId is empty", exCap.getValue().getMessage());

        verifyNoInteractions(mockEventDb, mockEntrantDb);
    }

    @Test
    public void notifyWaitlist_invalidMessage_usesValidationError() {
        NotifyWaitlistController.NotifyWaitlistCallback cb =
                mock(NotifyWaitlistController.NotifyWaitlistCallback.class);

        controller.notifyWaitlist("E1", "   ", cb);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());
        assertTrue(exCap.getValue() instanceof IllegalArgumentException);
        assertEquals("Message cannot be empty", exCap.getValue().getMessage());

        verifyNoInteractions(mockEventDb, mockEntrantDb);
    }

    // ---------------- notifyWaitlist: getWaitlist failures ----------------

    @Test
    public void notifyWaitlist_getWaitlistError_bubblesToCallback() {
        NotifyWaitlistController.NotifyWaitlistCallback cb =
                mock(NotifyWaitlistController.NotifyWaitlistCallback.class);

        controller.notifyWaitlist("E1", "Hello", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> waitCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E1"), waitCap.capture());

        Exception boom = new RuntimeException("db down");
        waitCap.getValue().onError(boom);

        verify(cb).onError(boom);
        verifyNoInteractions(mockEntrantDb);
    }

    @Test
    public void notifyWaitlist_emptyWaitlist_reportsError() {
        NotifyWaitlistController.NotifyWaitlistCallback cb =
                mock(NotifyWaitlistController.NotifyWaitlistCallback.class);

        controller.notifyWaitlist("E1", "Hello", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> waitCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E1"), waitCap.capture());

        // simulate empty waitlist
        waitCap.getValue().onSuccess(Collections.emptyList());

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());

        assertTrue(exCap.getValue() instanceof RuntimeException);
        assertEquals("Waitlist is empty", exCap.getValue().getMessage());
        verifyNoInteractions(mockEntrantDb);
    }

    // ---------------- notifyWaitlist: happy path ----------------

    @Test
    public void notifyWaitlist_allNotificationsSuccess_reportsCounts() {
        NotifyWaitlistController.NotifyWaitlistCallback cb =
                mock(NotifyWaitlistController.NotifyWaitlistCallback.class);

        controller.notifyWaitlist("E1", "Waitlist message", cb);

        // 1) eventDB.getWaitlist -> return two waitlist entries
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> waitCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E1"), waitCap.capture());

        List<Map<String, Object>> waitlist = new ArrayList<>();
        Map<String, Object> w1 = new HashMap<>();
        w1.put("deviceId", "dev1");
        Map<String, Object> w2 = new HashMap<>();
        w2.put("deviceId", "dev2");
        waitlist.add(w1);
        waitlist.add(w2);

        waitCap.getValue().onSuccess(waitlist);

        // 2) verify notifications are sent for each device
        ArgumentCaptor<String> deviceIdCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);

        verify(mockEntrantDb, times(2)).addNotification(
                deviceIdCap.capture(),
                eq("E1"),
                eq("Waitlist message"),
                eq("waitlist_broadcast"),
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

    // ---------------- notifyWaitlist: null deviceId entries ----------------

    @Test
    public void notifyWaitlist_entriesWithoutDeviceId_areCountedAsFailed() {
        NotifyWaitlistController.NotifyWaitlistCallback cb =
                mock(NotifyWaitlistController.NotifyWaitlistCallback.class);

        controller.notifyWaitlist("E1", "Waitlist message", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> waitCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E1"), waitCap.capture());

        // one entry missing deviceId, one valid
        List<Map<String, Object>> waitlist = new ArrayList<>();
        Map<String, Object> missingDevice = new HashMap<>();
        Map<String, Object> valid = new HashMap<>();
        valid.put("deviceId", "dev1");
        waitlist.add(missingDevice);
        waitlist.add(valid);

        waitCap.getValue().onSuccess(waitlist);

        // valid entry still triggers addNotification once
        ArgumentCaptor<String> deviceIdCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);

        verify(mockEntrantDb).addNotification(
                deviceIdCap.capture(),
                eq("E1"),
                eq("Waitlist message"),
                eq("waitlist_broadcast"),
                notifCap.capture()
        );
        assertEquals("dev1", deviceIdCap.getValue());

        // The controller calls the callback once when the null device entry is processed.
        // At that moment completed = 1 and failed = 1.
        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);

        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(1, notifiedCap.getValue().intValue());
        assertEquals(1, failedCap.getValue().intValue());
    }
}
