package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Tiny Firestore wrapper for events.
 *
 * We keep this intentionally small: list all events via a snapshot listener.
 * Additional calls (get one, waitlist ops) will be added as the stories require.
 */
public class EventDB {

    /** Lightweight async callback used by the data layer. */
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;

    /** Construct using the default Firestore instance. */
    public EventDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    /** Add or update an event in Firestore. */
    public void addEvent(Event event, Callback<Void> cb) {
        db.collection("events").document(event.getId())
                .set(event)
                .addOnSuccessListener(aVoid -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Streams all events in the `events` collection.
     * The callback is invoked whenever data changes.
     */
    public void getAllEvents(Callback<List<Event>> cb) {
        db.collection("events").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                cb.onError(e);
                return;
            }
            if (snapshots == null) {
                cb.onSuccess(new ArrayList<>());
                return;
            }
            List<Event> events = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshots) {
                Event event = parseEventFromDocument(doc);
                if (event != null) {
                    events.add(event);
                }
            }
            cb.onSuccess(events);
        });
    }

    /**
     * Fetches a single event by its ID.
     */
    public void getEvent(String eventId, Callback<Event> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        Event event = parseEventFromDocument(snapshot);
                        if (event != null) {
                            cb.onSuccess(event);
                        } else {
                            cb.onError(new RuntimeException("Failed to parse event"));
                        }
                    } else {
                        cb.onError(new RuntimeException("Event not found"));
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    public void isEntrantOnWaitlist(String eventId, String deviceId, Callback<Boolean> cb) {
        if (eventId == null || eventId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId or deviceId is empty"));
            return;
        }
        db.collection("events").document(eventId)
                .collection("waitingList").document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    cb.onSuccess(snapshot != null && snapshot.exists());
                })
                .addOnFailureListener(cb::onError);
    }

    // Checks if user can join (not already in any list)
    public void canJoinWaitlist(String eventId, String deviceId, Callback<Boolean> cb) {
        if (eventId == null || eventId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId or deviceId is empty"));
            return;
        }
        
        // Check if already in waitingList
        db.collection("events").document(eventId)
                .collection("waitingList").document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        cb.onSuccess(false); // Already on waitlist
                        return;
                    }
                    
                    // Check if in winners list
                    db.collection("events").document(eventId)
                            .collection("winners").document(deviceId)
                            .get()
                            .addOnSuccessListener(winnerSnapshot -> {
                                if (winnerSnapshot != null && winnerSnapshot.exists()) {
                                    cb.onSuccess(false); // Already a winner
                                    return;
                                }
                                
                                // Check if in accepted list
                                db.collection("events").document(eventId)
                                        .collection("accepted").document(deviceId)
                                        .get()
                                        .addOnSuccessListener(acceptedSnapshot -> {
                                            if (acceptedSnapshot != null && acceptedSnapshot.exists()) {
                                                cb.onSuccess(false); // Already accepted
                                            } else {
                                                // Can join if cancelled or not in any list
                                                cb.onSuccess(true);
                                            }
                                        })
                                        .addOnFailureListener(cb::onError);
                            })
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getWaitlistCount(String eventId, Callback<Integer> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot != null ? querySnapshot.size() : 0;
                    cb.onSuccess(count);
                })
                .addOnFailureListener(cb::onError);
    }

    // Real-time count (creates listener - remember to remove it!)
    public void fetchAccurateWaitlistCount(String eventId, Callback<Integer> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }

        db.collection("events").document(eventId)
                .collection("waitingList")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        cb.onError(e);
                        return;
                    }

                    int count = querySnapshot != null ? querySnapshot.size() : 0;
                    cb.onSuccess(count);
                });
    }

    public void joinWaitlist(String eventId, String deviceId, Callback<Void> cb) {
        if (eventId == null || eventId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId or deviceId is empty"));
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("request_time", FieldValue.serverTimestamp());

        db.collection("events").document(eventId)
                .collection("waitingList").document(deviceId)
                .set(data)
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    // Removes from waitlist (idempotent - safe to call multiple times)
    public void leaveWaitlist(String eventId, String deviceId, Callback<Void> cb) {
        if (eventId == null || eventId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId or deviceId is empty"));
            return;
        }
        db.collection("events").document(eventId)
                .collection("waitingList").document(deviceId)
                .delete()
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void getWaitlist(String eventId, Callback<List<Map<String, Object>>> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> entries = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("deviceId", doc.getId());
                            entry.put("requestTime", doc.get("request_time"));
                            entries.add(entry);
                        }
                    }
                    cb.onSuccess(entries);
                })
                .addOnFailureListener(cb::onError);
    }

    // Moves winners from waitlist to winners, creates replacement pool
    public void markWinners(String eventId, List<String> winnerIds, List<String> replacementIds, Callback<Void> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        if (winnerIds == null || winnerIds.isEmpty()) {
            cb.onError(new IllegalArgumentException("winnerIds is empty"));
            return;
        }

        WriteBatch batch = db.batch();
        long timestamp = System.currentTimeMillis();

        // Move winners from waitingList to winners
        for (String winnerId : winnerIds) {
            // Remove from waitingList
            DocumentReference waitlistRef = db.collection("events")
                    .document(eventId)
                    .collection("waitingList")
                    .document(winnerId);
            batch.delete(waitlistRef);

            // Add to winners
            DocumentReference winnersRef = db.collection("events")
                    .document(eventId)
                    .collection("winners")
                    .document(winnerId);
            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", winnerId);
            data.put("invitedAt", timestamp);
            batch.set(winnersRef, data);
        }

        // Move replacement pool from waitingList to replacementPool
        if (replacementIds != null && !replacementIds.isEmpty()) {
            for (String replacementId : replacementIds) {
                // Remove from waitingList
                DocumentReference waitlistRef = db.collection("events")
                        .document(eventId)
                        .collection("waitingList")
                        .document(replacementId);
                batch.delete(waitlistRef);

                // Add to replacementPool
                DocumentReference poolRef = db.collection("events")
                        .document(eventId)
                        .collection("replacementPool")
                        .document(replacementId);
                Map<String, Object> data = new HashMap<>();
                data.put("deviceId", replacementId);
                data.put("addedToPoolAt", timestamp);
                batch.set(poolRef, data);
            }
        }

        batch.commit()
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
    
    // Legacy - no replacement pool
    public void markWinners(String eventId, List<String> entrantIds, Callback<Void> cb) {
        markWinners(eventId, entrantIds, new ArrayList<>(), cb);
    }

    // Promotes replacement from pool to winners (picks first if entrantId is null)
    public void markReplacement(String eventId, String entrantId, Callback<Void> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }

        if (entrantId != null && !entrantId.isEmpty()) {
            // Specific entrant requested
            promoteReplacementToWinner(eventId, entrantId, cb);
        } else {
            // Pick first available from replacement pool
            getReplacementPool(eventId, new Callback<List<Map<String, Object>>>() {
                @Override
                public void onSuccess(List<Map<String, Object>> pool) {
                    if (pool == null || pool.isEmpty()) {
                        cb.onError(new IllegalStateException("Replacement pool is empty"));
                        return;
                    }
                    String firstReplacementId = (String) pool.get(0).get("deviceId");
                    promoteReplacementToWinner(eventId, firstReplacementId, cb);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    cb.onError(e);
                }
            });
        }
    }
    
    private void promoteReplacementToWinner(String eventId, String entrantId, Callback<Void> cb) {
        // Check if entrant is in replacement pool
        db.collection("events").document(eventId)
                .collection("replacementPool").document(entrantId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        cb.onError(new IllegalArgumentException("Entrant not in replacement pool"));
                        return;
                    }

                    WriteBatch batch = db.batch();

                    // Remove from replacementPool
                    DocumentReference poolRef = db.collection("events")
                            .document(eventId)
                            .collection("replacementPool")
                            .document(entrantId);
                    batch.delete(poolRef);

                    // Add to winners
                    DocumentReference winnersRef = db.collection("events")
                            .document(eventId)
                            .collection("winners")
                            .document(entrantId);
                    Map<String, Object> data = new HashMap<>();
                    data.put("deviceId", entrantId);
                    data.put("invitedAt", System.currentTimeMillis());
                    data.put("isReplacement", true); // Mark as replacement for tracking
                    batch.set(winnersRef, data);

                    batch.commit()
                            .addOnSuccessListener(unused -> cb.onSuccess(null))
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    // Moves winner to accepted or cancelled based on enrolled flag
    public void setEnrolledStatus(String eventId, String deviceId, Boolean enrolled, Callback<Void> cb) {
        if (eventId == null || eventId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId or deviceId is empty"));
            return;
        }

        WriteBatch batch = db.batch();

        // Remove from winners
        DocumentReference winnersRef = db.collection("events")
                .document(eventId)
                .collection("winners")
                .document(deviceId);
        batch.delete(winnersRef);

        // Add to appropriate list based on enrollment status
        String targetCollection = enrolled ? "accepted" : "cancelled";
        DocumentReference targetRef = db.collection("events")
                .document(eventId)
                .collection(targetCollection)
                .document(deviceId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("respondedAt", System.currentTimeMillis());
        batch.set(targetRef, data);

        batch.commit()
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void getWinners(String eventId, Callback<List<Map<String, Object>>> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }

        db.collection("events").document(eventId)
                .collection("winners")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> winners = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", doc.getId());
                            data.put("invitedAt", doc.get("invitedAt"));
                            winners.add(data);
                        }
                    }
                    cb.onSuccess(winners);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getCancelled(String eventId, Callback<List<Map<String, Object>>> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }

        db.collection("events").document(eventId)
                .collection("cancelled")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> cancelled = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", doc.getId());
                            data.put("respondedAt", doc.get("respondedAt"));
                            cancelled.add(data);
                        }
                    }
                    cb.onSuccess(cancelled);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getEnrolled(String eventId, Callback<List<Map<String, Object>>> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }

        db.collection("events").document(eventId)
                .collection("accepted")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> enrolled = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", doc.getId());
                            data.put("respondedAt", doc.get("respondedAt"));
                            enrolled.add(data);
                        }
                    }
                    cb.onSuccess(enrolled);
                })
                .addOnFailureListener(cb::onError);
    }
    
    public void getReplacementPool(String eventId, Callback<List<Map<String, Object>>> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }

        db.collection("events").document(eventId)
                .collection("replacementPool")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> pool = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", doc.getId());
                            data.put("addedToPoolAt", doc.get("addedToPoolAt"));
                            pool.add(data);
                        }
                    }
                    cb.onSuccess(pool);
                })
                .addOnFailureListener(cb::onError);
    }
    
    public void getReplacementPoolCount(String eventId, Callback<Integer> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        
        db.collection("events").document(eventId)
                .collection("replacementPool")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot != null ? querySnapshot.size() : 0;
                    cb.onSuccess(count);
                })
                .addOnFailureListener(cb::onError);
    }

    // Helper to parse event from Firestore doc
    private Event parseEventFromDocument(DocumentSnapshot doc) {
        try {
            Event event = new Event();
            event.setId(doc.getId());
            event.setName(doc.getString("name"));
            event.setDescription(doc.getString("description"));
            event.setLocation(doc.getString("location"));
            event.setOpen(doc.getBoolean("open") != null && doc.getBoolean("open"));
            event.setOrganizerId(doc.getString("organizerId"));
            event.setQrCode(doc.getString("qrCode"));
            event.setMaxCapacity(doc.get("maxCapacity", Integer.class));

            // Convert Timestamp objects to String (ISO format)
            event.setEventDateTime(convertTimestampToString(doc.get("eventDateTime")));
            event.setRegistrationOpen(convertTimestampToString(doc.get("registrationOpen")));
            event.setRegistrationClose(convertTimestampToString(doc.get("registrationClose")));

            return event;
        } catch (Exception e) {
            android.util.Log.e("EventDB", "Failed to parse event from document", e);
            return null;
        }
    }

    // Converts Firestore Timestamp to ISO string
    private String convertTimestampToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) value;
            Date date = timestamp.toDate();
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return isoFormat.format(date);
        }
        // Try to handle Date objects too
        if (value instanceof Date) {
            Date date = (Date) value;
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return isoFormat.format(date);
        }
        // Fallback: convert to string
        return value.toString();
    }
}