package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.util.Log;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.User;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Handles profile removal by administrators.
 */
public class AdminRemoveProfileController {

    private static final String TAG = "AdminRemoveProfileController";

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

    private final DeleteOwnProfileController deleteProfileController;
    private final UserDB userDB;

    public AdminRemoveProfileController() {
        this.deleteProfileController = new DeleteOwnProfileController();
        this.userDB = new UserDB();
    }

    /**
     * Creates a new AdminRemoveProfileController with provided data layer instances.
     *
     * @param entrantDB EntrantDB instance for profile operations
     * @param eventDB EventDB instance for event removal operations
     * @param userDB UserDB instance for admin validation
     */
    public AdminRemoveProfileController(EntrantDB entrantDB, EventDB eventDB, UserDB userDB) {
        this.deleteProfileController = new DeleteOwnProfileController(entrantDB, eventDB);
        this.userDB = userDB;
    }

    /**
     * Removes an entrant profile and all associated event data.
     *
     * @param deviceId the device ID of the entrant to remove
     * @param adminDeviceId the device ID of the admin performing the removal
     * @param callback callback for completion
     */
    public void removeProfile(String deviceId, String adminDeviceId, Callback callback) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
            ValidationHelper.requireNonEmpty(adminDeviceId, "adminDeviceId");
        } catch (IllegalArgumentException e) {
            callback.onResult(RemoveProfileResult.failure(e.getMessage()));
            return;
        }

        validateAdminStatus(adminDeviceId, new ValidationCallback() {
            @Override
            public void onSuccess() {
                deleteProfileController.deleteProfileInternal(deviceId, true, new DeleteOwnProfileController.Callback() {
                    @Override
                    public void onResult(DeleteOwnProfileController.DeleteProfileResult result) {
                        if (result.isSuccess()) {
                            callback.onResult(RemoveProfileResult.success());
                        } else {
                            callback.onResult(RemoveProfileResult.failure(result.getErrorMessage()));
                        }
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
     * Callback interface for validation operations.
     */
    private interface ValidationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    /**
     * Callback interface for removal operations.
     */
    public interface Callback {
        void onResult(RemoveProfileResult result);
    }
}
