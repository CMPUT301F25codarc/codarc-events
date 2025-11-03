package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.util.Patterns;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;

/**
 * Controller for profile creation and management.
 * Handles validation and business logic for profile operations.
 * Separated from UI to enable unit testing.
 */
public class ProfileController {

    /**
     * Result class for profile operations.
     */
    public static class ProfileResult {
        private final boolean isValid;
        private final String errorMessage;
        private final Entrant entrant;

        private ProfileResult(boolean isValid, String errorMessage, Entrant entrant) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.entrant = entrant;
        }

        public static ProfileResult success(Entrant entrant) {
            return new ProfileResult(true, null, entrant);
        }

        public static ProfileResult failure(String errorMessage) {
            return new ProfileResult(false, errorMessage, null);
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Entrant getEntrant() {
            return entrant;
        }
    }

    private final EntrantDB entrantDB;

    /**
     * Constructs a ProfileController.
     *
     * @param entrantDB the EntrantDB instance for database operations
     */
    public ProfileController(EntrantDB entrantDB) {
        this.entrantDB = entrantDB;
    }

    /**
     * Validates profile data and creates an Entrant object.
     * Does not persist to database - caller should call saveProfile() after validation.
     *
     * @param deviceId the device ID of the entrant
     * @param name the entrant's name
     * @param email the entrant's email
     * @param phone the entrant's phone number (optional)
     * @return ProfileResult containing validation status and Entrant if valid
     */
    public ProfileResult validateAndCreateProfile(String deviceId, String name, String email, String phone) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return ProfileResult.failure("Device ID is required");
        }

        if (TextUtils.isEmpty(name) || name.trim().isEmpty()) {
            return ProfileResult.failure("Name is required");
        }

        if (TextUtils.isEmpty(email) || email.trim().isEmpty()) {
            return ProfileResult.failure("Email is required");
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ProfileResult.failure("Valid email address is required");
        }

        Entrant entrant = new Entrant(deviceId, name.trim(), System.currentTimeMillis());
        entrant.setEmail(email.trim());
        entrant.setPhone(phone != null ? phone.trim() : "");
        entrant.setIsRegistered(true);

        return ProfileResult.success(entrant);
    }

    /**
     * Persists a profile to the database.
     *
     * @param deviceId the device ID of the entrant
     * @param entrant the entrant to persist
     * @param callback callback for success/error
     */
    public void saveProfile(String deviceId, Entrant entrant, EntrantDB.Callback<Void> callback) {
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Device ID cannot be null or empty"));
            return;
        }
        if (entrant == null) {
            callback.onError(new IllegalArgumentException("Entrant cannot be null"));
            return;
        }
        entrantDB.upsertProfile(deviceId, entrant, callback);
    }

    /**
     * Deletes a profile from the database.
     *
     * @param deviceId the device ID of the entrant whose profile should be deleted
     * @param callback callback for success/error
     */
    public void deleteProfile(String deviceId, EntrantDB.Callback<Void> callback) {
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Device ID cannot be null or empty"));
            return;
        }
        entrantDB.deleteProfile(deviceId, callback);
    }
}

