package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

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
     * Checks whether a profile document already exists for the given device ID.
     *
     * <p>This is primarily used by UI components (e.g., JoinWaitlistController or
     * ProfileActivity) to determine whether the current device has already
     * completed the signup flow.</p>
     *
     * @param deviceId The unique device identifier that serves as the Firestore document key.
     * @param cb       Callback invoked with {@code true} if the profile exists,
     *                 {@code false} if it does not, or {@link Callback#onError(Exception)}
     *                 if the lookup fails.
     */
    public void profileExists(String deviceId, Callback<Boolean> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        db.collection("profiles").document(deviceId).get()
                .addOnSuccessListener(doc -> cb.onSuccess(doc.exists()))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Deletes an entrant profile document for the specified device ID.
     *
     * <p>If the profile document does not exist, this operation still succeeds
     * (idempotent delete). This method is typically called when a user requests
     * to permanently remove their profile from the app through the Profile screen.</p>
     *
     * @param deviceId The unique device identifier whose profile document should be deleted.
     * @param cb       Callback invoked on successful deletion or with an exception if it fails.
     */
    public void deleteProfile(String deviceId, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        db.collection("profiles").document(deviceId)
                .delete()
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
}