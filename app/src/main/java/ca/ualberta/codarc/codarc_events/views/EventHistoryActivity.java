package ca.ualberta.codarc.codarc_events.views;


import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.EventCardAdapter;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Displays all events the entrant has registered for.
 */
public class EntrantHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventCardAdapter eventAdapter;
    private EventDB eventDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_history);

        recyclerView = findViewById(R.id.rv_history_events);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        eventDB = new EventDB();
        String deviceId = Identity.getOrCreateDeviceId(this);

        // Firestore query to get the entrant’s joined events
        eventDB.getEventsJoinedByEntrant(deviceId, new EventDB.Callback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                eventAdapter = new EventCardAdapter(EntrantHistoryActivity.this, events);
                recyclerView.setAdapter(eventAdapter);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(EntrantHistoryActivity.this, "Failed to load event history", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
