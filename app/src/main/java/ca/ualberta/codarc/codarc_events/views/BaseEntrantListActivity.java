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

import java.util.Date;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Base class for activities that display lists of entrants.
 * Provides common functionality for loading, displaying, and managing entrant lists.
 */
public abstract class BaseEntrantListActivity extends AppCompatActivity {

    protected RecyclerView recyclerView;
    protected TextView emptyState;
    protected EventDB eventDB;
    protected EntrantDB entrantDB;
    protected String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventDB = new EventDB();
        entrantDB = new EntrantDB();

        recyclerView = findViewById(getRecyclerViewId());
        emptyState = findViewById(getEmptyStateId());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        setupAdapter();

        if (needsOrganizerAccess()) {
            verifyOrganizerAccess();
        }

        initializeActivity();
        loadData();
    }

    /**
     * Returns the layout resource ID for this activity.
     */
    protected abstract int getLayoutResourceId();

    /**
     * Returns the RecyclerView resource ID.
     */
    protected abstract int getRecyclerViewId();

    /**
     * Returns the empty state TextView resource ID.
     */
    protected abstract int getEmptyStateId();

    /**
     * Sets up the RecyclerView adapter.
     */
    protected abstract void setupAdapter();

    /**
     * Returns whether this activity requires organizer access verification.
     */
    protected boolean needsOrganizerAccess() {
        return false;
    }

    /**
     * Called after basic setup is complete, before loadData().
     * Override to perform activity-specific initialization.
     */
    protected void initializeActivity() {
        // Default: no additional initialization
    }

    /**
     * Loads the data for this activity.
     */
    protected abstract void loadData();

    /**
     * Verifies that the current user is the organizer of the event.
     */
    protected void verifyOrganizerAccess() {
        String deviceId = Identity.getOrCreateDeviceId(this);

        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null || event.getOrganizerId() == null || !event.getOrganizerId().equals(deviceId)) {
                    runOnUiThread(() -> {
                        Toast.makeText(BaseEntrantListActivity.this, "Only event organizer can access this", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(BaseEntrantListActivity.this, "Failed to verify access", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    /**
     * Parses a timestamp object from Firestore into a long value.
     *
     * @param timestampObj the timestamp object (Timestamp, Long, Date, or null)
     * @return the timestamp as milliseconds since epoch, or 0 if invalid
     */
    protected long parseTimestamp(Object timestampObj) {
        if (timestampObj == null) {
            Log.w(getClass().getSimpleName(), "Timestamp is null, using 0");
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

        Log.w(getClass().getSimpleName(), "Unknown timestamp type: " + timestampObj.getClass().getName());
        return 0L;
    }

    /**
     * Shows the empty state and hides the RecyclerView.
     */
    protected void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    /**
     * Hides the empty state and shows the RecyclerView.
     */
    protected void hideEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }
}
