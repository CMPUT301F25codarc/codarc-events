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

import ca.ualberta.codarc.codarc_events.controllers.NotifyWinnersController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;

public class NotifyWinnersControllerTests {

    private static class Mocks {
        EventDB eventDB = Mockito.mock(EventDB.class);
        EntrantDB entrantDB = Mockito.mock(EntrantDB.class);
        NotifyWinnersController controller =
                new NotifyWinnersController(eventDB, entrantDB);
    }

    // ------------- validateMessage tests -------------

    @Test
    public void validateMessage_validMessage_returnsSuccess() {
        Mocks m = new Mocks();

        NotifyWinnersController.ValidationResult result =
                m.controller.validateMessage("Congrats, you have been selected!");

        assertTrue(result.isValid());
        assertNull(result.getErrorMessage());
    }

    @Test
    public void validateMessage_nullOrEmpty_returnsFailure() {
        Mocks m = new Mocks();

        NotifyWinnersController.ValidationResult resNull =
                m.controller.validateMessage(null);
        NotifyWinnersController.ValidationResult resEmpty =
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

        NotifyWinnersController.ValidationResult result =
                m.controller.validateMessage(sb.toString());

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("500"));
    }

    // ------------- notifyWinners tests -------------

    @Test
    public void notifyWinners_emptyEventId_triggersOnError() {
        Mocks m = new Mocks();

        final boolean[] errorCalled = { false };
        final Exception[] errorHolder = { null };

        NotifyWinnersController.NotifyWinnersCallback callback =
                new NotifyWinnersController.NotifyWinnersCallback() {
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

        m.controller.notifyWinners("", "msg", callback);

        assertTrue(errorCalled[0]);
        assertTrue(errorHolder[0] instanceof IllegalArgumentException);
        assertEquals("eventId is empty", errorHolder[0].getMessage());

        Mockito.verifyNoInteractions(m.eventDB);
        Mockito.verifyNoInteractions(m.entrantDB);
    }

    @Test
    public void notifyWinners_invalidMessage_triggersOnError() {
        Mocks m = new Mocks();

        final boolean[] errorCalled = { false };
        final Exception[] errorHolder = { null };

        NotifyWinnersController.NotifyWinnersCallback callback =
                new NotifyWinnersController.NotifyWinnersCallback() {
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

        m.controller.notifyWinners("EVENT1", "   ", callback);

        assertTrue(errorCalled[0]);
        assertTrue(errorHolder[0] instanceof IllegalArgumentException);
        assertEquals("Message cannot be empty", errorHolder[0].getMessage());

        Mockito.verifyNoInteractions(m.eventDB);
        Mockito.verifyNoInteractions(m.entrantDB);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void notifyWinners_noWinners_triggersOnError() {
        Mocks m = new Mocks();

        // Stub eventDB.getWinners to return an empty list
        Mockito.doAnswer(invocation -> {
            EventDB.Callback<List<Map<String, Object>>> cb =
                    (EventDB.Callback<List<Map<String, Object>>>) invocation.getArguments()[1];
            cb.onSuccess(new ArrayList<>()); // empty winners list
            return null;
        }).when(m.eventDB).getWinners(eq("EVENT1"), any(EventDB.Callback.class));

        final boolean[] errorCalled = { false };
        final Exception[] errorHolder = { null };

        NotifyWinnersController.NotifyWinnersCallback callback =
                new NotifyWinnersController.NotifyWinnersCallback() {
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

        m.controller.notifyWinners("EVENT1", "You have been selected", callback);

        assertTrue(errorCalled[0]);
        assertTrue(errorHolder[0] instanceof RuntimeException);
        assertEquals("No winners selected", errorHolder[0].getMessage());

        Mockito.verify(m.eventDB).getWinners(eq("EVENT1"), any(EventDB.Callback.class));
        Mockito.verifyNoInteractions(m.entrantDB);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void notifyWinners_success_notifiesAllWinners() {
        Mocks m = new Mocks();

        // Build fake winners list with one deviceId
        List<Map<String, Object>> winners = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("deviceId", "device123");
        winners.add(entry);

        // Stub getWinners to return our list
        Mockito.doAnswer(invocation -> {
            EventDB.Callback<List<Map<String, Object>>> cb =
                    (EventDB.Callback<List<Map<String, Object>>>) invocation.getArguments()[1];
            cb.onSuccess(winners);
            return null;
        }).when(m.eventDB).getWinners(eq("EVENT1"), any(EventDB.Callback.class));

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

        final boolean[] errorCalled = { false };
        final int[] notifiedHolder = { -1 };
        final int[] failedHolder = { -1 };

        NotifyWinnersController.NotifyWinnersCallback callback =
                new NotifyWinnersController.NotifyWinnersCallback() {
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

        m.controller.notifyWinners("EVENT1", "You have been selected!", callback);

        assertFalse(errorCalled[0]);
        assertEquals(1, notifiedHolder[0]);
        assertEquals(0, failedHolder[0]);

        Mockito.verify(m.eventDB).getWinners(eq("EVENT1"), any(EventDB.Callback.class));
        Mockito.verify(m.entrantDB).addNotification(
                eq("device123"),
                eq("EVENT1"),
                anyString(),
                anyString(),
                any(EntrantDB.Callback.class)
        );
    }
}
