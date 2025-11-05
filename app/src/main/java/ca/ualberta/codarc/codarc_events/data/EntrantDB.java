package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

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
                        Entrant entrant = snapshot.toObject(Entrant.class);
                        cb.onSuccess(entrant);
                    } else {
                        cb.onError(new RuntimeException("Entrant not found"));
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

    // Clears profile data but keeps the document (don't delete it)
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
                upsertProfile(deviceId, cleared, cb);
            }

            @Override
            public void onError(@NonNull Exception e) {
                // Entrant doesn't exist, create new one with cleared data
                Entrant cleared = new Entrant(deviceId, "", System.currentTimeMillis());
                cleared.setEmail("");
                cleared.setPhone("");
                cleared.setIsRegistered(false);
                upsertProfile(deviceId, cleared, cb);
            }
        });
    }

}

