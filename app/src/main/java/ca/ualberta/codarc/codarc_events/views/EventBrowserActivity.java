package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.EventCardAdapter;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Displays the list of events for entrants.
 *
 * This screen is intentionally light: it initializes the recycler, subscribes
 * to `EventDB.getAllEvents()`, and lets the card adapter handle per-item
 * interactions like Join.
 */
public class EventBrowserActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private final List<Event> eventList = new ArrayList<>();
    private EventCardAdapter adapter;
    private EventDB eventDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_browser);

        // Stage 0: device identification (redundant safety; Landing already ensures)
        String deviceId = Identity.getOrCreateDeviceId(this);
        new EntrantDB().getOrCreateEntrant(deviceId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) { }

            @Override
            public void onError(@NonNull Exception e) { }
        });

        rvEvents = findViewById(R.id.rv_events);
        if (rvEvents == null) {
            android.util.Log.e("EventBrowserActivity", "RecyclerView not found in layout");
            finish();
            return;
        }
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventCardAdapter(this, eventList);
        rvEvents.setAdapter(adapter);

        eventDB = new EventDB();
        loadEvents();

        ImageButton plusBtn = findViewById(R.id.btn_plus);
        if (plusBtn != null) {
            plusBtn.setOnClickListener(v -> {
                Intent intent = new Intent(EventBrowserActivity.this, CreateEventActivity.class);
                startActivity(intent);
            });
        }
    }

    /**
     * Subscribes to Firestore for all events and refreshes the adapter list.
     */
    private void loadEvents() {
        eventDB.getAllEvents(new EventDB.Callback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> value) {
                if (value != null) {
                    eventList.clear();
                    eventList.addAll(value);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("EventBrowserActivity", "Failed to load events", e);
            }
        });
    }
}


