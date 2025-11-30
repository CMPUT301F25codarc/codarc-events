package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.util.Log;

import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.PosterStorage;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.User;

/**
 * Handles image removal by administrators.
 * Validates admin status, coordinates deletion of poster from Firebase Storage,
 * and updates the event document to set posterUrl to null.
 */
public class RemoveImageController {

    private static final String TAG = "RemoveImageController";

    /**
     * Result object for image removal operations.
     */
    public static class RemoveImageResult {
        private final boolean success;
        private final String errorMessage;

        private RemoveImageResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static RemoveImageResult success() {
            return new RemoveImageResult(true, null);
        }

        public static RemoveImageResult failure(String errorMessage) {
            return new RemoveImageResult(false, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private final EventDB eventDB;
    private final PosterStorage posterStorage;
    private final UserDB userDB;

    public RemoveImageController() {
        this.eventDB = new EventDB();
        this.posterStorage = new PosterStorage();
        this.userDB = new UserDB();
    }

    /**
     * Removes an image (poster) from an event.
     * Validates admin status, then coordinates deletion of:
     * - Poster image from Firebase Storage
     * - Event document update (set posterUrl to null)
     *
     * @param eventId the event ID whose image should be removed
     * @param adminDeviceId the device ID of the admin performing the removal
     * @param callback callback for completion
     */
    public void removeImage(String eventId, String adminDeviceId, Callback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onResult(RemoveImageResult.failure("Event ID is required"));
            return;
        }
        if (adminDeviceId == null || adminDeviceId.isEmpty()) {
            callback.onResult(RemoveImageResult.failure("Admin device ID is required"));
            return;
        }

        // Step 1: Validate admin status
        validateAdminStatus(adminDeviceId, new ValidationCallback() {
            @Override
            public void onSuccess() {
                // Step 2: Validate event exists and has a poster
                validateEventHasPoster(eventId, new EventCallback() {
                    @Override
                    public void onSuccess(Event event) {
                        // Step 3: Delete poster from Firebase Storage (non-blocking)
                        deletePoster(eventId);

                        // Step 4: Update event document to set posterUrl to null
                        event.setPosterUrl(null);
                        eventDB.addEvent(event, new EventDB.Callback<Void>() {
                            @Override
                            public void onSuccess(Void value) {
                                Log.d(TAG, "Event poster removed successfully: " + eventId);
                                callback.onResult(RemoveImageResult.success());
                            }

                            @Override
                            public void onError(@NonNull Exception e) {
                                Log.e(TAG, "Failed to update event after removing poster: " + eventId, e);
                                callback.onResult(RemoveImageResult.failure("Failed to update event. Please try again."));
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onResult(RemoveImageResult.failure(errorMessage));
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onResult(RemoveImageResult.failure(errorMessage));
            }
        });
    }

    /**
     * Validates that the user is an administrator.
     *
     * @param deviceId the device ID to check
     * @param callback callback for validation result
     */
    private void validateAdminStatus(String deviceId, ValidationCallback callback) {
        userDB.getUser(deviceId, new UserDB.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.isAdmin()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Admin access required");
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to check admin status", e);
                callback.onError("Failed to verify admin status");
            }
        });
    }

    /**
     * Validates that the event exists and has a poster to remove.
     *
     * @param eventId the event ID to validate
     * @param callback callback with the event if it exists and has a poster
     */
    private void validateEventHasPoster(String eventId, EventCallback callback) {
        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null) {
                    callback.onError("Event not found");
                    return;
                }

                String posterUrl = event.getPosterUrl();
                if (posterUrl == null || posterUrl.trim().isEmpty()) {
                    callback.onError("This event has no image to remove");
                    return;
                }

                callback.onSuccess(event);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Event not found: " + eventId, e);
                callback.onError("Event not found");
            }
        });
    }

    /**
     * Deletes the event poster from Firebase Storage.
     * Non-blocking - logs errors but doesn't fail the overall operation.
     *
     * @param eventId the event ID
     */
    private void deletePoster(String eventId) {
        posterStorage.deletePoster(eventId, new PosterStorage.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Poster deleted successfully from Storage for event: " + eventId);
            }

            @Override
            public void onError(@NonNull Exception e) {
                // Log warning but don't fail - poster may not exist in Storage
                Log.w(TAG, "Failed to delete poster from Storage for event: " + eventId, e);
            }
        });
    }

    /**
     * Callback interface for validation operations.
     */
    private interface ValidationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    /**
     * Callback interface for event retrieval operations.
     */
    private interface EventCallback {
        void onSuccess(Event event);
        void onError(String errorMessage);
    }

    /**
     * Callback interface for removal operations.
     */
    public interface Callback {
        void onResult(RemoveImageResult result);
    }
}

