package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
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

    private final EventDB eventDB;
    private final EntrantDB entrantDB;

    /**
     * Creates a new NotificationController.
     *
     * @param eventDB EventDB instance
     * @param entrantDB EntrantDB instance
     */
    public NotificationController(EventDB eventDB, EntrantDB entrantDB) {
        this.eventDB = eventDB;
        this.entrantDB = entrantDB;
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
        final int[] completed = {0};
        final int[] failed = {0};

        if (total == 0) {
            callback.onSuccess(0, 0);
            return;
        }

        for (Map<String, Object> entry : entrants) {
            Object deviceIdObj = entry.get("deviceId");
            if (deviceIdObj == null) {
                completed[0]++;
                failed[0]++;
                checkNotificationCompletion(completed, failed, total, callback);
                continue;
            }

            String deviceId = deviceIdObj.toString();
            entrantDB.addNotification(deviceId, eventId, message, categoryValue,
                    new EntrantDB.Callback<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                            completed[0]++;
                            checkNotificationCompletion(completed, failed, total, callback);
                        }

                        @Override
                        public void onError(@NonNull Exception e) {
                            failed[0]++;
                            android.util.Log.e("NotificationController",
                                    "Failed to send notification to " + deviceId, e);
                            checkNotificationCompletion(completed, failed, total, callback);
                        }
                    });
        }
    }

    /**
     * Checks if all notifications have been processed and calls the callback.
     *
     * @param completed array tracking completed notifications
     * @param failed array tracking failed notifications
     * @param total total number of notifications to send
     * @param callback callback to invoke when all notifications are processed
     */
    private void checkNotificationCompletion(int[] completed, int[] failed, int total,
                                             NotificationCallback callback) {
        if (completed[0] + failed[0] == total) {
            callback.onSuccess(completed[0], failed[0]);
        }
    }
}
