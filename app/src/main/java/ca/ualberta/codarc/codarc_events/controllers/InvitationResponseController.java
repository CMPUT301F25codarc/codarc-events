package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;

/**
 * Handles invitation accept/decline responses.
 * Updates event status and notification.
 * Automatically reselects replacements when invitations are declined.
 */
public class InvitationResponseController {

    public interface ResponseCallback {
        void onSuccess();
        void onError(@NonNull Exception e);
    }

    private final EventDB eventDB;
    private final EntrantDB entrantDB;

    public InvitationResponseController(EventDB eventDB, EntrantDB entrantDB) {
        this.eventDB = eventDB;
        this.entrantDB = entrantDB;
    }

    public void acceptInvitation(String eventId,
                                 String deviceId,
                                 String notificationId,
                                 ResponseCallback cb) {
        respondToInvitation(eventId, deviceId, notificationId, true, "accepted", cb);
    }

    public void declineInvitation(String eventId,
                                  String deviceId,
                                  String notificationId,
                                  ResponseCallback cb) {
        respondToInvitation(eventId, deviceId, notificationId, false, "declined", cb);
    }

    private void respondToInvitation(String eventId,
                                     String deviceId,
                                     String notificationId,
                                     boolean enroll,
                                     String response,
                                     ResponseCallback cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        if (notificationId == null || notificationId.isEmpty()) {
            cb.onError(new IllegalArgumentException("notificationId is empty"));
            return;
        }

        eventDB.setEnrolledStatus(eventId, deviceId, enroll, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("read", true);
                updates.put("response", response);
                updates.put("respondedAt", System.currentTimeMillis());

                entrantDB.updateNotificationState(deviceId, notificationId, updates, new EntrantDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void ignore) {
                        // If declined, trigger automatic reselection
                        if (!enroll) {
                            handleAutomaticReselection(eventId, deviceId, cb);
                        } else {
                            cb.onSuccess();
                        }
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                cb.onError(e);
            }
        });
    }

    /**
     * Handles automatic reselection when an entrant declines an invitation.
     * Tries replacement pool first, then waitlist. Promotes replacement and sends notification.
     * Decline operation always succeeds even if reselection fails.
     *
     * @param eventId          the event ID
     * @param declinedEntrantId the device ID of the entrant who declined
     * @param cb               callback for completion
     */
    private void handleAutomaticReselection(String eventId, String declinedEntrantId, ResponseCallback cb) {
        // Try replacement pool first
        eventDB.getReplacementPool(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> pool) {
                if (pool != null && !pool.isEmpty()) {
                    // Use first entry from replacement pool
                    Object deviceIdObj = pool.get(0).get("deviceId");
                    if (deviceIdObj == null || !(deviceIdObj instanceof String)) {
                        android.util.Log.w("InvitationResponseController", "Invalid deviceId in replacement pool, trying waitlist");
                        tryWaitlistSelection(eventId, declinedEntrantId, cb);
                        return;
                    }
                    String replacementId = (String) deviceIdObj;
                    String source = "replacementPool";
                    promoteAndNotifyReplacement(eventId, declinedEntrantId, replacementId, source, cb);
                } else {
                    // Pool empty, try waitlist
                    tryWaitlistSelection(eventId, declinedEntrantId, cb);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("InvitationResponseController", "Failed to get replacement pool", e);
                // Try waitlist as fallback
                tryWaitlistSelection(eventId, declinedEntrantId, cb);
            }
        });
    }

    /**
     * Attempts to select a replacement from the waitlist.
     *
     * @param eventId          the event ID
     * @param declinedEntrantId the device ID of the entrant who declined
     * @param cb               callback for completion
     */
    private void tryWaitlistSelection(String eventId, String declinedEntrantId, ResponseCallback cb) {
        eventDB.getWaitlist(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> waitlist) {
                if (waitlist != null && !waitlist.isEmpty()) {
                    // Randomly select from waitlist
                    Collections.shuffle(waitlist);
                    Object deviceIdObj = waitlist.get(0).get("deviceId");
                    if (deviceIdObj == null || !(deviceIdObj instanceof String)) {
                        android.util.Log.w("InvitationResponseController", "Invalid deviceId in waitlist, no replacement available");
                        logDeclineOnly(eventId, declinedEntrantId, cb);
                        return;
                    }
                    String replacementId = (String) deviceIdObj;
                    String source = "waitlist";
                    promoteAndNotifyReplacement(eventId, declinedEntrantId, replacementId, source, cb);
                } else {
                    // No replacement available
                    logDeclineOnly(eventId, declinedEntrantId, cb);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("InvitationResponseController", "Failed to get waitlist", e);
                // Log decline without replacement
                logDeclineOnly(eventId, declinedEntrantId, cb);
            }
        });
    }

    /**
     * Promotes a replacement entrant to winners and sends notification.
     * Handles both pool and waitlist sources.
     *
     * @param eventId          the event ID
     * @param declinedEntrantId the device ID of the entrant who declined
     * @param replacementId   the device ID of the replacement
     * @param source          source of replacement ("replacementPool" or "waitlist")
     * @param cb              callback for completion
     */
    private void promoteAndNotifyReplacement(String eventId,
                                             String declinedEntrantId,
                                             String replacementId,
                                             String source,
                                             ResponseCallback cb) {
        EventDB.Callback<Void> promoteCallback = new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void ignore) {
                // Send notification to replacement
                String message = "Congratulations! You've been selected as a replacement. Proceed to signup.";
                entrantDB.addNotification(replacementId, eventId, message, "winner", new EntrantDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void value) {
                        // Log the decline and replacement
                        logDeclineReplacement(eventId, declinedEntrantId, replacementId, source, true, cb);
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        android.util.Log.e("InvitationResponseController", "Failed to notify replacement", e);
                        // Log decline with notification failure
                        logDeclineReplacement(eventId, declinedEntrantId, replacementId, source, false, cb);
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("InvitationResponseController", "Failed to promote replacement", e);
                // Log decline with promotion failure
                logDeclineReplacement(eventId, declinedEntrantId, replacementId, source, false, cb);
            }
        };

        // Promote based on source
        if ("replacementPool".equals(source)) {
            eventDB.markReplacement(eventId, replacementId, promoteCallback);
        } else {
            // Promote from waitlist
            eventDB.promoteFromWaitlist(eventId, replacementId, promoteCallback);
        }
    }

    /**
     * Logs a decline when no replacement is available.
     *
     * @param eventId          the event ID
     * @param declinedEntrantId the device ID of the entrant who declined
     * @param cb              callback for completion
     */
    private void logDeclineOnly(String eventId, String declinedEntrantId, ResponseCallback cb) {
        logDeclineReplacement(eventId, declinedEntrantId, null, null, false, cb);
    }

    /**
     * Logs a decline and replacement, then completes the callback.
     * Errors in logging don't fail the decline operation.
     *
     * @param eventId            the event ID
     * @param declinedEntrantId  the device ID of the entrant who declined
     * @param replacementId      the device ID of the replacement (null if none)
     * @param source            source of replacement (null if none)
     * @param replacementNotified whether replacement was notified
     * @param cb                callback for completion
     */
    private void logDeclineReplacement(String eventId,
                                       String declinedEntrantId,
                                       String replacementId,
                                       String source,
                                       boolean replacementNotified,
                                       ResponseCallback cb) {
        eventDB.logDeclineReplacement(eventId, declinedEntrantId, replacementId, source, replacementNotified,
                new EventDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void ignore) {
                        cb.onSuccess();
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        // Log error but don't fail the decline operation
                        android.util.Log.e("InvitationResponseController", "Failed to log decline replacement", e);
                        cb.onSuccess();
                    }
                });
    }
}