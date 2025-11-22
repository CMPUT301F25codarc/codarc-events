package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.util.Log;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Handles leaving waitlists.
 */
public class LeaveWaitlistController {

    public static class LeaveResult {
        private final boolean success;
        private final String message;

        private LeaveResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static LeaveResult success(String message) {
            return new LeaveResult(true, message);
        }

        public static LeaveResult failure(String message) {
            return new LeaveResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    private final EventDB eventDB;
    private final EntrantDB entrantDB;

    public LeaveWaitlistController(EventDB eventDB) {
        this.eventDB = eventDB;
        this.entrantDB = new EntrantDB();
    }

    public LeaveWaitlistController(EventDB eventDB, EntrantDB entrantDB) {
        this.eventDB = eventDB;
        this.entrantDB = entrantDB;
    }

    public void leaveWaitlist(Event event, String deviceId, Callback callback) {
        if (event == null) {
            callback.onResult(LeaveResult.failure("Event is required"));
            return;
        }
        if (deviceId == null || deviceId.isEmpty()) {
            callback.onResult(LeaveResult.failure("Device ID is required"));
            return;
        }

        // Check if actually on waitlist
        eventDB.isEntrantOnWaitlist(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isOnWaitlist) {
                if (!isOnWaitlist) {
                    callback.onResult(LeaveResult.failure("You are not registered for this event"));
                    return;
                }

                // Perform leave operation
                eventDB.leaveWaitlist(event.getId(), deviceId, new EventDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void value) {
                        // Remove from registration history (graceful degradation - don't fail leave if this fails)
                        entrantDB.removeEventFromEntrant(deviceId, event.getId(), new EntrantDB.Callback<Void>() {
                            @Override
                            public void onSuccess(Void v) {
                                // History updated successfully
                            }

                            @Override
                            public void onError(@NonNull Exception e) {
                                // Log but don't fail the leave operation
                                Log.w("LeaveWaitlistController", "Failed to update registration history", e);
                            }
                        });
                        callback.onResult(LeaveResult.success("You have left this event"));
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        callback.onResult(LeaveResult.failure("Failed to leave. Please try again."));
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onResult(LeaveResult.failure("Failed to check status. Please try again."));
            }
        });
    }

    public interface Callback {
        void onResult(LeaveResult result);
    }
}
