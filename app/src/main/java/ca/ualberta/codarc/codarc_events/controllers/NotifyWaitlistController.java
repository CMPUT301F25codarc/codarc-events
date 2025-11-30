package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;

/**
 * Handles sending broadcast notifications to all entrants on an event's waiting list.
 * Validates message content and coordinates notification delivery.
 */
public class NotifyWaitlistController {

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
    public static class NotifyWaitlistResult {
        private final boolean isSuccess;
        private final String errorMessage;
        private final int notifiedCount;
        private final int failedCount;

        private NotifyWaitlistResult(boolean isSuccess, String errorMessage, int notifiedCount, int failedCount) {
            this.isSuccess = isSuccess;
            this.errorMessage = errorMessage;
            this.notifiedCount = notifiedCount;
            this.failedCount = failedCount;
        }

        public static NotifyWaitlistResult success(int notifiedCount, int failedCount) {
            return new NotifyWaitlistResult(true, null, notifiedCount, failedCount);
        }

        public static NotifyWaitlistResult failure(String errorMessage) {
            return new NotifyWaitlistResult(false, errorMessage, 0, 0);
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
    public interface NotifyWaitlistCallback {
        void onSuccess(int notifiedCount, int failedCount);
        void onError(@NonNull Exception e);
    }

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final String NOTIFICATION_CATEGORY = "waitlist_broadcast";

    private final EventDB eventDB;
    private final EntrantDB entrantDB;

    /**
     * Creates a new NotifyWaitlistController.
     *
     * @param eventDB EventDB instance for accessing waitlist data
     * @param entrantDB EntrantDB instance for sending notifications
     */
    public NotifyWaitlistController(EventDB eventDB, EntrantDB entrantDB) {
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
     * Sends notifications to all entrants on the waiting list.
     * Validates inputs, fetches waitlist, and sends notifications to each entrant.
     *
     * @param eventId the event ID
     * @param message the notification message
     * @param callback callback for operation completion
     */
    public void notifyWaitlist(String eventId, String message, NotifyWaitlistCallback callback) {
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

        // Fetch waitlist
        eventDB.getWaitlist(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> waitlist) {
                if (waitlist == null || waitlist.isEmpty()) {
                    callback.onError(new RuntimeException("Waitlist is empty"));
                    return;
                }

                // Send notifications to all entrants
                sendNotificationsToWaitlist(eventId, message, waitlist, callback);
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Sends notifications to each entrant in the waitlist.
     * Tracks success and failure counts, continuing even if some notifications fail.
     *
     * @param eventId the event ID
     * @param message the notification message
     * @param waitlist list of waitlist entries
     * @param callback callback for operation completion
     */
    private void sendNotificationsToWaitlist(String eventId, String message,
                                             List<Map<String, Object>> waitlist,
                                             NotifyWaitlistCallback callback) {
        final int total = waitlist.size();
        final int[] completed = {0};
        final int[] failed = {0};

        if (total == 0) {
            callback.onSuccess(0, 0);
            return;
        }

        for (Map<String, Object> entry : waitlist) {
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
                            android.util.Log.e("NotifyWaitlistController",
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
                                             NotifyWaitlistCallback callback) {
        if (completed[0] + failed[0] == total) {
            // Report success even if some failed - better to notify most users than fail entirely
            callback.onSuccess(completed[0], failed[0]);
        }
    }
}

