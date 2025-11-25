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
    
    // Creates organizer doc (called when user creates first event)
    public void createOrganizer(String deviceId, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        Organizer organizer = new Organizer(deviceId);
        db.collection("organizers").document(deviceId)
            .set(organizer)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    public void organizerExists(String deviceId, Callback<Boolean> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        db.collection("organizers").document(deviceId)
            .get()
            .addOnSuccessListener(snapshot -> {
                cb.onSuccess(snapshot != null && snapshot.exists());
            })
            .addOnFailureListener(cb::onError);
    }
    
    // Adds event to organizer's events subcollection
    public void addEventToOrganizer(String deviceId, String eventId, Callback<Void> cb) {
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
        
        db.collection("organizers").document(deviceId)
            .collection("events").document(eventId)
            .set(data)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    public void getOrganizerEvents(String deviceId, Callback<List<String>> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
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
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        
        db.collection("organizers").document(deviceId)
            .collection("events").document(eventId)
            .delete()
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    // Bans/unbans an organizer (admin only)
    public void setBannedStatus(String deviceId, boolean banned, Callback<Void> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        db.collection("organizers").document(deviceId)
            .update("banned", banned)
            .addOnSuccessListener(unused -> cb.onSuccess(null))
            .addOnFailureListener(cb::onError);
    }
    
    public void isBanned(String deviceId, Callback<Boolean> cb) {
        if (deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("deviceId is empty"));
            return;
        }
        
        db.collection("organizers").document(deviceId)
            .get()
            .addOnSuccessListener(snapshot -> {
                if (snapshot != null && snapshot.exists()) {
                    Boolean banned = snapshot.getBoolean("banned");
                    cb.onSuccess(banned != null && banned);
                } else {
                    cb.onSuccess(false); // Not an organizer, so not banned as organizer
                }
            })
            .addOnFailureListener(cb::onError);
    }
}

