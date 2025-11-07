package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Handles joining waitlists - validation and business logic.
 */
public class JoinWaitlistController {

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

    public JoinWaitlistController(EventDB eventDB, EntrantDB entrantDB) {
        this.eventDB = eventDB;
        this.entrantDB = entrantDB;
    }

    // Check if user has completed profile registration
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

    // Main join waitlist logic - validates and joins
    public void joinWaitlist(Event event, String deviceId, Callback callback) {
        if (event == null) {
            callback.onResult(JoinResult.failure("Event is required"));
            return;
        }
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onResult(JoinResult.failure("Device ID is required"));
            return;
        }

        // Check if user is the organizer of this event
        if (event.getOrganizerId() != null && event.getOrganizerId().equals(deviceId)) {
            callback.onResult(JoinResult.failure("You cannot join your own event"));
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

    public void getWaitlistCount(String eventId, EventDB.Callback<Integer> cb) {
        eventDB.getWaitlistCount(eventId, cb);
    }

    public interface Callback {
        void onResult(JoinResult result);
    }
}
