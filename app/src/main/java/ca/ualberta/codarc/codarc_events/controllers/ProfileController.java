package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.util.Patterns;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;

/**
 * Handles profile creation and validation.
 */
public class ProfileController {

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

    public ProfileController(EntrantDB entrantDB) {
        this.entrantDB = entrantDB;
    }

    // Validates and creates Entrant object (doesn't save to DB yet)
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

    // Saves profile to Firestore
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

    // Clears profile (doesn't delete document)
    public void deleteProfile(String deviceId, EntrantDB.Callback<Void> callback) {
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Device ID cannot be null or empty"));
            return;
        }
        entrantDB.deleteProfile(deviceId, callback);
    }
}

