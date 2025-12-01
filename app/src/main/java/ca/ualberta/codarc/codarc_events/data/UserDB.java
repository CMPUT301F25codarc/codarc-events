package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import ca.ualberta.codarc.codarc_events.models.User;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Handles Users collection operations in Firestore.
 * Users start with no roles - flags get set when they do actions.
 */
public class UserDB {
    
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception e);
    }
    
    private final FirebaseFirestore db;
    
    public UserDB() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Ensures a User document exists for the given device ID.
     *
     * @param deviceId the unique device identifier
     * @param cb callback invoked once operation completes
     */
    public void ensureUserExists(String deviceId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        DocumentReference userRef = db.collection("users").document(deviceId);
        userRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                cb.onError(task.getException() != null ? task.getException() : new RuntimeException("Unknown error"));
                return;
            }
            
            DocumentSnapshot snapshot = task.getResult();
            if (snapshot != null && snapshot.exists()) {
                cb.onSuccess(null);
            } else {
                User newUser = new User(deviceId);
                userRef.set(newUser)
                    .addOnSuccessListener(unused -> cb.onSuccess(null))
                    .addOnFailureListener(cb::onError);
            }
        });
    }
    
    public void getUser(String deviceId, Callback<User> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        db.collection("users").document(deviceId)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot != null && snapshot.exists()) {
                    User user = snapshot.toObject(User.class);
                    cb.onSuccess(user);
                } else {
                    cb.onError(new RuntimeException("User not found"));
                }
            })
            .addOnFailureListener(cb::onError);
    }
    
    /**
     * Sets a role flag for a user.
     *
     * @param deviceId the device ID
     * @param roleField the field name to update (e.g., "isEntrant", "isOrganizer", "isAdmin")
     * @param value the value to set
     * @param cb callback for completion
     */
    private void setRole(String deviceId, String roleField, boolean value, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        db.collection("users").document(deviceId)
            .update(roleField, value)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    /**
     * Sets the isEntrant flag.
     *
     * @param deviceId the device ID
     * @param isEntrant true if user is an entrant
     * @param cb callback for completion
     */
    public void setEntrantRole(String deviceId, boolean isEntrant, Callback<Void> cb) {
        setRole(deviceId, "isEntrant", isEntrant, cb);
    }
    
    /**
     * Sets the isOrganizer flag.
     *
     * @param deviceId the device ID
     * @param isOrganizer true if user is an organizer
     * @param cb callback for completion
     */
    public void setOrganizerRole(String deviceId, boolean isOrganizer, Callback<Void> cb) {
        setRole(deviceId, "isOrganizer", isOrganizer, cb);
    }
}

