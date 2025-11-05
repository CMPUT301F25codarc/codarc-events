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
 * Small helper around Firestore operations related to entrants/profiles.
 *
 * The app keeps one profile document per device under the `profiles` collection
 * using the local deviceId as the document id. This class exposes only the
 * reads/writes we need for Stage 0/1 flows – nothing fancy, just straight calls.
 */
public class EntrantDB {

    /**
     * Minimal async callback used across the data layer.
     */
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;

    public EntrantDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Backwards-compatible alias used by early screens. Creates the profile doc if missing.
     * This keeps older calls working while we migrate to the `profiles` collection.
     */
    public void getOrCreateEntrant(String deviceId, Callback<Void> cb) {
        ensureProfileDefaults(deviceId, cb);
    }

    /**
     * Ensures a `profiles/<deviceId>` document exists.
     * If it doesn't, writes a document with default fields and flags.
     */
    public void ensureProfileDefaults(String deviceId, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }

        DocumentReference docRef = db.collection("profiles").document(deviceId);
        docRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                cb.onError(task.getException() != null ? task.getException() : new RuntimeException("Unknown error"));
                return;
            }
            DocumentSnapshot snapshot = task.getResult();
            if (snapshot != null && snapshot.exists()) {
                cb.onSuccess(null);
            } else {
                // Constructor already sets all defaults (empty strings, false flags)
                Entrant defaultEntrant = new Entrant(deviceId, "", System.currentTimeMillis());
                Task<Void> setTask = docRef.set(defaultEntrant);
                setTask.addOnSuccessListener(unused -> cb.onSuccess(null))
                        .addOnFailureListener(cb::onError);
            }
        });
    }

    public void getProfile(String deviceId, Callback<Entrant> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        db.collection("profiles").document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        Entrant entrant = snapshot.toObject(Entrant.class);
                        cb.onSuccess(entrant);
                    } else {
                        cb.onError(new RuntimeException("Profile not found"));
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
        db.collection("profiles").document(deviceId)
                .set(entrant, SetOptions.merge())
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Appends a notification entry for the given entrant profile.
     *
     * <p>The notification is stored under <code>profiles/&lt;deviceId&gt;/notifications</code>
     * with a generated id so multiple notifications can coexist. Each entry records the
     * associated event, message, category, creation timestamp, and read state.</p>
     *
     * @param deviceId the entrant's device identifier
     * @param eventId the related event identifier
     * @param message the human-readable notification body
     * @param category short label (e.g., "winner" or "cancelled") for filtering
     * @param cb callback invoked once the write completes
     */
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

        DocumentReference profileRef = db.collection("profiles").document(deviceId);

        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        data.put("message", message);
        data.put("category", category);
        data.put("createdAt", System.currentTimeMillis());
        data.put("read", false);

        profileRef.collection("notifications")
                .add(data)
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Retrieves all notifications for an entrant ordered by most recent first.
     */
    public void getNotifications(String deviceId, Callback<List<Map<String, Object>>> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }

        db.collection("profiles").document(deviceId)
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
     * Updates arbitrary state on a notification document. Used by controllers
     * to persist read/response fields after an entrant acts on an invite.
     */
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

        DocumentReference notificationRef = db.collection("profiles")
                .document(deviceId)
                .collection("notifications")
                .document(notificationId);

        notificationRef.update(updates)
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Clears an entrant's profile information and sets registration status to false.
     *
     * <p>Rather than deleting the profile document, this method clears the user's
     * personal information (name, email, phone) and sets isRegistered to false.
     * This preserves the device identity document and prevents issues when
     * checking profile registration status during waitlist operations.</p>
     *
     * @param deviceId The unique device identifier whose profile should be cleared.
     * @param cb       Callback invoked on successful update or with an exception if it fails.
     */
    public void deleteProfile(String deviceId, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }

        // Get existing profile to preserve createdAtUtc
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
                // Profile doesn't exist, create new one
                Entrant cleared = new Entrant(deviceId, "", System.currentTimeMillis());
                cleared.setEmail("");
                cleared.setPhone("");
                cleared.setIsRegistered(false);
                upsertProfile(deviceId, cleared, cb);
            }
        });
    }

}

