package ca.ualberta.codarc.codarc_events;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.controllers.NotifyWaitlistController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;

public class NotifyWaitlistControllerTests {

    private static class Mocks {
        EventDB eventDB = Mockito.mock(EventDB.class);
        EntrantDB entrantDB = Mockito.mock(EntrantDB.class);
        NotifyWaitlistController controller =
                new NotifyWaitlistController(eventDB, entrantDB);
    }

    // ------------- validateMessage tests -------------

    @Test
    public void validateMessage_validMessage_returnsSuccess() {
        Mocks m = new Mocks();

        NotifyWaitlistController.ValidationResult result =
                m.controller.validateMessage("Spots have opened up for this event");

        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    public void validateMessage_nullOrEmpty_returnsFailure() {
        Mocks m = new Mocks();

        NotifyWaitlistController.ValidationResult resNull =
                m.controller.validateMessage(null);
        NotifyWaitlistController.ValidationResult resEmpty =
                m.controller.validateMessage("   ");

        assertFalse(resNull.isValid());
        assertEquals("Message cannot be empty", resNull.getErrorMessage());

        assertFalse(resEmpty.isValid());
        assertEquals("Message cannot be empty", resEmpty.getErrorMessage());
    }

    @Test
    public void validateMessage_tooLong_returnsFailure() {
        Mocks m = new Mocks();

        // 501 characters
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            sb.append("x");
        }

        NotifyWaitlistController.ValidationResult result =
                m.controller.validateMessage(sb.toString());

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("500"));
    }

    // ------------- notifyWaitlist tests -------------

    @Test
    public void notifyWaitlist_emptyEventId_triggersOnError() {
        Mocks m = new Mocks();

        final boolean[] errorCalled = {false};
        final Exception[] errorHolder = {null};

        NotifyWaitlistController.NotifyWaitlistCallback callback =
                new NotifyWaitlistController.NotifyWaitlistCallback() {
                    @Override
                    public void onSuccess(int notifiedCount, int failedCount) {
                        // should not be called
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception e) {
                        errorCalled[0] = true;
                        errorHolder[0] = e;
                    }
                };

        m.controller.notifyWaitlist("", "msg", callback);

        assertTrue(errorCalled[0]);
        assertTrue(errorHolder[0] instanceof IllegalArgumentException);
        assertEquals("eventId is empty", errorHolder[0].getMessage());

        Mockito.verifyNoInteractions(m.eventDB);
        Mockito.verifyNoInteractions(m.entrantDB);
    }

    @Test
    public void notifyWaitlist_invalidMessage_triggersOnError() {
        Mocks m = new Mocks();

        final boolean[] errorCalled = {false};
        final Exception[] errorHolder = {null};

        NotifyWaitlistController.NotifyWaitlistCallback callback =
                new NotifyWaitlistController.NotifyWaitlistCallback() {
                    @Override
                    public void onSuccess(int notifiedCount, int failedCount) {
                        // should not be called
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception e) {
                        errorCalled[0] = true;
                        errorHolder[0] = e;
                    }
                };

        m.controller.notifyWaitlist("EVENT1", "   ", callback);

        assertTrue(errorCalled[0]);
        assertTrue(errorHolder[0] instanceof IllegalArgumentException);
        assertEquals("Message cannot be empty", errorHolder[0].getMessage());

        Mockito.verifyNoInteractions(m.eventDB);
        Mockito.verifyNoInteractions(m.entrantDB);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void notifyWaitlist_emptyWaitlist_triggersOnError() {
        Mocks m = new Mocks();

        // Stub eventDB.getWaitlist to return an empty list
        Mockito.doAnswer(invocation -> {
            EventDB.Callback<List<Map<String, Object>>> cb =
                    (EventDB.Callback<List<Map<String, Object>>>) invocation.getArguments()[1];
            cb.onSuccess(new ArrayList<>()); // empty waitlist
            return null;
        }).when(m.eventDB).getWaitlist(eq("EVENT1"), any(EventDB.Callback.class));

        final boolean[] errorCalled = {false};
        final Exception[] errorHolder = {null};

        NotifyWaitlistController.NotifyWaitlistCallback callback =
                new NotifyWaitlistController.NotifyWaitlistCallback() {
                    @Override
                    public void onSuccess(int notifiedCount, int failedCount) {
                        // should not be called
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception e) {
                        errorCalled[0] = true;
                        errorHolder[0] = e;
                    }
                };

        m.controller.notifyWaitlist("EVENT1", "Waitlist update", callback);

        assertTrue(errorCalled[0]);
        assertTrue(errorHolder[0] instanceof RuntimeException);
        assertEquals("Waitlist is empty", errorHolder[0].getMessage());

        Mockito.verify(m.eventDB).getWaitlist(eq("EVENT1"), any(EventDB.Callback.class));
        Mockito.verifyNoInteractions(m.entrantDB);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void notifyWaitlist_success_notifiesAllWaitlistedEntrants() {
        Mocks m = new Mocks();

        // Build a fake waitlist with one deviceId
        List<Map<String, Object>> waitlist = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("deviceId", "device123");
        waitlist.add(entry);

        // Stub getWaitlist to return our list
        Mockito.doAnswer(invocation -> {
            EventDB.Callback<List<Map<String, Object>>> cb =
                    (EventDB.Callback<List<Map<String, Object>>>) invocation.getArguments()[1];
            cb.onSuccess(waitlist);
            return null;
        }).when(m.eventDB).getWaitlist(eq("EVENT1"), any(EventDB.Callback.class));

        // Stub addNotification to immediately succeed
        Mockito.doAnswer(invocation -> {
            EntrantDB.Callback<Void> cb =
                    (EntrantDB.Callback<Void>) invocation.getArguments()[4];
            cb.onSuccess(null);
            return null;
        }).when(m.entrantDB).addNotification(
                eq("device123"),
                eq("EVENT1"),
                anyString(),
                anyString(),
                any(EntrantDB.Callback.class)
        );

        final boolean[] errorCalled = {false};
        final int[] notifiedHolder = { -1 };
        final int[] failedHolder = { -1 };

        NotifyWaitlistController.NotifyWaitlistCallback callback =
                new NotifyWaitlistController.NotifyWaitlistCallback() {
                    @Override
                    public void onSuccess(int notifiedCount, int failedCount) {
                        notifiedHolder[0] = notifiedCount;
                        failedHolder[0] = failedCount;
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception e) {
                        errorCalled[0] = true;
                    }
                };

        m.controller.notifyWaitlist("EVENT1", "A spot has opened up", callback);

        assertFalse(errorCalled[0]);
        assertEquals(1, notifiedHolder[0]);
        assertEquals(0, failedHolder[0]);

        Mockito.verify(m.eventDB).getWaitlist(eq("EVENT1"), any(EventDB.Callback.class));
        Mockito.verify(m.entrantDB).addNotification(
                eq("device123"),
                eq("EVENT1"),
                anyString(),
                anyString(),
                any(EntrantDB.Callback.class)
        );
    }
}
