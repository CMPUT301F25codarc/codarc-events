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
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Handles Entrants collection - profile info and notifications.
 */
public class EntrantDB {

    private static final int BATCH_SIZE = 500;

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;

    public EntrantDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Checks if entrant exists.
     *
     * @param deviceId the device ID
     * @param cb callback for completion
     */
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
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        db.collection("entrants").document(deviceId)
            .get()
            .addOnSuccessListener(snapshot -> {
                cb.onSuccess(snapshot != null && snapshot.exists());
            })
            .addOnFailureListener(cb::onError);
    }

    /**
     * Creates an entrant document.
     *
     * @param entrant the entrant to create
     * @param cb callback for completion
     */
    public void createEntrant(Entrant entrant, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonNull(entrant, "entrant");
            ValidationHelper.requireNonEmpty(entrant.getDeviceId(), "entrant.deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        db.collection("entrants").document(entrant.getDeviceId())
            .set(entrant)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }

    public void getProfile(String deviceId, Callback<Entrant> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
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

    /**
     * Updates or creates an entrant profile.
     *
     * @param deviceId the device ID
     * @param entrant the entrant data
     * @param cb callback for completion
     */
    public void upsertProfile(String deviceId, Entrant entrant, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
            ValidationHelper.requireNonNull(entrant, "entrant");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        entrant.setDeviceId(deviceId);
        db.collection("entrants").document(deviceId)
                .set(entrant, SetOptions.merge())
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Adds a notification to the entrant's notifications subcollection.
     *
     * @param deviceId the device ID
     * @param eventId the event ID
     * @param message the notification message
     * @param category the notification category
     * @param cb callback for completion
     */
    public void addNotification(String deviceId,
                                String eventId,
                                String message,
                                String category,
                                Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
            ValidationHelper.requireNonEmpty(message, "message");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
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
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
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

    /**
     * Updates notification state.
     *
     * @param deviceId the device ID
     * @param notificationId the notification ID
     * @param updates map of fields to update
     * @param cb callback for completion
     */
    public void updateNotificationState(String deviceId,
                                        String notificationId,
                                        Map<String, Object> updates,
                                        Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
            ValidationHelper.requireNonEmpty(notificationId, "notificationId");
            ValidationHelper.requireNonNull(updates, "updates");
            if (updates.isEmpty()) {
                throw new IllegalArgumentException("updates cannot be empty");
            }
        } catch (IllegalArgumentException e) {
            cb.onError(e);
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

    /**
     * Adds an event to the entrant's events subcollection.
     *
     * @param deviceId the device ID
     * @param eventId the event ID
     * @param cb callback for completion
     */
    public void addEventToEntrant(String deviceId, String eventId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
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
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
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
     * Gets the entrant's registration history.
     *
     * @param deviceId the device ID of the entrant
     * @param cb callback that receives the list of event IDs
     */
    public void getRegistrationHistory(String deviceId, Callback<List<String>> cb) {
        getEntrantEvents(deviceId, cb);
    }

    public void removeEventFromEntrant(String deviceId, String eventId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
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
     *
     * @param deviceId the device ID of the entrant
     * @param cb callback for completion
     */
    public void deleteAllEntrantEvents(String deviceId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
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
                
                List<QueryDocumentSnapshot> docs = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    docs.add(doc);
                }
                
                deleteInBatches(docs, 0, cb);
            })
            .addOnFailureListener(cb::onError);
    }

    /**
     * Deletes documents in batches.
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
        int endIndex = Math.min(startIndex + BATCH_SIZE, docs.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            batch.delete(docs.get(i).getReference());
        }
        
        batch.commit()
            .addOnSuccessListener(unused -> deleteInBatches(docs, endIndex, cb))
            .addOnFailureListener(cb::onError);
    }
    
    /**
     * Sets the banned status of an entrant.
     *
     * @param deviceId the device ID
     * @param banned true to ban, false to unban
     * @param cb callback for completion
     */
    public void setBannedStatus(String deviceId, boolean banned, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        db.collection("entrants").document(deviceId)
            .update("banned", banned)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    public void isBanned(String deviceId, Callback<Boolean> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        getProfile(deviceId, new Callback<Entrant>() {
            @Override
            public void onSuccess(Entrant entrant) {
                cb.onSuccess(entrant != null && entrant.isBanned());
            }

            @Override
            public void onError(@NonNull Exception e) {
                cb.onSuccess(false);
            }
        });
    }

    /**
     * Clears profile data.
     *
     * @param deviceId the device ID of the entrant
     * @param shouldBan true to set banned flag, false to keep it as is
     * @param cb callback for completion
     */
    public void deleteProfile(String deviceId, boolean shouldBan, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        getProfile(deviceId, new Callback<Entrant>() {
            @Override
            public void onSuccess(Entrant existing) {
                long createdAtUtc = existing != null ? existing.getCreatedAtUtc() : System.currentTimeMillis();
                Entrant cleared = new Entrant(deviceId, "", createdAtUtc);
                cleared.setEmail("");
                cleared.setPhone("");
                cleared.setIsRegistered(false);
                cleared.setBanned(shouldBan);
                upsertProfile(deviceId, cleared, cb);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Entrant cleared = new Entrant(deviceId, "", System.currentTimeMillis());
                cleared.setEmail("");
                cleared.setPhone("");
                cleared.setIsRegistered(false);
                cleared.setBanned(shouldBan);
                upsertProfile(deviceId, cleared, cb);
            }
        });
    }

    /**
     * Gets all entrant profiles.
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
     *
     * @param deviceId the device ID of the entrant
     * @param notificationId the notification ID to remove
     * @param cb callback for completion
     */
    public void removeNotification(String deviceId, String notificationId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
            ValidationHelper.requireNonEmpty(notificationId, "notificationId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        db.collection("entrants").document(deviceId)
            .collection("notifications").document(notificationId)
            .delete()
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }

}

