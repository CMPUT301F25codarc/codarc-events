package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.EventCardAdapter;
import ca.ualberta.codarc.codarc_events.models.Event;

public class UnifiedDashboardActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private List<Event> eventList;
    private EventCardAdapter adapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unified_dashboard);

        // Initialize RecyclerView
        rvEvents = findViewById(R.id.rv_events);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));

        // Initialize list and adapter
        eventList = new ArrayList<>();
        adapter = new EventCardAdapter(this, eventList);
        rvEvents.setAdapter(adapter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Load event data
        loadEvents();
    }

    private void loadEvents() {
        db.collection("events").addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;

            eventList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Event event = doc.toObject(Event.class);
                eventList.add(event);
            }
            adapter.notifyDataSetChanged();
        });
    }
}


