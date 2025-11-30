package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Handles retrieval of notification logs for admin review.
 */
public class NotificationLogController {

    public interface NotificationLogCallback {
        void onSuccess(List<Map<String, Object>> logs);
        void onError(@NonNull Exception e);
    }

    private final EntrantDB entrantDB;
    private final EventDB eventDB;

    public NotificationLogController(EntrantDB entrantDB, EventDB eventDB) {
        this.entrantDB = entrantDB;
        this.eventDB = eventDB;
    }

    /**
     * Loads all notification logs with event names resolved.
     *
     * @param callback callback for completion
     */
    public void loadNotificationLogs(NotificationLogCallback callback) {
        entrantDB.getAllNotificationsForAdmin(new EntrantDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> notifications) {
                if (notifications == null || notifications.isEmpty()) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                resolveEventNames(notifications, callback);
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Resolves event names for notifications.
     */
    private void resolveEventNames(List<Map<String, Object>> notifications,
                                   NotificationLogCallback callback) {
        final int total = notifications.size();
        final Map<String, String> eventNameCache = new HashMap<>();
        final EventNameResolver resolver = new EventNameResolver(total, notifications, eventNameCache, callback);

        if (total == 0) {
            callback.onSuccess(notifications);
            return;
        }

        for (Map<String, Object> notification : notifications) {
            Object eventIdObj = notification.get("eventId");
            if (eventIdObj == null) {
                notification.put("eventName", "Unknown Event");
                resolver.onEventResolved();
                continue;
            }

            String eventId = eventIdObj.toString();
            if (eventNameCache.containsKey(eventId)) {
                notification.put("eventName", eventNameCache.get(eventId));
                resolver.onEventResolved();
                continue;
            }

            eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
                @Override
                public void onSuccess(Event event) {
                    String eventName = (event != null && event.getName() != null) ?
                            event.getName() : "Unknown Event";
                    eventNameCache.put(eventId, eventName);
                    notification.put("eventName", eventName);
                    resolver.onEventResolved();
                }

                @Override
                public void onError(@NonNull Exception e) {
                    notification.put("eventName", "Unknown Event");
                    resolver.onEventResolved();
                }
            });
        }
    }

    /**
     * Helper class to track event name resolution completion.
     */
    private static class EventNameResolver {
        private final int total;
        private final List<Map<String, Object>> notifications;
        private final Map<String, String> eventNameCache;
        private final NotificationLogCallback callback;
        private int completed;

        EventNameResolver(int total, List<Map<String, Object>> notifications,
                         Map<String, String> eventNameCache,
                         NotificationLogCallback callback) {
            this.total = total;
            this.notifications = notifications;
            this.eventNameCache = eventNameCache;
            this.callback = callback;
            this.completed = 0;
        }

        synchronized void onEventResolved() {
            completed++;
            if (completed == total) {
                callback.onSuccess(notifications);
            }
        }
    }
}
