package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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
                events.add(event);
            }
            cb.onSuccess(events);
        });
    }
}


