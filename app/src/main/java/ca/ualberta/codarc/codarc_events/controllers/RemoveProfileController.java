package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.util.Log;

import java.util.List;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.models.User;

/**
 * Handles profile removal by administrators.
 * Validates admin status, coordinates removal of entrant from all events,
 * and wipes profile data while setting the banned flag.
 */
public class RemoveProfileController {

    private static final String TAG = "RemoveProfileController";
    
    /**
     * Firestore batch write limit - maximum operations per batch.
     * Used for reference when batching deletions.
     */
    private static final int FIRESTORE_BATCH_LIMIT = 500;

    /**
     * Result object for profile removal operations.
     */
    public static class RemoveProfileResult {
        private final boolean success;
        private final String errorMessage;

        private RemoveProfileResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static RemoveProfileResult success() {
            return new RemoveProfileResult(true, null);
        }

        public static RemoveProfileResult failure(String errorMessage) {
            return new RemoveProfileResult(false, errorMessage);
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
    private final UserDB userDB;

    /**
     * Creates a new RemoveProfileController with default data layer instances.
     */
    public RemoveProfileController() {
        this.entrantDB = new EntrantDB();
        this.eventDB = new EventDB();
        this.userDB = new UserDB();
    }

    /**
     * Creates a new RemoveProfileController with provided data layer instances.
     * Useful for testing with mocked dependencies.
     *
     * @param entrantDB EntrantDB instance for profile operations
     * @param eventDB EventDB instance for event removal operations
     * @param userDB UserDB instance for admin validation
     */
    public RemoveProfileController(EntrantDB entrantDB, EventDB eventDB, UserDB userDB) {
        this.entrantDB = entrantDB;
        this.eventDB = eventDB;
        this.userDB = userDB;
    }

    /**
     * Removes an entrant profile and all associated event data.
     * Validates admin status, then coordinates:
     * - Removal of entrant from all event subcollections
     * - Wiping of profile data (name, email, phone, isRegistered)
     * - Setting banned flag to prevent re-registration
     *
     * @param deviceId the device ID of the entrant to remove
     * @param adminDeviceId the device ID of the admin performing the removal
     * @param callback callback for completion
     */
    public void removeProfile(String deviceId, String adminDeviceId, Callback callback) {
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onResult(RemoveProfileResult.failure("Entrant device ID is required"));
            return;
        }
        if (adminDeviceId == null || adminDeviceId.isEmpty()) {
            callback.onResult(RemoveProfileResult.failure("Admin device ID is required"));
            return;
        }

        // Step 1: Validate admin status
        validateAdminStatus(adminDeviceId, new ValidationCallback() {
            @Override
            public void onSuccess() {
                // Step 2: Validate entrant exists
                validateEntrantExists(deviceId, new EntrantValidationCallback() {
                    @Override
                    public void onSuccess() {
                        // Step 3: Get list of events entrant is part of
                        getEntrantEvents(deviceId, new EventsCallback() {
                            @Override
                            public void onSuccess(List<String> eventIds) {
                                // Step 4: Remove from all events (non-blocking - log errors but continue)
                                removeFromAllEvents(deviceId, eventIds);

                                // Step 5: Delete events subcollection from entrant document
                                deleteEntrantEventsSubcollection(deviceId);

                                // Step 6: Wipe profile data and set banned flag
                                wipeProfile(deviceId, callback);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                // If we can't get events, still try to delete subcollection and wipe profile
                                Log.w(TAG, "Failed to get entrant events, proceeding with cleanup: " + deviceId);
                                deleteEntrantEventsSubcollection(deviceId);
                                wipeProfile(deviceId, callback);
                            }
                        });
                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onResult(RemoveProfileResult.failure(errorMessage));
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onResult(RemoveProfileResult.failure(errorMessage));
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
     * Validates that the entrant exists.
     *
     * @param deviceId the device ID to validate
     * @param callback callback for validation result
     */
    private void validateEntrantExists(String deviceId, EntrantValidationCallback callback) {
        entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
            @Override
            public void onSuccess(Entrant entrant) {
                // Entrant exists (even if null, document might exist)
                callback.onSuccess();
            }

            @Override
            public void onError(@NonNull Exception e) {
                // Entrant doesn't exist - still allow removal (idempotent)
                Log.w(TAG, "Entrant not found, but proceeding with removal: " + deviceId);
                callback.onSuccess();
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
     * Non-blocking - logs errors but doesn't fail the overall operation.
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
                    // Log warning but continue - event may not exist or entrant may not be in it
                    Log.w(TAG, "Failed to remove entrant from event: " + eventId, e);
                }
            });
        }
    }

    /**
     * Deletes all events from the entrant's events subcollection.
     * Non-blocking - logs errors but doesn't fail the overall operation.
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
                // Log warning but continue - subcollection may not exist
                Log.w(TAG, "Failed to delete entrant events subcollection: " + deviceId, e);
            }
        });
    }

    /**
     * Wipes profile data and sets banned flag.
     *
     * @param deviceId the entrant device ID
     * @param callback callback for completion
     */
    private void wipeProfile(String deviceId, Callback callback) {
        entrantDB.deleteProfile(deviceId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Profile wiped and banned successfully: " + deviceId);
                callback.onResult(RemoveProfileResult.success());
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to wipe profile: " + deviceId, e);
                callback.onResult(RemoveProfileResult.failure("Failed to remove profile. Please try again."));
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
     * Callback interface for entrant validation operations.
     */
    private interface EntrantValidationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    /**
     * Callback interface for events retrieval operations.
     */
    private interface EventsCallback {
        void onSuccess(List<String> eventIds);
        void onError(String errorMessage);
    }

    /**
     * Callback interface for removal operations.
     */
    public interface Callback {
        void onResult(RemoveProfileResult result);
    }
}