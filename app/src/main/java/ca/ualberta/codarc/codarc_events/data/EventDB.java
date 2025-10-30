package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.models.Event;

public class EventDB {

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;

    public EventDB() {
        this.db = FirebaseFirestore.getInstance();
    }

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


