package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Handles invitation accept/decline responses.
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
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
            ValidationHelper.requireNonEmpty(notificationId, "notificationId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        eventDB.setEnrolledStatus(eventId, deviceId, enroll, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                updateNotificationAndHandleResponse(eventId, deviceId, notificationId, enroll, response, cb);
            }

            @Override
            public void onError(@NonNull Exception e) {
                cb.onError(e);
            }
        });
    }

    /**
     * Updates notification state and handles response logic.
     */
    private void updateNotificationAndHandleResponse(String eventId, String deviceId, String notificationId,
                                                    boolean enroll, String response, ResponseCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("read", true);
        updates.put("response", response);
        updates.put("respondedAt", System.currentTimeMillis());

        entrantDB.updateNotificationState(deviceId, notificationId, updates, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void ignore) {
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

    // The following function is from Anthropic Claude Sonnet 4.5, "How to automatically select replacement when entrant declines?", 2024-01-15
    /**
     * Handles automatic reselection when an entrant declines an invitation.
     *
     * @param eventId          the event ID
     * @param declinedEntrantId the device ID of the entrant who declined
     * @param cb               callback for completion
     */
    private void handleAutomaticReselection(String eventId, String declinedEntrantId, ResponseCallback cb) {
        eventDB.getReplacementPool(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> pool) {
                if (pool != null && !pool.isEmpty()) {
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
                    tryWaitlistSelection(eventId, declinedEntrantId, cb);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("InvitationResponseController", "Failed to get replacement pool", e);
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
                    logDeclineOnly(eventId, declinedEntrantId, cb);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("InvitationResponseController", "Failed to get waitlist", e);
                logDeclineOnly(eventId, declinedEntrantId, cb);
            }
        });
    }

    /**
     * Promotes a replacement entrant to winners and sends notification.
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
                notifyReplacement(eventId, declinedEntrantId, replacementId, source, cb);
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("InvitationResponseController", "Failed to promote replacement", e);
                logDeclineReplacement(eventId, declinedEntrantId, replacementId, source, false, cb);
            }
        };

        if ("replacementPool".equals(source)) {
            eventDB.markReplacement(eventId, replacementId, promoteCallback);
        } else {
            eventDB.promoteFromWaitlist(eventId, replacementId, promoteCallback);
        }
    }

    /**
     * Sends notification to replacement and logs the decline.
     */
    private void notifyReplacement(String eventId, String declinedEntrantId, String replacementId,
                                  String source, ResponseCallback cb) {
        String message = "Congratulations! You've been selected as a replacement. Proceed to signup.";
        entrantDB.addNotification(replacementId, eventId, message, "winner", new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                logDeclineReplacement(eventId, declinedEntrantId, replacementId, source, true, cb);
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("InvitationResponseController", "Failed to notify replacement", e);
                logDeclineReplacement(eventId, declinedEntrantId, replacementId, source, false, cb);
            }
        });
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
     * Logs a decline and replacement.
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
                        android.util.Log.e("InvitationResponseController", "Failed to log decline replacement", e);
                        cb.onSuccess();
                    }
                });
    }
}