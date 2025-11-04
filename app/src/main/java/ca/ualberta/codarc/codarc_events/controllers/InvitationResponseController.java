package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;

/**
 * Encapsulates the business logic for entrant invitation responses.
 *
 * <p>Activities delegate to this controller so that the UI layer stays free
 * from Firestore specifics. The controller coordinates the required writes:</p>
 * <ol>
 *     <li>Update the entrant's enrolled status within the event document.</li>
 *     <li>Persist the entrant's response on their notification record.</li>
 * </ol>
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
                        cb.onSuccess();
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
}