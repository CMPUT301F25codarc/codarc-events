package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
import android.os.Bundle;

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

public class EventBrowserActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private final List<Event> eventList = new ArrayList<>();
    private EventCardAdapter adapter;
    private EventDB eventDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_browser);

        // Stage 0: device identification (moved from LandingActivity)
        String deviceId = Identity.getOrCreateDeviceId(this);
        new EntrantDB().getOrCreateEntrant(deviceId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) { }

            @Override
            public void onError(@NonNull Exception e) { }
        });

        rvEvents = findViewById(R.id.rv_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventCardAdapter(this, eventList);
        rvEvents.setAdapter(adapter);

        eventDB = new EventDB();
        loadEvents();
    }

    private void loadEvents() {
        eventDB.getAllEvents(new EventDB.Callback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> value) {
                eventList.clear();
                eventList.addAll(value);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(@NonNull Exception e) { }
        });
    }
}


