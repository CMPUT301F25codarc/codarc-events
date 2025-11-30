package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;

/**
 * Handles sending broadcast notifications to all selected entrants (winners) for an event.
 * This class delegates to the unified NotificationController for implementation.
 */
public class NotifyWinnersController {

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
    public interface NotifyWinnersCallback {
        void onSuccess(int notifiedCount, int failedCount);
        void onError(@NonNull Exception e);
    }

    private final NotificationController notificationController;

    /**
     * Creates a new NotifyWinnersController.
     *
     * @param eventDB EventDB instance
     * @param entrantDB EntrantDB instance
     */
    public NotifyWinnersController(EventDB eventDB, EntrantDB entrantDB) {
        this.notificationController = new NotificationController(eventDB, entrantDB);
    }

    /**
     * Validates the notification message.
     *
     * @param message the message to validate
     * @return ValidationResult indicating if message is valid
     */
    public ValidationResult validateMessage(String message) {
        NotificationController.ValidationResult result = notificationController.validateMessage(message);
        if (result.isValid()) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure(result.getErrorMessage());
        }
    }

    /**
     * Sends notifications to all selected entrants (winners) for an event.
     *
     * @param eventId the event ID
     * @param message the notification message
     * @param callback callback for operation completion
     */
    public void notifyWinners(String eventId, String message, NotifyWinnersCallback callback) {
        notificationController.notifyUsers(
                eventId,
                message,
                NotificationController.NotificationCategory.WINNERS,
                "No winners selected",
                new NotificationController.NotificationCallback() {
                    @Override
                    public void onSuccess(int notifiedCount, int failedCount) {
                        callback.onSuccess(notifiedCount, failedCount);
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        callback.onError(e);
                    }
                }
        );
    }
}

