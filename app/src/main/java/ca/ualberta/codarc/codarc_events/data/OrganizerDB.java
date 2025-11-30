package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.models.Organizer;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Handles Organizers collection - minimal, just deviceId and events list.
 */
public class OrganizerDB {
    
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception e);
    }
    
    private final FirebaseFirestore db;
    
    public OrganizerDB() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    /**
     * Creates an organizer document.
     *
     * @param deviceId the device ID
     * @param cb callback for completion
     */
    public void createOrganizer(String deviceId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        Organizer organizer = new Organizer(deviceId);
        db.collection("organizers").document(deviceId)
            .set(organizer)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    public void organizerExists(String deviceId, Callback<Boolean> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        db.collection("organizers").document(deviceId)
            .get()
            .addOnSuccessListener(snapshot -> {
                cb.onSuccess(snapshot != null && snapshot.exists());
            })
            .addOnFailureListener(cb::onError);
    }
    
    /**
     * Adds an event to the organizer's events subcollection.
     *
     * @param deviceId the device ID
     * @param eventId the event ID
     * @param cb callback for completion
     */
    public void addEventToOrganizer(String deviceId, String eventId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId);
        
        db.collection("organizers").document(deviceId)
            .collection("events").document(eventId)
            .set(data)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    public void getOrganizerEvents(String deviceId, Callback<List<String>> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        db.collection("organizers").document(deviceId)
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
    
    public void removeEventFromOrganizer(String deviceId, String eventId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        db.collection("organizers").document(deviceId)
            .collection("events").document(eventId)
            .delete()
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    /**
     * Sets the banned status of an organizer.
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
        
        db.collection("organizers").document(deviceId)
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
        
        db.collection("organizers").document(deviceId)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot != null && snapshot.exists()) {
                    Boolean banned = snapshot.getBoolean("banned");
                    cb.onSuccess(banned != null && banned);
                } else {
                    cb.onSuccess(false);
                }
            })
            .addOnFailureListener(cb::onError);
    }
}

