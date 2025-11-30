package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.NotifyWinnersController;
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

public class NotifyWinnersControllerTests {

    private EventDB mockEventDb;
    private EntrantDB mockEntrantDb;
    private NotifyWinnersController controller;

    @Before
    public void setUp() {
        mockEventDb = mock(EventDB.class);
        mockEntrantDb = mock(EntrantDB.class);
        controller = new NotifyWinnersController(mockEventDb, mockEntrantDb);
    }

    @Test
    public void validateMessage_nullOrEmpty_fails() {
        NotifyWinnersController.ValidationResult res1 = controller.validateMessage(null);
        assertFalse(res1.isValid());
        assertEquals("Message cannot be empty", res1.getErrorMessage());

        NotifyWinnersController.ValidationResult res2 = controller.validateMessage("   ");
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

        NotifyWinnersController.ValidationResult res = controller.validateMessage(longMsg);
        assertFalse(res.isValid());
        assertEquals("Message cannot exceed 500 characters", res.getErrorMessage());
    }

    @Test
    public void validateMessage_valid_succeeds() {
        NotifyWinnersController.ValidationResult res = controller.validateMessage("Congrats, you won!");
        assertTrue(res.isValid());
        assertNull(res.getErrorMessage());
    }

    @Test
    public void notifyWinners_emptyEventId_errorsFast() {
        NotifyWinnersController.NotifyWinnersCallback cb =
                mock(NotifyWinnersController.NotifyWinnersCallback.class);

        controller.notifyWinners("", "Hello", cb);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());

        assertTrue(exCap.getValue() instanceof IllegalArgumentException);
        assertEquals("eventId cannot be null or empty", exCap.getValue().getMessage());

        verifyNoInteractions(mockEventDb, mockEntrantDb);
    }

    @Test
    public void notifyWinners_invalidMessage_usesValidationError() {
        NotifyWinnersController.NotifyWinnersCallback cb =
                mock(NotifyWinnersController.NotifyWinnersCallback.class);

        controller.notifyWinners("E1", "   ", cb);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());

        assertTrue(exCap.getValue() instanceof IllegalArgumentException);
        assertEquals("Message cannot be empty", exCap.getValue().getMessage());

        verifyNoInteractions(mockEventDb, mockEntrantDb);
    }

    @Test
    public void notifyWinners_getWinnersError_bubblesToCallback() {
        NotifyWinnersController.NotifyWinnersCallback cb =
                mock(NotifyWinnersController.NotifyWinnersCallback.class);

        controller.notifyWinners("E1", "Hello winners", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> winnersCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWinners(eq("E1"), winnersCap.capture());

        Exception boom = new RuntimeException("db down");
        winnersCap.getValue().onError(boom);

        verify(cb).onError(boom);
        verifyNoInteractions(mockEntrantDb);
    }

    @Test
    public void notifyWinners_noWinners_reportsError() {
        NotifyWinnersController.NotifyWinnersCallback cb =
                mock(NotifyWinnersController.NotifyWinnersCallback.class);

        controller.notifyWinners("E1", "Hello winners", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> winnersCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWinners(eq("E1"), winnersCap.capture());

        winnersCap.getValue().onSuccess(Collections.emptyList());

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());

        assertTrue(exCap.getValue() instanceof RuntimeException);
        assertEquals("No winners selected", exCap.getValue().getMessage());
        verifyNoInteractions(mockEntrantDb);
    }

    @Test
    public void notifyWinners_allNotificationsSuccess_reportsCounts() {
        NotifyWinnersController.NotifyWinnersCallback cb =
                mock(NotifyWinnersController.NotifyWinnersCallback.class);

        controller.notifyWinners("E1", "You won!", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> winnersCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWinners(eq("E1"), winnersCap.capture());

        List<Map<String, Object>> winners = new ArrayList<>();
        Map<String, Object> w1 = new HashMap<>();
        w1.put("deviceId", "dev1");
        Map<String, Object> w2 = new HashMap<>();
        w2.put("deviceId", "dev2");
        winners.add(w1);
        winners.add(w2);

        winnersCap.getValue().onSuccess(winners);

        ArgumentCaptor<String> deviceIdCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);

        verify(mockEntrantDb, times(2)).addNotification(
                deviceIdCap.capture(),
                eq("E1"),
                eq("You won!"),
                eq("winners_broadcast"),
                notifCap.capture()
        );

        List<String> deviceIds = deviceIdCap.getAllValues();
        assertEquals(2, deviceIds.size());
        assertTrue(deviceIds.contains("dev1"));
        assertTrue(deviceIds.contains("dev2"));

        List<EntrantDB.Callback<Void>> callbacks = notifCap.getAllValues();
        callbacks.get(0).onSuccess(null);
        callbacks.get(1).onSuccess(null);
        
        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);

        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(2, notifiedCap.getValue().intValue());
        assertEquals(0, failedCap.getValue().intValue());
    }

    @Test
    public void notifyWinners_entriesWithoutDeviceId_areCountedAsFailed() {
        NotifyWinnersController.NotifyWinnersCallback cb =
                mock(NotifyWinnersController.NotifyWinnersCallback.class);

        controller.notifyWinners("E1", "You won!", cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> winnersCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWinners(eq("E1"), winnersCap.capture());

        List<Map<String, Object>> winners = new ArrayList<>();
        Map<String, Object> missing = new HashMap<>();
        Map<String, Object> valid = new HashMap<>();
        valid.put("deviceId", "dev1");
        winners.add(missing);
        winners.add(valid);

        winnersCap.getValue().onSuccess(winners);

        ArgumentCaptor<String> deviceIdCap = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntrantDB.Callback<Void>> notifCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);

        verify(mockEntrantDb).addNotification(
                deviceIdCap.capture(),
                eq("E1"),
                eq("You won!"),
                eq("winners_broadcast"),
                notifCap.capture()
        );
        
        assertEquals("dev1", deviceIdCap.getValue());
        notifCap.getValue().onSuccess(null);

        ArgumentCaptor<Integer> notifiedCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> failedCap = ArgumentCaptor.forClass(Integer.class);

        verify(cb).onSuccess(notifiedCap.capture(), failedCap.capture());
        assertEquals(1, notifiedCap.getValue().intValue());
        assertEquals(1, failedCap.getValue().intValue());
    }
}
