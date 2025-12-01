package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.LocationHelper;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

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

    /**
     * Checks if user has completed profile registration.
     *
     * @param deviceId the device ID
     * @param callback callback with registration status
     */
    public void checkProfileRegistration(String deviceId, EntrantDB.Callback<Boolean> callback) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            callback.onError(e);
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
     * Joins the waitlist for an event.
     *
     * @param event the event to join
     * @param deviceId the device ID of the entrant
     * @param callback callback for completion
     */
    public void joinWaitlist(Event event, String deviceId, Callback callback) {
        joinWaitlist(event, deviceId, null, callback);
    }

    // The following function is from OpenAI GPT 5.1, "How to validate multiple checks before joining waitlist?", 2024-01-15
    /**
     * Joins the waitlist for an event with optional location capture.
     *
     * @param event the event to join
     * @param deviceId the device ID of the entrant
     * @param context context for location capture (null to skip location)
     * @param callback callback for completion
     */
    public void joinWaitlist(Event event, String deviceId, Context context, Callback callback) {
        try {
            ValidationHelper.requireNonNull(event, "event");
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            callback.onResult(JoinResult.failure(e.getMessage()));
            return;
        }

        if (event.getOrganizerId() != null && event.getOrganizerId().equals(deviceId)) {
            callback.onResult(JoinResult.failure("You cannot join your own event"));
            return;
        }

        checkProfileRegistration(deviceId, new EntrantDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isRegistered) {
                if (!isRegistered) {
                    callback.onResult(JoinResult.requiresProfileRegistration());
                    return;
                }
                checkBanStatusAndJoin(event, deviceId, context, callback);
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onResult(JoinResult.failure("Failed to check profile"));
            }
        });
    }

    /**
     * Checks ban status and proceeds with join logic.
     */
    private void checkBanStatusAndJoin(Event event, String deviceId, Context context, Callback callback) {
                entrantDB.isBanned(deviceId, new EntrantDB.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isBanned) {
                        if (isBanned != null && isBanned) {
                            callback.onResult(JoinResult.failure("You are banned from joining events"));
                            return;
                        }
                        checkAlreadyJoinedAndJoin(event, deviceId, context, callback);
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Log.w("JoinWaitlistController", "Failed to check ban status", e);
                        checkAlreadyJoinedAndJoin(event, deviceId, context, callback);
            }
        });
    }

    /**
     * Checks if already joined and proceeds with join logic.
     *
     * @param event the event to join
     * @param deviceId the device ID of the entrant
     * @param context context for location capture
     * @param callback callback for completion
     */
    private void checkAlreadyJoinedAndJoin(Event event, String deviceId, Context context, Callback callback) {
        eventDB.isEntrantOnWaitlist(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean alreadyJoined) {
                if (alreadyJoined) {
                    callback.onResult(JoinResult.failure("Already joined"));
                    return;
                }

                if (!EventValidationHelper.isWithinRegistrationWindow(event)) {
                    callback.onResult(JoinResult.failure("Registration window is closed"));
                    return;
                }

                checkCapacityAndJoin(event, deviceId, context, callback);
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onResult(JoinResult.failure("Failed to check status. Please try again."));
            }
        });
    }

    /**
     * Checks waitlist capacity and proceeds with joining.
     */
    private void checkCapacityAndJoin(Event event, String deviceId, Context context, Callback callback) {
        eventDB.getWaitlistCount(event.getId(), new EventDB.Callback<Integer>() {
                    @Override
            public void onSuccess(Integer waitlistCount) {
                if (waitlistCount == null) {
                    waitlistCount = 0;
                }
                if (!EventValidationHelper.hasWaitlistCapacity(event, waitlistCount)) {
                            callback.onResult(JoinResult.failure("Event is full"));
                            return;
                        }
                performJoin(event, deviceId, context, callback);
                                    }

                                    @Override
                                    public void onError(@NonNull Exception e) {
                callback.onResult(JoinResult.failure("Failed to check availability"));
            }
        });
    }

    /**
     * Performs the actual waitlist join operation with optional location capture.
     */
    private void performJoin(Event event, String deviceId, Context context, Callback callback) {
        if (context != null) {
            captureLocationAndJoin(event, deviceId, context, callback);
        } else {
            joinWaitlistWithLocation(event, deviceId, null, callback);
        }
    }

    /**
     * Captures location and then joins waitlist.
     */
    private void captureLocationAndJoin(Event event, String deviceId, Context context, Callback callback) {
        LocationHelper.getCurrentLocation(context, new LocationHelper.LocationCallback() {
            @Override
            public void onLocation(Location location) {
                com.google.firebase.firestore.GeoPoint geoPoint = null;
                if (location != null) {
                    geoPoint = new com.google.firebase.firestore.GeoPoint(
                        location.getLatitude(), 
                        location.getLongitude()
                    );
                }
                joinWaitlistWithLocation(event, deviceId, geoPoint, callback);
            }
        });
    }

    /**
     * Joins waitlist with location data.
     */
    private void joinWaitlistWithLocation(Event event, String deviceId, 
                                         com.google.firebase.firestore.GeoPoint location,
                                         Callback callback) {
        eventDB.joinWaitlist(event.getId(), deviceId, location, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                updateRegistrationHistory(event.getId(), deviceId);
                                callback.onResult(JoinResult.success("Joined successfully"));
                            }

                            @Override
                            public void onError(@NonNull Exception e) {
                                callback.onResult(JoinResult.failure("Failed to join. Please try again."));
                            }
                        });
                    }

    /**
     * Updates registration history (fire-and-forget operation).
     */
    private void updateRegistrationHistory(String eventId, String deviceId) {
        entrantDB.addEventToEntrant(deviceId, eventId, new EntrantDB.Callback<Void>() {
                    @Override
            public void onSuccess(Void v) {
                // Success - no action needed
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.w("JoinWaitlistController", "Failed to update registration history", e);
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
