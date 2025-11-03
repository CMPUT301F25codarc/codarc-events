package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Controller for joining waitlists.
 * Handles validation and business logic for waitlist operations.
 * Separated from UI to enable unit testing.
 */
public class JoinWaitlistController {

    /**
     * Result class for join waitlist operations.
     */
    public static class JoinResult {
        private final boolean success;
        private final String message;
        private final boolean needsProfileRegistration;

        private JoinResult(boolean success, String message, boolean needsProfileRegistration) {
            this.success = success;
            this.message = message;
            this.needsProfileRegistration = needsProfileRegistration;
        }

        public static JoinResult success(String message) {
            return new JoinResult(true, message, false);
        }

        public static JoinResult failure(String message) {
            return new JoinResult(false, message, false);
        }

        public static JoinResult requiresProfileRegistration() {
            return new JoinResult(false, "Profile registration required", true);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public boolean needsProfileRegistration() {
            return needsProfileRegistration;
        }
    }

    private final EventDB eventDB;
    private final EntrantDB entrantDB;

    /**
     * Constructs a JoinWaitlistController.
     *
     * @param eventDB the EventDB instance for event operations
     * @param entrantDB the EntrantDB instance for entrant operations
     */
    public JoinWaitlistController(EventDB eventDB, EntrantDB entrantDB) {
        this.eventDB = eventDB;
        this.entrantDB = entrantDB;
    }

    /**
     * Checks if an entrant is registered (has completed profile).
     *
     * @param deviceId the device ID of the entrant
     * @param callback callback with true if registered, false otherwise
     */
    public void checkProfileRegistration(String deviceId, EntrantDB.Callback<Boolean> callback) {
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onError(new IllegalArgumentException("deviceId cannot be null or empty"));
            return;
        }

        entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
            @Override
            public void onSuccess(Entrant entrant) {
                boolean isRegistered = entrant != null && entrant.getIsRegistered();
                callback.onSuccess(isRegistered);
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onError(e);
            }
        });
    }

    /**
     * Validates conditions for joining a waitlist and performs the join operation.
     *
     * @param event the event to join
     * @param deviceId the device ID of the entrant
     * @param callback callback with JoinResult
     */
    public void joinWaitlist(Event event, String deviceId, Callback callback) {
        if (event == null) {
            callback.onResult(JoinResult.failure("Event is required"));
            return;
        }
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onResult(JoinResult.failure("Device ID is required"));
            return;
        }

        // First check profile registration
        checkProfileRegistration(deviceId, new EntrantDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isRegistered) {
                if (!isRegistered) {
                    callback.onResult(JoinResult.requiresProfileRegistration());
                    return;
                }

                // Check if already joined
                eventDB.isEntrantOnWaitlist(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean alreadyJoined) {
                        if (alreadyJoined) {
                            callback.onResult(JoinResult.failure("Already joined"));
                            return;
                        }

                        // Validate registration window
                        if (!EventValidationHelper.isWithinRegistrationWindow(event)) {
                            callback.onResult(JoinResult.failure("Registration window is closed"));
                            return;
                        }

                        // Validate capacity
                        eventDB.getWaitlistCount(event.getId(), new EventDB.Callback<Integer>() {
                            @Override
                            public void onSuccess(Integer currentCount) {
                                if (!EventValidationHelper.hasCapacity(event, currentCount)) {
                                    callback.onResult(JoinResult.failure("Event is full"));
                                    return;
                                }

                                // All validations passed, join waitlist
                                eventDB.joinWaitlist(event.getId(), deviceId, new EventDB.Callback<Void>() {
                                    @Override
                                    public void onSuccess(Void value) {
                                        callback.onResult(JoinResult.success("Joined successfully"));
                                    }

                                    @Override
                                    public void onError(@NonNull Exception e) {
                                        callback.onResult(JoinResult.failure("Failed to join. Please try again."));
                                    }
                                });
                            }

                            @Override
                            public void onError(@NonNull Exception e) {
                                callback.onResult(JoinResult.failure("Failed to check availability"));
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        callback.onResult(JoinResult.failure("Failed to check status. Please try again."));
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onResult(JoinResult.failure("Failed to check profile"));
            }
        });
    }

    /**
     * Callback interface for join waitlist operations.
     */
    public interface Callback {
        void onResult(JoinResult result);
    }
}
