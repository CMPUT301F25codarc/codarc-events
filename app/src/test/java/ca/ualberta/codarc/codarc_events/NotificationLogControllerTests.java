package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.NotificationLogController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
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

public class NotificationLogControllerTests {

    private EntrantDB mockEntrantDb;
    private EventDB mockEventDb;
    private NotificationLogController controller;

    private static final String EVENT_ID = "event-123";
    private static final String DEVICE_ID = "dev-456";

    @Before
    public void setUp() {
        mockEntrantDb = mock(EntrantDB.class);
        mockEventDb = mock(EventDB.class);
        controller = new NotificationLogController(mockEntrantDb, mockEventDb);
    }

    @Test
    public void loadNotificationLogs_emptyList_returnsEmpty() {
        NotificationLogController.NotificationLogCallback cb =
                mock(NotificationLogController.NotificationLogCallback.class);

        controller.loadNotificationLogs(cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getAllNotificationsForAdmin(cap.capture());

        cap.getValue().onSuccess(new ArrayList<>());

        ArgumentCaptor<List<Map<String, Object>>> logsCap =
                ArgumentCaptor.forClass(List.class);
        verify(cb).onSuccess(logsCap.capture());

        assertTrue(logsCap.getValue().isEmpty());
    }

    @Test
    public void loadNotificationLogs_success_resolvesEventNames() {
        NotificationLogController.NotificationLogCallback cb =
                mock(NotificationLogController.NotificationLogCallback.class);

        controller.loadNotificationLogs(cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getAllNotificationsForAdmin(cap.capture());

        List<Map<String, Object>> notifications = new ArrayList<>();
        Map<String, Object> notif = new HashMap<>();
        notif.put("eventId", EVENT_ID);
        notif.put("deviceId", DEVICE_ID);
        notifications.add(notif);
        cap.getValue().onSuccess(notifications);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq(EVENT_ID), eventCap.capture());

        Event event = new Event();
        event.setName("Test Event");
        eventCap.getValue().onSuccess(event);

        ArgumentCaptor<List<Map<String, Object>>> logsCap =
                ArgumentCaptor.forClass(List.class);
        verify(cb).onSuccess(logsCap.capture());

        List<Map<String, Object>> logs = logsCap.getValue();
        assertEquals(1, logs.size());
        assertEquals("Test Event", logs.get(0).get("eventName"));
    }

    @Test
    public void loadNotificationLogs_nullEventId_usesUnknownEvent() {
        NotificationLogController.NotificationLogCallback cb =
                mock(NotificationLogController.NotificationLogCallback.class);

        controller.loadNotificationLogs(cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getAllNotificationsForAdmin(cap.capture());

        List<Map<String, Object>> notifications = new ArrayList<>();
        Map<String, Object> notif = new HashMap<>();
        notif.put("eventId", null);
        notifications.add(notif);
        cap.getValue().onSuccess(notifications);

        verify(mockEventDb, never()).getEvent(anyString(), any());

        ArgumentCaptor<List<Map<String, Object>>> logsCap =
                ArgumentCaptor.forClass(List.class);
        verify(cb).onSuccess(logsCap.capture());

        List<Map<String, Object>> logs = logsCap.getValue();
        assertEquals(1, logs.size());
        assertEquals("Unknown Event", logs.get(0).get("eventName"));
    }

    @Test
    public void loadNotificationLogs_getNotificationsError_propagates() {
        NotificationLogController.NotificationLogCallback cb =
                mock(NotificationLogController.NotificationLogCallback.class);

        controller.loadNotificationLogs(cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getAllNotificationsForAdmin(cap.capture());

        Exception error = new RuntimeException("DB error");
        cap.getValue().onError(error);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());
        assertSame(error, exCap.getValue());
    }
}
