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

    /**
     * Checks if an entrant is currently on the waitlist for an event.
     * Returns true if entrant exists with is_winner=false and is_enrolled=null.
     */
    public void isEntrantOnWaitlist(String eventId, String deviceId, Callback<Boolean> cb) {
        if (eventId == null || eventId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId or deviceId is empty"));
            return;
        }
        db.collection("events").document(eventId)
                .collection("entrants").document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        Boolean isWinner = snapshot.getBoolean("is_winner");
                        Boolean isEnrolled = snapshot.getBoolean("is_enrolled");
                        boolean onWaitlist = (isWinner == null || !isWinner) && isEnrolled == null;
                        cb.onSuccess(onWaitlist);
                    } else {
                        cb.onSuccess(false);
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Checks if an entrant can join the waitlist.
     */
    public void canJoinWaitlist(String eventId, String deviceId, Callback<Boolean> cb) {
        if (eventId == null || eventId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId or deviceId is empty"));
            return;
        }
        db.collection("events").document(eventId)
                .collection("entrants").document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        cb.onSuccess(true);
                        return;
                    }
                    Boolean isWinner = snapshot.getBoolean("is_winner");
                    Boolean isEnrolled = snapshot.getBoolean("is_enrolled");
                    
                    boolean onWaitlist = (isWinner == null || !isWinner) && isEnrolled == null;
                    boolean winnerDeclined = Boolean.TRUE.equals(isWinner) && Boolean.FALSE.equals(isEnrolled);
                    
                    cb.onSuccess(!onWaitlist && winnerDeclined);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Counts the number of entrants on waitlist (is_winner=false or null, is_enrolled=null).
     */
    public void getWaitlistCount(String eventId, Callback<Integer> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        getWaitlist(eventId, new Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> waitlist) {
                cb.onSuccess(waitlist != null ? waitlist.size() : 0);
            }

            @Override
            public void onError(@NonNull Exception e) {
                cb.onError(e);
            }
        });
    }

    /**
     * Adds or updates an entrant document in the event's entrants subcollection.
     * Sets is_winner=false, is_enrolled=null and records request_time timestamp.
     * Idempotent: if already on waitlist, no error.
     */
    public void joinWaitlist(String eventId, String deviceId, Callback<Void> cb) {
        if (eventId == null || eventId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId or deviceId is empty"));
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("is_winner", false);
        data.put("is_enrolled", null);
        data.put("request_time", FieldValue.serverTimestamp());

        db.collection("events").document(eventId)
                .collection("entrants").document(deviceId)
                .set(data)
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Removes an entrant from the waitlist by deleting their document.
     * If document doesn't exist, still succeeds (idempotent).
     */
    public void leaveWaitlist(String eventId, String deviceId, Callback<Void> cb) {
        if (eventId == null || eventId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId or deviceId is empty"));
            return;
        }
        db.collection("events").document(eventId)
                .collection("entrants").document(deviceId)
                .delete()
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Fetches all entrants on the waitlist for an event.
     * Returns entrants with is_winner=false (or null) and is_enrolled=null.
     *
     * @param eventId the event ID to get waitlist for
     * @param cb callback with list of maps: {deviceId: String, requestTime: Object}
     */
    public void getWaitlist(String eventId, Callback<List<Map<String, Object>>> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        db.collection("events").document(eventId)
                .collection("entrants")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> entries = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Boolean isWinner = doc.getBoolean("is_winner");
                            Boolean isEnrolled = doc.getBoolean("is_enrolled");
                            boolean onWaitlist = (isWinner == null || !isWinner) && isEnrolled == null;
                            
                            if (onWaitlist) {
                                Map<String, Object> entry = new HashMap<>();
                                entry.put("deviceId", doc.getId());
                                entry.put("requestTime", doc.get("request_time"));
                                entries.add(entry);
                            }
                        }
                    }
                    cb.onSuccess(entries);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Sets is_winner=true for selected entrants.
     */
    public void markWinners(String eventId, List<String> entrantIds, Callback<Void> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        if (entrantIds == null || entrantIds.isEmpty()) {
            cb.onError(new IllegalArgumentException("entrantIds is empty"));
            return;
        }

        WriteBatch batch = db.batch();

        for (String entrantId : entrantIds) {
            DocumentReference ref = db.collection("events")
                    .document(eventId)
                    .collection("entrants")
                    .document(entrantId);

            Map<String, Object> data = new HashMap<>();
            data.put("is_winner", true);
            data.put("invitedAt", System.currentTimeMillis());
            batch.set(ref, data, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Sets is_winner=true for a replacement entrant.
     */
    public void markReplacement(String eventId, String entrantId, Callback<Void> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        if (entrantId == null || entrantId.isEmpty()) {
            cb.onError(new IllegalArgumentException("entrantId is empty"));
            return;
        }

        db.collection("events").document(eventId)
                .collection("entrants").document(entrantId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        cb.onError(new IllegalArgumentException("Entrant not found"));
                        return;
                    }
                    Boolean isWinner = snapshot.getBoolean("is_winner");
                    if (Boolean.TRUE.equals(isWinner)) {
                        cb.onError(new IllegalStateException("Entrant already a winner"));
                        return;
                    }
                    
                    db.collection("events").document(eventId)
                            .collection("entrants").document(entrantId)
                            .update("is_winner", true, "invitedAt", System.currentTimeMillis())
                            .addOnSuccessListener(unused -> cb.onSuccess(null))
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Sets is_enrolled status for an entrant.
     */
    public void setEnrolledStatus(String eventId, String deviceId, Boolean enrolled, Callback<Void> cb) {
        if (eventId == null || eventId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId or deviceId is empty"));
            return;
        }

        db.collection("events").document(eventId)
                .collection("entrants").document(deviceId)
                .update("is_enrolled", enrolled)
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Retrieves entrants with is_winner=true.
     */
    public void getWinners(String eventId, Callback<List<Map<String, Object>>> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }

        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("is_winner", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> winners = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", doc.getId());
                            data.put("invitedAt", doc.get("invitedAt"));
                            data.put("is_enrolled", doc.get("is_enrolled"));
                            winners.add(data);
                        }
                    }
                    cb.onSuccess(winners);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Retrieves entrants with is_enrolled=false.
     */
    public void getCancelled(String eventId, Callback<List<Map<String, Object>>> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }

        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("is_enrolled", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> cancelled = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", doc.getId());
                            data.put("invitedAt", doc.get("invitedAt"));
                            cancelled.add(data);
                        }
                    }
                    cb.onSuccess(cancelled);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Retrieves entrants with is_enrolled=true.
     */
    public void getEnrolled(String eventId, Callback<List<Map<String, Object>>> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }

        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("is_enrolled", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> enrolled = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", doc.getId());
                            data.put("invitedAt", doc.get("invitedAt"));
                            enrolled.add(data);
                        }
                    }
                    cb.onSuccess(enrolled);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Parses an Event from a Firestore DocumentSnapshot.
     * Handles conversion of Timestamp objects to String format.
     *
     * @param doc the document snapshot
     * @return Event or null if parsing fails
     */
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

    /**
     * Converts a Firestore Timestamp to ISO format string.
     * If the value is already a String, returns it as-is.
     * If it's a Timestamp, converts to ISO format.
     * If null, returns null.
     */
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