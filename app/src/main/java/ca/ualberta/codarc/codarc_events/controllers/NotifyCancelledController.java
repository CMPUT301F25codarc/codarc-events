package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;

/**
 * Handles sending broadcast notifications to all cancelled entrants for an event.
 * Validates message content and coordinates notification delivery.
 */
public class NotifyCancelledController {

    /**
     * Result object for message validation.
     */
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
     * Result object for notification sending operation.
     */
    public static class NotifyCancelledResult {
        private final boolean isSuccess;
        private final String errorMessage;
        private final int notifiedCount;
        private final int failedCount;

        private NotifyCancelledResult(boolean isSuccess, String errorMessage, int notifiedCount, int failedCount) {
            this.isSuccess = isSuccess;
            this.errorMessage = errorMessage;
            this.notifiedCount = notifiedCount;
            this.failedCount = failedCount;
        }

        public static NotifyCancelledResult success(int notifiedCount, int failedCount) {
            return new NotifyCancelledResult(true, null, notifiedCount, failedCount);
        }

        public static NotifyCancelledResult failure(String errorMessage) {
            return new NotifyCancelledResult(false, errorMessage, 0, 0);
        }

        public boolean isSuccess() {
            return isSuccess;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public int getNotifiedCount() {
            return notifiedCount;
        }

        public int getFailedCount() {
            return failedCount;
        }
    }

    /**
     * Callback interface for notification sending operation.
     */
    public interface NotifyCancelledCallback {
        void onSuccess(int notifiedCount, int failedCount);
        void onError(@NonNull Exception e);
    }

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final String NOTIFICATION_CATEGORY = "cancelled_broadcast";

    private final EventDB eventDB;
    private final EntrantDB entrantDB;

    /**
     * Creates a new NotifyCancelledController.
     *
     * @param eventDB EventDB instance for accessing cancelled entrants data
     * @param entrantDB EntrantDB instance for sending notifications
     */
    public NotifyCancelledController(EventDB eventDB, EntrantDB entrantDB) {
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
     * Sends notifications to all cancelled entrants for an event.
     * Validates inputs, fetches cancelled entrants, and sends notifications to each entrant.
     *
     * @param eventId the event ID
     * @param message the notification message
     * @param callback callback for operation completion
     */
    public void notifyCancelled(String eventId, String message, NotifyCancelledCallback callback) {
        // Validate inputs
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }

        ValidationResult validation = validateMessage(message);
        if (!validation.isValid()) {
            callback.onError(new IllegalArgumentException(validation.getErrorMessage()));
            return;
        }

        // Fetch cancelled entrants
        eventDB.getCancelled(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> cancelled) {
                if (cancelled == null || cancelled.isEmpty()) {
                    callback.onError(new RuntimeException("No cancelled entrants found"));
                    return;
                }

                // Send notifications to all cancelled entrants
                sendNotificationsToCancelled(eventId, message, cancelled, callback);
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Sends notifications to each entrant in the cancelled list.
     * Tracks success and failure counts, continuing even if some notifications fail.
     *
     * @param eventId the event ID
     * @param message the notification message
     * @param cancelled list of cancelled entrant entries
     * @param callback callback for operation completion
     */
    private void sendNotificationsToCancelled(String eventId, String message,
                                               List<Map<String, Object>> cancelled,
                                               NotifyCancelledCallback callback) {
        final int total = cancelled.size();
        final int[] completed = {0};
        final int[] failed = {0};

        if (total == 0) {
            callback.onSuccess(0, 0);
            return;
        }

        for (Map<String, Object> entry : cancelled) {
            Object deviceIdObj = entry.get("deviceId");
            if (deviceIdObj == null) {
                // Skip entries without deviceId
                completed[0]++;
                failed[0]++;
                checkNotificationCompletion(completed, failed, total, callback);
                continue;
            }

            String deviceId = deviceIdObj.toString();
            entrantDB.addNotification(deviceId, eventId, message, NOTIFICATION_CATEGORY,
                    new EntrantDB.Callback<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                            completed[0]++;
                            checkNotificationCompletion(completed, failed, total, callback);
                        }

                        @Override
                        public void onError(@NonNull Exception e) {
                            failed[0]++;
                            // Log but don't fail the entire operation
                            android.util.Log.e("NotifyCancelledController",
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
                                             NotifyCancelledCallback callback) {
        if (completed[0] + failed[0] == total) {
            // Report success even if some failed - better to notify most users than fail entirely
            callback.onSuccess(completed[0], failed[0]);
        }
    }
}

