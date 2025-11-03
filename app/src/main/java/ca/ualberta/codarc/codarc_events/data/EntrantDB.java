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


