package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                Event event = doc.toObject(Event.class);
                if (event != null) {
                    event.setId(doc.getId());
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
                        Event event = snapshot.toObject(Event.class);
                        if (event != null) {
                            event.setId(snapshot.getId());
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
     * Returns true if a document exists with is_waiting=true.
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
                        Boolean isWaiting = snapshot.getBoolean("is_waiting");
                        cb.onSuccess(isWaiting != null && isWaiting);
                    } else {
                        cb.onSuccess(false);
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Counts the number of entrants with is_waiting=true for the event.
     */
    public void getWaitlistCount(String eventId, Callback<Integer> cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        db.collection("events").document(eventId)
                .collection("entrants")
                .whereEqualTo("is_waiting", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    cb.onSuccess(querySnapshot != null ? querySnapshot.size() : 0);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Adds or updates an entrant document in the event's entrants subcollection.
     * Sets is_waiting=true and records request_time timestamp.
     * Idempotent: if already on waitlist, no error.
     */
    public void joinWaitlist(String eventId, String deviceId, Callback<Void> cb) {
        if (eventId == null || eventId.isEmpty() || deviceId == null || deviceId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId or deviceId is empty"));
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("is_waiting", true);
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
     * Returns a list of maps containing deviceId and requestTime.
     * Only includes entrants with is_waiting=true.
     * Note: Sorting by request_time should be done in the calling Activity
     * to avoid requiring a Firestore composite index.
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
                .whereEqualTo("is_waiting", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> entries = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("deviceId", doc.getId());
                            Object requestTime = doc.get("request_time");
                            entry.put("requestTime", requestTime);
                            entries.add(entry);
                        }
                    }
                    cb.onSuccess(entries);
                })
                .addOnFailureListener(cb::onError);
    }
}