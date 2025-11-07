package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.WaitlistAdapter;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Displays list of enrolled entrants for an event.
 */
public class ViewEnrolledActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WaitlistAdapter adapter;
    private TextView emptyState;
    private EventDB eventDB;
    private EntrantDB entrantDB;
    private String eventId;
    private List<WaitlistAdapter.WaitlistItem> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_enrolled);

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventDB = new EventDB();
        entrantDB = new EntrantDB();
        itemList = new ArrayList<>();

        recyclerView = findViewById(R.id.rv_entrants);
        emptyState = findViewById(R.id.tv_empty_state);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaitlistAdapter(itemList);
        recyclerView.setAdapter(adapter);

        verifyOrganizerAccess();
        loadEnrolled();
    }

    private void verifyOrganizerAccess() {
        String deviceId = Identity.getOrCreateDeviceId(this);
        
        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null || event.getOrganizerId() == null || !event.getOrganizerId().equals(deviceId)) {
                    runOnUiThread(() -> {
                        Toast.makeText(ViewEnrolledActivity.this, "Only event organizer can access this", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ViewEnrolledActivity.this, "Failed to verify access", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void loadEnrolled() {
        eventDB.getEnrolled(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> entries) {
                if (entries == null || entries.isEmpty()) {
                    showEmptyState();
                    return;
                }

                fetchEntrantNames(entries);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("ViewEnrolledActivity", "Failed to load enrolled entrants", e);
                Toast.makeText(ViewEnrolledActivity.this, "Failed to load enrolled entrants", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchEntrantNames(List<Map<String, Object>> entries) {
        itemList.clear();
        if (entries == null || entries.isEmpty()) {
            showEmptyState();
            return;
        }

        final int totalEntries = entries.size();
        final int[] completed = {0};

        for (Map<String, Object> entry : entries) {
            String deviceId = (String) entry.get("deviceId");
            Object respondedAtObj = entry.get("respondedAt");

            entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
                @Override
                public void onSuccess(Entrant entrant) {
                    String name = deviceId;
                    if (entrant != null && entrant.getName() != null && !entrant.getName().isEmpty()) {
                        name = entrant.getName();
                    }
                    long timestamp = parseTimestamp(respondedAtObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, name, timestamp));

                    checkAndUpdateUI(completed, totalEntries);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    Log.w("ViewEnrolledActivity", "Failed to fetch profile for " + deviceId, e);
                    long timestamp = parseTimestamp(respondedAtObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, deviceId, timestamp));

                    checkAndUpdateUI(completed, totalEntries);
                }
            });
        }
    }

    private void checkAndUpdateUI(int[] completed, int totalEntries) {
        completed[0]++;
        if (completed[0] == totalEntries) {
            adapter.notifyDataSetChanged();
            hideEmptyState();
        }
    }

    private long parseTimestamp(Object timestampObj) {
        if (timestampObj == null) {
            Log.w("ViewEnrolledActivity", "Timestamp is null, using 0");
            return 0L;
        }

        if (timestampObj instanceof Timestamp) {
            Timestamp ts = (Timestamp) timestampObj;
            return ts.toDate().getTime();
        }

        if (timestampObj instanceof Long) {
            return (Long) timestampObj;
        }

        if (timestampObj instanceof Date) {
            return ((Date) timestampObj).getTime();
        }

        Log.w("ViewEnrolledActivity", "Unknown timestamp type: " + timestampObj.getClass().getName());
        return 0L;
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }
}

