package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import ca.ualberta.codarc.codarc_events.models.User;

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
     * If it doesn't exist, creates one with all role flags set to false.
     * 
     * @param deviceId the unique device identifier
     * @param cb callback invoked once operation completes
     */
    public void ensureUserExists(String deviceId, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
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
                // User already exists
                cb.onSuccess(null);
            } else {
                // Create new user with default flags
                User newUser = new User(deviceId);
                userRef.set(newUser)
                    .addOnSuccessListener(unused -> cb.onSuccess(null))
                    .addOnFailureListener(cb::onError);
            }
        });
    }
    
    public void getUser(String deviceId, Callback<User> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
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
    
    // Sets isEntrant flag (called when user joins waitlist)
    public void setEntrantRole(String deviceId, boolean isEntrant, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        db.collection("users").document(deviceId)
            .update("isEntrant", isEntrant)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    // Sets isOrganizer flag (called when user creates event)
    public void setOrganizerRole(String deviceId, boolean isOrganizer, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        db.collection("users").document(deviceId)
            .update("isOrganizer", isOrganizer)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    public void setAdminRole(String deviceId, boolean isAdmin, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        db.collection("users").document(deviceId)
            .update("isAdmin", isAdmin)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    public void updateUser(User user, Callback<Void> cb) {
        if (user == null || user.getDeviceId() == null || user.getDeviceId().isEmpty()) {
            cb.onError(new IllegalArgumentException("user or deviceId is invalid"));
            return;
        }
        
        db.collection("users").document(user.getDeviceId())
            .set(user, SetOptions.merge())
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
}

