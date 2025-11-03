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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.WaitlistAdapter;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.utils.DateTimeUtils;

/**
 * Shows the waitlist for an event.
 * Displays entrant names and request times.
 */
public class ManageWaitlistActivity extends AppCompatActivity {

    private RecyclerView entrantRecyclerView;
    private TextView emptyStateText;
    private WaitlistAdapter adapter;
    private EventDB eventDB;
    private EntrantDB entrantDB;
    private String eventId;
    private List<Map<String, Object>> waitlistEntries;

    /**
     * Sets up the waitlist screen and loads data.
     *
     * @param savedInstanceState saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_waitlist);

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Log.e("ManageWaitlistActivity", "Event ID not found in Intent");
            Toast.makeText(this, "Event ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventDB = new EventDB();
        entrantDB = new EntrantDB();
        waitlistEntries = new ArrayList<>();

        setupUI();
        loadWaitlist();
    }

    private void setupUI() {
        entrantRecyclerView = findViewById(R.id.recycler_entrants);
        emptyStateText = findViewById(R.id.tv_empty_state);

        adapter = new WaitlistAdapter(this, waitlistEntries);
        entrantRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        entrantRecyclerView.setAdapter(adapter);
    }

    /**
     * Loads waitlist entries and fetches names for each entrant.
     */
    private void loadWaitlist() {
        eventDB.getWaitlist(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> entries) {
                if (entries == null || entries.isEmpty()) {
                    runOnUiThread(() -> showEmptyState());
                    return;
                }

                // Fetch names for all entries
                fetchEntrantNames(entries);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("ManageWaitlistActivity", "Failed to load waitlist", e);
                runOnUiThread(() -> {
                    Toast.makeText(ManageWaitlistActivity.this,
                            "Failed to load waitlist. Please try again.", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
            }
        });
    }

    /**
     * Fetches names for all waitlist entries.
     * Sorts by request time after all names are loaded.
     *
     * @param entries waitlist entries with deviceId and requestTime
     */
    private void fetchEntrantNames(List<Map<String, Object>> entries) {
        waitlistEntries.clear();
        int totalEntries = entries.size();
        final int[] completedFetches = {0};

        if (totalEntries == 0) {
            runOnUiThread(() -> {
                showEmptyState();
                adapter.notifyDataSetChanged();
            });
            return;
        }

        for (Map<String, Object> entry : entries) {
            String deviceId = (String) entry.get("deviceId");
            if (deviceId == null) {
                completedFetches[0]++;
                if (completedFetches[0] == totalEntries) {
                    updateUI();
                }
                continue;
            }

            entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
                @Override
                public void onSuccess(Entrant entrant) {
                    Map<String, Object> enrichedEntry = new HashMap<>(entry);
                    String name = getEntrantName(entrant, deviceId);
                    enrichedEntry.put("name", name);
                    synchronized (waitlistEntries) {
                        waitlistEntries.add(enrichedEntry);
                        completedFetches[0]++;
                        if (completedFetches[0] == totalEntries) {
                            updateUI();
                        }
                    }
                }

                @Override
                public void onError(@NonNull Exception e) {
                    Log.w("ManageWaitlistActivity", "Failed to fetch name for " + deviceId, e);
                    Map<String, Object> enrichedEntry = new HashMap<>(entry);
                    enrichedEntry.put("name", deviceId);
                    synchronized (waitlistEntries) {
                        waitlistEntries.add(enrichedEntry);
                        completedFetches[0]++;
                        if (completedFetches[0] == totalEntries) {
                            updateUI();
                        }
                    }
                }
            });
        }
    }

    /**
     * Gets entrant name from profile, or deviceId if not available.
     */
    private String getEntrantName(Entrant entrant, String deviceId) {
        if (entrant != null && entrant.getName() != null && !entrant.getName().isEmpty()) {
            return entrant.getName();
        }
        return deviceId;
    }

    /**
     * Updates the UI after all names are fetched.
     * Sorts by request time (earliest first).
     */
    private void updateUI() {
        runOnUiThread(() -> {
            waitlistEntries.sort(new Comparator<Map<String, Object>>() {
                @Override
                public int compare(Map<String, Object> a, Map<String, Object> b) {
                    long timeA = extractTimestamp(a.get("requestTime"));
                    long timeB = extractTimestamp(b.get("requestTime"));
                    return Long.compare(timeA, timeB);
                }

                private long extractTimestamp(Object timeObj) {
                    return DateTimeUtils.extractTimeMillis(timeObj);
                }
            });

            if (waitlistEntries.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showEmptyState() {
        emptyStateText.setVisibility(View.VISIBLE);
        entrantRecyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        emptyStateText.setVisibility(View.GONE);
        entrantRecyclerView.setVisibility(View.VISIBLE);
    }
}

