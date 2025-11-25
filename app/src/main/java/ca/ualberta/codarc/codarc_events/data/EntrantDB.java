package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.models.Entrant;

/**
 * Handles Entrants collection - profile info and notifications.
 */
public class EntrantDB {

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;

    public EntrantDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    // Backwards compat - just checks if exists
    public void getOrCreateEntrant(String deviceId, Callback<Void> cb) {
        entrantExists(deviceId, new Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean exists) {
                cb.onSuccess(null);
            }

            @Override
            public void onError(@NonNull Exception e) {
                cb.onError(e);
            }
        });
    }

    public void entrantExists(String deviceId, Callback<Boolean> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        db.collection("entrants").document(deviceId)
            .get()
            .addOnSuccessListener(snapshot -> {
                cb.onSuccess(snapshot != null && snapshot.exists());
            })
            .addOnFailureListener(cb::onError);
    }

    // Creates entrant doc (called when user joins first waitlist)
    public void createEntrant(Entrant entrant, Callback<Void> cb) {
        if (entrant == null || entrant.getDeviceId() == null || entrant.getDeviceId().isEmpty()) {
            cb.onError(new IllegalArgumentException("entrant or deviceId is invalid"));
            return;
        }
        
        db.collection("entrants").document(entrant.getDeviceId())
            .set(entrant)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }

    public void getProfile(String deviceId, Callback<Entrant> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        db.collection("entrants").document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        cb.onSuccess(snapshot.toObject(Entrant.class));
                    } else {
                        cb.onSuccess(null);
                    }
                })
                .addOnFailureListener(cb::onError);
    }


    // merge update so we don't lose existing fields
    public void upsertProfile(String deviceId, Entrant entrant, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        if (entrant == null) {
            cb.onError(new IllegalArgumentException("entrant is null"));
            return;
        }
        entrant.setDeviceId(deviceId);
        db.collection("entrants").document(deviceId)
                .set(entrant, SetOptions.merge())
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // Adds notification to entrant's notifications subcollection
    public void addNotification(String deviceId,
                                String eventId,
                                String message,
                                String category,
                                Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        if (message == null || message.isEmpty()) {
            cb.onError(new IllegalArgumentException("message is empty"));
            return;
        }

        DocumentReference entrantRef = db.collection("entrants").document(deviceId);

        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("message", message);
        data.put("category", category);
        data.put("createdAt", System.currentTimeMillis());
        data.put("read", false);

        entrantRef.collection("notifications")
                .add(data)
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void getNotifications(String deviceId, Callback<List<Map<String, Object>>> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }

        db.collection("entrants").document(deviceId)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> notifications = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>(doc.getData());
                            data.put("id", doc.getId());
                            notifications.add(data);
                        }
                    }
                    cb.onSuccess(notifications);
                })
                .addOnFailureListener(cb::onError);
    }

    // Updates notification (read status, response, etc.)
    public void updateNotificationState(String deviceId,
                                        String notificationId,
                                        Map<String, Object> updates,
                                        Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        if (notificationId == null || notificationId.isEmpty()) {
            cb.onError(new IllegalArgumentException("notificationId is empty"));
            return;
        }
        if (updates == null || updates.isEmpty()) {
            cb.onError(new IllegalArgumentException("updates is empty"));
            return;
        }

        DocumentReference notificationRef = db.collection("entrants")
                .document(deviceId)
                .collection("notifications")
                .document(notificationId);

        notificationRef.update(updates)
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // Adds event to entrant's events subcollection
    public void addEventToEntrant(String deviceId, String eventId, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        
        db.collection("entrants").document(deviceId)
            .collection("events").document(eventId)
            .set(data)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }

    public void getEntrantEvents(String deviceId, Callback<List<String>> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        db.collection("entrants").document(deviceId)
            .collection("events")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<String> eventIds = new ArrayList<>();
                if (querySnapshot != null) {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String eventId = doc.getString("eventId");
                        if (eventId != null) {
                            eventIds.add(eventId);
                        }
                    }
                }
                cb.onSuccess(eventIds);
            })
            .addOnFailureListener(cb::onError);
    }

    /**
     * Retrieves the list of event IDs from the entrant's registration history.
     * Queries the events subcollection under entrants/{deviceId}/events.
     *
     * @param deviceId the device ID of the entrant
     * @param cb       callback that receives the list of event IDs
     */
    public void getRegistrationHistory(String deviceId, Callback<List<String>> cb) {
        getEntrantEvents(deviceId, cb);
    }

    public void removeEventFromEntrant(String deviceId, String eventId, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        
        db.collection("entrants").document(deviceId)
            .collection("events").document(eventId)
            .delete()
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }

    /**
     * Deletes all events from the entrant's events subcollection.
     * Used when admin removes a profile to clean up registration history.
     * Handles Firestore batch write limit (500 operations per batch) by batching deletions.
     *
     * @param deviceId the device ID of the entrant
     * @param cb callback for completion
     */
    public void deleteAllEntrantEvents(String deviceId, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        db.collection("entrants").document(deviceId)
            .collection("events")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot == null || querySnapshot.isEmpty()) {
                    cb.onSuccess(null);
                    return;
                }
                
                // Collect all documents to delete
                List<QueryDocumentSnapshot> docs = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    docs.add(doc);
                }
                
                // Delete in batches to respect Firestore's 500 operation limit
                deleteInBatches(docs, 0, cb);
            })
            .addOnFailureListener(cb::onError);
    }

    /**
     * Recursively deletes documents in batches to respect Firestore's batch write limit.
     * Firestore allows maximum 500 operations per batch write.
     *
     * @param docs list of documents to delete
     * @param startIndex starting index for this batch
     * @param cb callback for completion
     */
    private void deleteInBatches(List<QueryDocumentSnapshot> docs, int startIndex, Callback<Void> cb) {
        if (startIndex >= docs.size()) {
            cb.onSuccess(null);
            return;
        }
        
        WriteBatch batch = db.batch();
        int endIndex = Math.min(startIndex + 500, docs.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            batch.delete(docs.get(i).getReference());
        }
        
        batch.commit()
            .addOnSuccessListener(unused -> {
                // Continue with next batch if there are more documents
                deleteInBatches(docs, endIndex, cb);
            })
            .addOnFailureListener(cb::onError);
    }
    
    // Bans/unbans an entrant (admin only)
    public void setBannedStatus(String deviceId, boolean banned, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        db.collection("entrants").document(deviceId)
            .update("banned", banned)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    public void isBanned(String deviceId, Callback<Boolean> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        getProfile(deviceId, new Callback<Entrant>() {
            @Override
            public void onSuccess(Entrant entrant) {
                if (entrant != null) {
                    cb.onSuccess(entrant.isBanned());
                } else {
                    cb.onSuccess(false); // Not an entrant, so not banned as entrant
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                // If entrant doesn't exist, they're not banned
                cb.onSuccess(false);
            }
        });
    }

    /**
     * Clears profile data but keeps the document (don't delete it).
     * Also sets the banned flag to true to prevent re-registration.
     *
     * @param deviceId the device ID of the entrant
     * @param cb callback for completion
     */
    public void deleteProfile(String deviceId, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }

        // Get existing entrant to preserve createdAtUtc
        getProfile(deviceId, new Callback<Entrant>() {
            @Override
            public void onSuccess(Entrant existing) {
                long createdAtUtc = existing != null ? existing.getCreatedAtUtc() : System.currentTimeMillis();
                Entrant cleared = new Entrant(deviceId, "", createdAtUtc);
                cleared.setEmail("");
                cleared.setPhone("");
                cleared.setIsRegistered(false);
                cleared.setBanned(true); // Set banned flag to prevent re-registration
                upsertProfile(deviceId, cleared, cb);
            }

            @Override
            public void onError(@NonNull Exception e) {
                // Entrant doesn't exist, create new one with cleared data and banned flag
                Entrant cleared = new Entrant(deviceId, "", System.currentTimeMillis());
                cleared.setEmail("");
                cleared.setPhone("");
                cleared.setIsRegistered(false);
                cleared.setBanned(true); // Set banned flag to prevent re-registration
                upsertProfile(deviceId, cleared, cb);
            }
        });
    }

    /**
     * Retrieves all entrant profiles from Firestore.
     * Used by admin to display list of all entrants.
     *
     * @param cb callback that receives list of all entrants
     */
    public void getAllEntrants(Callback<List<Entrant>> cb) {
        db.collection("entrants")
            .get()
            .addOnSuccessListener(querySnapshot -> {
                List<Entrant> entrants = new ArrayList<>();
                if (querySnapshot != null) {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Entrant entrant = doc.toObject(Entrant.class);
                        if (entrant != null) {
                            entrants.add(entrant);
                        }
                    }
                }
                cb.onSuccess(entrants);
            })
            .addOnFailureListener(cb::onError);
    }

    /**
     * Removes a notification from the entrant's notifications subcollection.
     * Used for lazy cleanup of notifications for deleted events.
     *
     * @param deviceId the device ID of the entrant
     * @param notificationId the notification ID to remove
     * @param cb callback for completion
     */
    public void removeNotification(String deviceId, String notificationId, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        if (notificationId == null || notificationId.isEmpty()) {
            cb.onError(new IllegalArgumentException("notificationId is empty"));
            return;
        }
        
        db.collection("entrants").document(deviceId)
            .collection("notifications").document(notificationId)
            .delete()
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }

}

