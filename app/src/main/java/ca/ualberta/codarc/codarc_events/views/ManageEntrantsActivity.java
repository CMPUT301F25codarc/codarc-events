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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.WaitlistAdapter;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import com.google.firebase.Timestamp;

/**
 * Displays list of entrants on the waitlist for an event.
 * Shows entrant names and request timestamps.
 */
public class ManageEntrantsActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_manage_entrants);

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

        loadWaitlist();
    }

    private void loadWaitlist() {
        eventDB.getWaitlist(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
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
                Log.e("ManageEntrantsActivity", "Failed to load waitlist", e);
                Toast.makeText(ManageEntrantsActivity.this, "Failed to load entrants", Toast.LENGTH_SHORT).show();
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
            Object requestTimeObj = entry.get("requestTime");

            entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
                @Override
                public void onSuccess(Entrant entrant) {
                    String name = deviceId;
                    if (entrant != null && entrant.getName() != null && !entrant.getName().isEmpty()) {
                        name = entrant.getName();
                    }
                    long timestamp = parseTimestamp(requestTimeObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, name, timestamp));
                    
                    checkAndUpdateUI(completed, totalEntries);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    // use deviceId if can't get name
                    long timestamp = parseTimestamp(requestTimeObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, deviceId, timestamp));
                    
                    checkAndUpdateUI(completed, totalEntries);
                }
            });
        }
    }

    private void checkAndUpdateUI(int[] completed, int totalEntries) {
        completed[0]++;
        if (completed[0] == totalEntries) {
            sortByTime();
            adapter.notifyDataSetChanged();
            hideEmptyState();
        }
    }

    private long parseTimestamp(Object requestTimeObj) {
        if (requestTimeObj == null) {
            Log.w("ManageEntrantsActivity", "Request time is null, using 0");
            return 0L;
        }

        if (requestTimeObj instanceof Timestamp) {
            Timestamp ts = (Timestamp) requestTimeObj;
            return ts.toDate().getTime();
        }

        if (requestTimeObj instanceof Long) {
            return (Long) requestTimeObj;
        }

        if (requestTimeObj instanceof Date) {
            return ((Date) requestTimeObj).getTime();
        }

        Log.w("ManageEntrantsActivity", "Unknown timestamp type: " + requestTimeObj.getClass().getName());
        return 0L;
    }

    private void sortByTime() {
        Collections.sort(itemList, (a, b) -> Long.compare(a.getRequestTime(), b.getRequestTime()));
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
