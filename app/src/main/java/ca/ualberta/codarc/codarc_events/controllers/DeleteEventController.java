package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.util.Log;

import java.util.List;

import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.OrganizerDB;
import ca.ualberta.codarc.codarc_events.data.PosterStorage;
import ca.ualberta.codarc.codarc_events.data.TagDB;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.User;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Handles event deletion by administrators.
 */
public class DeleteEventController {

    private static final String TAG = "DeleteEventController";

    public static class DeleteEventResult {
        private final boolean success;
        private final String errorMessage;

        private DeleteEventResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static DeleteEventResult success() {
            return new DeleteEventResult(true, null);
        }

        public static DeleteEventResult failure(String errorMessage) {
            return new DeleteEventResult(false, errorMessage);
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
    private final OrganizerDB organizerDB;
    private final TagDB tagDB;
    private final UserDB userDB;

    public DeleteEventController() {
        this.eventDB = new EventDB();
        this.posterStorage = new PosterStorage();
        this.organizerDB = new OrganizerDB();
        this.tagDB = new TagDB();
        this.userDB = new UserDB();
    }

    /**
     * Deletes an event and all associated data.
     *
     * @param eventId the event ID to delete
     * @param adminDeviceId the device ID of the admin performing the deletion
     * @param callback callback for completion
     */
    public void deleteEvent(String eventId, String adminDeviceId, Callback callback) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(adminDeviceId, "adminDeviceId");
        } catch (IllegalArgumentException e) {
            callback.onResult(DeleteEventResult.failure(e.getMessage()));
            return;
        }

        validateAdminStatus(adminDeviceId, new ValidationCallback() {
            @Override
            public void onSuccess() {
                proceedWithEventDeletion(eventId, callback);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onResult(DeleteEventResult.failure(errorMessage));
            }
        });
    }

    /**
     * Proceeds with event deletion after admin validation.
     */
    private void proceedWithEventDeletion(String eventId, Callback callback) {
        validateAndGetEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(Event event) {
                performCleanupOperations(eventId, event);
                deleteEventFromFirestore(eventId, callback);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onResult(DeleteEventResult.failure(errorMessage));
            }
        });
    }

    /**
     * Performs cleanup operations (poster deletion, organizer removal, tag decrement).
     * These are fire-and-forget operations that don't block event deletion.
     */
    private void performCleanupOperations(String eventId, Event event) {
        deletePoster(eventId);

        if (event.getOrganizerId() != null && !event.getOrganizerId().isEmpty()) {
            removeFromOrganizer(event.getOrganizerId(), eventId);
        }

        if (event.getTags() != null && !event.getTags().isEmpty()) {
            decrementTagUsage(event.getTags());
        }
    }

    /**
     * Deletes the event from Firestore.
     */
    private void deleteEventFromFirestore(String eventId, Callback callback) {
        eventDB.deleteEvent(eventId, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Event deleted successfully: " + eventId);
                callback.onResult(DeleteEventResult.success());
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to delete event: " + eventId, e);
                callback.onResult(DeleteEventResult.failure("Failed to delete event. Please try again."));
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
     * Validates that the event exists and retrieves it.
     *
     * @param eventId the event ID to validate
     * @param callback callback with the event if it exists
     */
    private void validateAndGetEvent(String eventId, EventCallback callback) {
        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
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
     *
     * @param eventId the event ID
     */
    private void deletePoster(String eventId) {
        posterStorage.deletePoster(eventId, new PosterStorage.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Poster deleted successfully for event: " + eventId);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.w(TAG, "Failed to delete poster for event: " + eventId, e);
            }
        });
    }

    /**
     * Removes the event from the organizer's events subcollection.
     *
     * @param organizerId the organizer's device ID
     * @param eventId the event ID
     */
    private void removeFromOrganizer(String organizerId, String eventId) {
        organizerDB.removeEventFromOrganizer(organizerId, eventId, new OrganizerDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Removed event from organizer's events: " + eventId);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.w(TAG, "Failed to remove event from organizer: " + eventId, e);
            }
        });
    }

    /**
     * Decrements tag usage counts for the event's tags.
     *
     * @param tags the list of tags to decrement
     */
    private void decrementTagUsage(List<String> tags) {
        tagDB.removeTags(tags, new TagDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Tag usage decremented successfully");
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.w(TAG, "Failed to decrement tag usage", e);
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
     * Callback interface for deletion operations.
     */
    public interface Callback {
        void onResult(DeleteEventResult result);
    }
}

