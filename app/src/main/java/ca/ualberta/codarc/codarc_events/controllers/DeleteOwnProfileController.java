package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.util.Log;

import java.util.List;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Handles profile removal by the entrant themselves.
 */
public class DeleteOwnProfileController {

    private static final String TAG = "DeleteOwnProfileController";

    public static class DeleteProfileResult {
        private final boolean success;
        private final String errorMessage;

        private DeleteProfileResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static DeleteProfileResult success() {
            return new DeleteProfileResult(true, null);
        }

        public static DeleteProfileResult failure(String errorMessage) {
            return new DeleteProfileResult(false, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private final EntrantDB entrantDB;
    private final EventDB eventDB;

    public DeleteOwnProfileController() {
        this.entrantDB = new EntrantDB();
        this.eventDB = new EventDB();
    }

    /**
     * Creates a new DeleteOwnProfileController with provided data layer instances.
     *
     * @param entrantDB EntrantDB instance for profile operations
     * @param eventDB EventDB instance for event removal operations
     */
    public DeleteOwnProfileController(EntrantDB entrantDB, EventDB eventDB) {
        this.entrantDB = entrantDB;
        this.eventDB = eventDB;
    }

    /**
     * Removes the entrant's own profile and all associated event data.
     *
     * @param deviceId the device ID of the entrant removing their profile
     * @param callback callback for completion
     */
    public void deleteOwnProfile(String deviceId, Callback callback) {
        deleteProfileInternal(deviceId, false, callback);
    }

    /**
     * Internal method to delete a profile with optional ban flag.
     * Used by both DeleteOwnProfileController and AdminRemoveProfileController.
     *
     * @param deviceId the device ID of the entrant
     * @param shouldBan true to ban the user, false to just clear the profile
     * @param callback callback for completion
     */
    void deleteProfileInternal(String deviceId, boolean shouldBan, Callback callback) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            callback.onResult(DeleteProfileResult.failure("Device ID is required"));
            return;
        }

        getEntrantEvents(deviceId, new EventsCallback() {
            @Override
            public void onSuccess(List<String> eventIds) {
                removeFromAllEvents(deviceId, eventIds);
                deleteEntrantEventsSubcollection(deviceId);
                clearProfile(deviceId, shouldBan, callback);
            }

            @Override
            public void onError(String errorMessage) {
                Log.w(TAG, "Failed to get entrant events, proceeding with cleanup: " + deviceId);
                deleteEntrantEventsSubcollection(deviceId);
                clearProfile(deviceId, shouldBan, callback);
            }
        });
    }

    /**
     * Gets the list of event IDs the entrant is part of.
     *
     * @param deviceId the entrant device ID
     * @param callback callback with list of event IDs
     */
    private void getEntrantEvents(String deviceId, EventsCallback callback) {
        entrantDB.getEntrantEvents(deviceId, new EntrantDB.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> eventIds) {
                callback.onSuccess(eventIds != null ? eventIds : new java.util.ArrayList<>());
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to get entrant events", e);
                callback.onError("Failed to get entrant events");
            }
        });
    }

    /**
     * Removes the entrant from all events they are part of.
     *
     * @param deviceId the entrant device ID
     * @param eventIds the list of event IDs to remove from
     */
    private void removeFromAllEvents(String deviceId, List<String> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            Log.d(TAG, "No events to remove entrant from: " + deviceId);
            return;
        }

        for (String eventId : eventIds) {
            eventDB.removeEntrantFromEvent(eventId, deviceId, new EventDB.Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                    Log.d(TAG, "Removed entrant from event: " + eventId);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    Log.w(TAG, "Failed to remove entrant from event: " + eventId, e);
                }
            });
        }
    }

    /**
     * Deletes all events from the entrant's events subcollection.
     *
     * @param deviceId the entrant device ID
     */
    private void deleteEntrantEventsSubcollection(String deviceId) {
        entrantDB.deleteAllEntrantEvents(deviceId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Deleted entrant events subcollection: " + deviceId);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.w(TAG, "Failed to delete entrant events subcollection: " + deviceId, e);
            }
        });
    }

    /**
     * Clears profile data.
     *
     * @param deviceId the entrant device ID
     * @param shouldBan true to ban the user, false to just clear the profile
     * @param callback callback for completion
     */
    private void clearProfile(String deviceId, boolean shouldBan, Callback callback) {
        entrantDB.deleteProfile(deviceId, shouldBan, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Profile cleared successfully: " + deviceId);
                callback.onResult(DeleteProfileResult.success());
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to clear profile: " + deviceId, e);
                callback.onResult(DeleteProfileResult.failure("Failed to delete profile. Please try again."));
            }
        });
    }

    /**
     * Callback interface for events retrieval operations.
     */
    private interface EventsCallback {
        void onSuccess(List<String> eventIds);
        void onError(String errorMessage);
    }

    /**
     * Callback interface for deletion operations.
     */
    public interface Callback {
        void onResult(DeleteProfileResult result);
    }
}
