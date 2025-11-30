package ca.ualberta.codarc.codarc_events.controllers;

import android.util.Log;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.utils.FCMHelper;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Unified controller for sending broadcast notifications to different groups of entrants.
 * Supports waitlist, cancelled, winners, and enrolled notification categories.
 */
public class NotificationController {

    /**
     * Notification category types.
     */
    public enum NotificationCategory {
        WAITLIST("waitlist_broadcast"),
        CANCELLED("cancelled_broadcast"),
        WINNERS("winners_broadcast"),
        ENROLLED("enrolled_broadcast");

        private final String categoryValue;

        NotificationCategory(String categoryValue) {
            this.categoryValue = categoryValue;
        }

        public String getCategoryValue() {
            return categoryValue;
        }
    }

    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;

        private ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Callback interface for notification sending operation.
     */
    public interface NotificationCallback {
        void onSuccess(int notifiedCount, int failedCount);
        void onError(@NonNull Exception e);
    }

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final String TAG = "NotificationController";

    private final EventDB eventDB;
    private final EntrantDB entrantDB;
    private final FCMHelper fcmHelper;

    /**
     * Creates a new NotificationController.
     *
     * @param eventDB EventDB instance
     * @param entrantDB EntrantDB instance
     */
    public NotificationController(EventDB eventDB, EntrantDB entrantDB) {
        this.eventDB = eventDB;
        this.entrantDB = entrantDB;
        this.fcmHelper = null;
    }

    /**
     * Creates a new NotificationController with FCM support.
     *
     * @param eventDB EventDB instance
     * @param entrantDB EntrantDB instance
     * @param fcmHelper FCMHelper instance (can be null)
     */
    public NotificationController(EventDB eventDB, EntrantDB entrantDB, FCMHelper fcmHelper) {
        this.eventDB = eventDB;
        this.entrantDB = entrantDB;
        this.fcmHelper = fcmHelper;
    }

    /**
     * Validates the notification message.
     *
     * @param message the message to validate
     * @return ValidationResult indicating if message is valid
     */
    public ValidationResult validateMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return ValidationResult.failure("Message cannot be empty");
        }

        if (message.length() > MAX_MESSAGE_LENGTH) {
            return ValidationResult.failure("Message cannot exceed " + MAX_MESSAGE_LENGTH + " characters");
        }

        return ValidationResult.success();
    }

    /**
     * Sends notifications to a group of entrants based on the category.
     *
     * @param eventId the event ID
     * @param message the notification message
     * @param category the notification category (WAITLIST, CANCELLED, or WINNERS)
     * @param emptyListErrorMessage error message to show if the list is empty
     * @param callback callback for operation completion
     */
    public void notifyUsers(String eventId, String message, NotificationCategory category,
                           String emptyListErrorMessage, NotificationCallback callback) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            callback.onError(e);
            return;
        }

        ValidationResult validation = validateMessage(message);
        if (!validation.isValid()) {
            callback.onError(new IllegalArgumentException(validation.getErrorMessage()));
            return;
        }

        getEntrantList(eventId, category, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> entrants) {
                if (entrants == null || entrants.isEmpty()) {
                    callback.onError(new RuntimeException(emptyListErrorMessage));
                    return;
                }

                sendNotifications(eventId, message, category.getCategoryValue(), entrants, callback);
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Gets the appropriate entrant list based on category.
     */
    private void getEntrantList(String eventId, NotificationCategory category,
                               EventDB.Callback<List<Map<String, Object>>> callback) {
        switch (category) {
            case WAITLIST:
                eventDB.getWaitlist(eventId, callback);
                break;
            case CANCELLED:
                eventDB.getCancelled(eventId, callback);
                break;
            case WINNERS:
                eventDB.getWinners(eventId, callback);
                break;
            case ENROLLED:
                eventDB.getEnrolled(eventId, callback);
                break;
            default:
                callback.onError(new IllegalArgumentException("Unknown notification category"));
        }
    }

    /**
     * Sends notifications to each entrant in the list.
     * Filters entrants based on notification preference (except for "winner" category).
     *
     * @param eventId the event ID
     * @param message the notification message
     * @param categoryValue the notification category value
     * @param entrants list of entrant entries
     * @param callback callback for operation completion
     */
    private void sendNotifications(String eventId, String message, String categoryValue,
                                   List<Map<String, Object>> entrants,
                                   NotificationCallback callback) {
        final int total = entrants.size();

        if (total == 0) {
            callback.onSuccess(0, 0);
            return;
        }

        boolean isWinnerCategory = "winner".equals(categoryValue);
        
        if (isWinnerCategory) {
            sendNotificationsToFilteredList(eventId, message, categoryValue, entrants, callback);
        } else {
            filterByPreferenceAndSend(eventId, message, categoryValue, entrants, callback);
        }
    }

    /**
     * Filters entrants by notification preference and sends notifications.
     */
    private void filterByPreferenceAndSend(String eventId, String message, String categoryValue,
                                           List<Map<String, Object>> entrants,
                                           NotificationCallback callback) {
        final int total = entrants.size();
        final List<String> enabledDeviceIds = Collections.synchronizedList(new ArrayList<>());
        final PreferenceCheckAggregator aggregator = new PreferenceCheckAggregator(total, enabledDeviceIds, () -> {
            List<Map<String, Object>> filteredEntrants = new ArrayList<>();
            for (Map<String, Object> entry : entrants) {
                Object deviceIdObj = entry.get("deviceId");
                if (deviceIdObj != null && enabledDeviceIds.contains(deviceIdObj.toString())) {
                    filteredEntrants.add(entry);
                }
            }
            sendNotificationsToFilteredList(eventId, message, categoryValue, filteredEntrants, callback);
        });

        for (Map<String, Object> entry : entrants) {
            Object deviceIdObj = entry.get("deviceId");
            if (deviceIdObj == null) {
                aggregator.onPreferenceChecked(null, false);
                continue;
            }

            String deviceId = deviceIdObj.toString();
            entrantDB.getNotificationPreference(deviceId, new EntrantDB.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean enabled) {
                    aggregator.onPreferenceChecked(deviceId, enabled != null && enabled);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    Log.d(TAG, "Failed to get notification preference for " + deviceId, e);
                    aggregator.onPreferenceChecked(deviceId, true);
                }
            });
        }
    }

    /**
     * Sends notifications to a filtered list of entrants.
     */
    private void sendNotificationsToFilteredList(String eventId, String message, String categoryValue,
                                                List<Map<String, Object>> entrants,
                                                NotificationCallback callback) {
        final int total = entrants.size();

        if (total == 0) {
            callback.onSuccess(0, 0);
            return;
        }

        final NotificationSender sender = new NotificationSender(total, callback);

        for (Map<String, Object> entry : entrants) {
            Object deviceIdObj = entry.get("deviceId");
            if (deviceIdObj == null) {
                sender.onError();
                continue;
            }

            String deviceId = deviceIdObj.toString();
            entrantDB.addNotification(deviceId, eventId, message, categoryValue,
                    new EntrantDB.Callback<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                            sender.onSuccess();
                        }

                        @Override
                        public void onError(@NonNull Exception e) {
                            Log.e(TAG, "Failed to send notification to " + deviceId, e);
                            sender.onError();
                        }
                    });
        }

        if (fcmHelper != null) {
            sendFCMPushNotifications(entrants, eventId, message, categoryValue);
        }
    }

    /**
     * Sends FCM push notifications to entrants.
     * Fire-and-forget: errors are logged but don't affect Firestore notification saving.
     * 
     * Note: The FCM token fetching and aggregation logic (fetchTokensAndSend and
     * TokenFetchAggregator) was implemented with assistance from Claude Sonnet 4.5 (Anthropic).
     * The thread-safe token collection pattern using Collections.synchronizedList() and the
     * aggregator helper class for coordinating multiple async token fetches were developed
     * with LLM assistance.
     */
    private void sendFCMPushNotifications(List<Map<String, Object>> entrants,
                                         String eventId, String message,
                                         String categoryValue) {
        List<String> deviceIds = new ArrayList<>();
        for (Map<String, Object> entry : entrants) {
            Object deviceIdObj = entry.get("deviceId");
            if (deviceIdObj != null) {
                deviceIds.add(deviceIdObj.toString());
            }
        }

        if (deviceIds.isEmpty()) {
            return;
        }

        fetchTokensAndSend(deviceIds, eventId, message, categoryValue);
    }

    private void fetchTokensAndSend(List<String> deviceIds, String eventId,
                                    String message, String categoryValue) {
        final int total = deviceIds.size();
        final List<String> tokens = Collections.synchronizedList(new ArrayList<>());
        final TokenFetchAggregator aggregator = new TokenFetchAggregator(total, tokens, () -> {
            if (!tokens.isEmpty() && fcmHelper != null) {
                Map<String, String> data = new HashMap<>();
                data.put("eventId", eventId);
                data.put("category", categoryValue);
                fcmHelper.sendNotifications(tokens, "Event Notification", message, data);
            }
        });

        boolean isWinnerCategory = "winner".equals(categoryValue);

        for (String deviceId : deviceIds) {
            if (isWinnerCategory) {
                fetchTokenForDevice(deviceId, tokens, aggregator);
            } else {
                checkPreferenceAndFetchToken(deviceId, tokens, aggregator);
            }
        }
    }

    private void fetchTokenForDevice(String deviceId, List<String> tokens, TokenFetchAggregator aggregator) {
        entrantDB.getFCMToken(deviceId, new EntrantDB.Callback<String>() {
            @Override
            public void onSuccess(String token) {
                if (token != null && !token.isEmpty()) {
                    tokens.add(token);
                }
                aggregator.onTokenFetched();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.d(TAG, "Failed to get FCM token for " + deviceId, e);
                aggregator.onTokenFetched();
            }
        });
    }

    private void checkPreferenceAndFetchToken(String deviceId, List<String> tokens, TokenFetchAggregator aggregator) {
        entrantDB.getNotificationPreference(deviceId, new EntrantDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean enabled) {
                if (enabled != null && enabled) {
                    fetchTokenForDevice(deviceId, tokens, aggregator);
                } else {
                    aggregator.onTokenFetched();
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.d(TAG, "Failed to get notification preference for " + deviceId, e);
                aggregator.onTokenFetched();
            }
        });
    }

    private static class TokenFetchAggregator {
        private final int total;
        private final List<String> tokens;
        private final Runnable onComplete;
        private int completed;

        TokenFetchAggregator(int total, List<String> tokens, Runnable onComplete) {
            this.total = total;
            this.tokens = tokens;
            this.onComplete = onComplete;
            this.completed = 0;
        }

        synchronized void onTokenFetched() {
            completed++;
            if (completed == total) {
                onComplete.run();
            }
        }
    }

    private static class PreferenceCheckAggregator {
        private final int total;
        private final List<String> enabledDeviceIds;
        private final Runnable onComplete;
        private int completed = 0;

        PreferenceCheckAggregator(int total, List<String> enabledDeviceIds, Runnable onComplete) {
            this.total = total;
            this.enabledDeviceIds = enabledDeviceIds;
            this.onComplete = onComplete;
        }

        synchronized void onPreferenceChecked(String deviceId, boolean enabled) {
            if (enabled && deviceId != null) {
                enabledDeviceIds.add(deviceId);
            }
            completed++;
            if (completed == total) {
                onComplete.run();
            }
        }
    }

    private static class NotificationSender {
        private final int total;
        private final NotificationCallback callback;
        private int completed = 0;
        private int failed = 0;

        NotificationSender(int total, NotificationCallback callback) {
            this.total = total;
            this.callback = callback;
        }

        synchronized void onSuccess() {
            completed++;
            checkCompletion();
        }

        synchronized void onError() {
            failed++;
            checkCompletion();
        }

        private void checkCompletion() {
            if (completed + failed == total) {
                callback.onSuccess(completed, failed);
            }
        }
    }
}
