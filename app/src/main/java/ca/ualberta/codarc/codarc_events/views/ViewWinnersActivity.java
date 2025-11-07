package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.util.Log;
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
import ca.ualberta.codarc.codarc_events.adapters.WinnersAdapter;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Displays list of winners for an event.
 * Notifications are automatically sent when the lottery draw is completed.
 */
public class ViewWinnersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WinnersAdapter adapter;
    private TextView emptyState;
    private EventDB eventDB;
    private EntrantDB entrantDB;
    private String eventId;
    private List<WinnersAdapter.WinnerItem> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_winners);

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
        adapter = new WinnersAdapter(itemList);
        recyclerView.setAdapter(adapter);

        verifyOrganizerAccess();
        loadWinners();
    }

    private void verifyOrganizerAccess() {
        String deviceId = Identity.getOrCreateDeviceId(this);

        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null || event.getOrganizerId() == null || !event.getOrganizerId().equals(deviceId)) {
                    runOnUiThread(() -> {
                        Toast.makeText(ViewWinnersActivity.this, "Only event organizer can access this", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ViewWinnersActivity.this, "Failed to verify access", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void loadWinners() {
        eventDB.getWinners(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
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
                Log.e("ViewWinnersActivity", "Failed to load winners", e);
                Toast.makeText(ViewWinnersActivity.this, "Failed to load winners", Toast.LENGTH_SHORT).show();
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
            Object invitedAtObj = entry.get("invitedAt");
            Object isEnrolledObj = entry.get("is_enrolled");
            final Boolean isEnrolled = (isEnrolledObj instanceof Boolean) ? (Boolean) isEnrolledObj : null;

            entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
                @Override
                public void onSuccess(Entrant entrant) {
                    String name = (entrant != null && entrant.getName() != null && !entrant.getName().isEmpty())
                            ? entrant.getName() : deviceId;
                    long timestamp = parseTimestamp(invitedAtObj);
                    itemList.add(new WinnersAdapter.WinnerItem(deviceId, name, timestamp, isEnrolled));
                    checkAndUpdateUI(completed, totalEntries);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    long timestamp = parseTimestamp(invitedAtObj);
                    itemList.add(new WinnersAdapter.WinnerItem(deviceId, deviceId, timestamp, isEnrolled));
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
            Log.w("ViewWinnersActivity", "Timestamp is null, using 0");
            return 0L;
        }
        if (timestampObj instanceof Timestamp) {
            return ((Timestamp) timestampObj).toDate().getTime();
        }
        if (timestampObj instanceof Long) {
            return (Long) timestampObj;
        }
        if (timestampObj instanceof Date) {
            return ((Date) timestampObj).getTime();
        }
        Log.w("ViewWinnersActivity", "Unknown timestamp type: " + timestampObj.getClass().getName());
        return 0L;
    }

    private void showEmptyState() {
        recyclerView.setVisibility(android.view.View.GONE);
        emptyState.setVisibility(android.view.View.VISIBLE);
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(android.view.View.VISIBLE);
        emptyState.setVisibility(android.view.View.GONE);
    }
}

