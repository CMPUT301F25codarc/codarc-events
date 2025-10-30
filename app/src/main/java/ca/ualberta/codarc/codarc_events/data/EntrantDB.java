package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * Builds an EntrantDB bound to the default Firestore instance.
     */
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
                Map<String, Object> payload = new HashMap<>();
                payload.put("deviceId", deviceId);
                payload.put("createdAtUtc", System.currentTimeMillis());
                payload.put("name", "");
                payload.put("email", "");
                payload.put("phone", "");
                payload.put("is_registered", false);
                payload.put("is_organizer", "");
                payload.put("is_admin", false);
                Task<Void> setTask = docRef.set(payload);
                setTask.addOnSuccessListener(unused -> cb.onSuccess(null))
                        .addOnFailureListener(cb::onError);
            }
        });
    }

    /**
     * Fetches the profile snapshot for the given device id.
     */
    public void getProfile(String deviceId, Callback<DocumentSnapshot> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        db.collection("profiles").document(deviceId)
                .get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(cb::onError);
    }

    /**
     * Merges the provided fields into `profiles/<deviceId>`.
     * Caller decides which keys to set; we always keep the deviceId mirrored.
     */
    public void upsertProfile(String deviceId, Map<String, Object> fields, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        if (fields == null) fields = new HashMap<>();
        fields.put("deviceId", deviceId);
        db.collection("profiles").document(deviceId)
                .set(fields, SetOptions.merge())
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
}


