package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

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
 * Displays the list of available events for entrants.
 *
 * <p>This activity:
 * <ul>
 *   <li>Initializes and subscribes to Firestore via {@link EventDB#getAllEvents(EventDB.Callback)}.</li>
 *   <li>Displays all events in a RecyclerView using {@link EventCardAdapter}.</li>
 *   <li>Allows navigation to the profile screen (via iv_profile).</li>
 *   <li>Allows organizers to create a new event (via btn_plus).</li>
 * </ul></p>
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

        // --- Ensure device has a unique ID (used as entrant identifier)
        String deviceId = Identity.getOrCreateDeviceId(this);
        new EntrantDB().getOrCreateEntrant(deviceId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) { }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("EventBrowserActivity", "Failed to ensure entrant profile", e);
            }
        });

        // --- RecyclerView setup for events
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

        // --- "+" icon: opens CreateEventActivity for organizers
        ImageView plusIcon = findViewById(R.id.btn_plus);
        if (plusIcon != null) {
            plusIcon.setOnClickListener(v -> {
                Intent intent = new Intent(EventBrowserActivity.this, CreateEventActivity.class);
                startActivity(intent);
            });
        }

        // Profile icon: opens ProfileCreationActivity for profile management
        ImageView profileIcon = findViewById(R.id.iv_profile_settings);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> {
                Intent intent = new Intent(EventBrowserActivity.this, ProfileCreationActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }
    }

    /**
     * Loads all events from Firestore and updates the adapter list.
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



