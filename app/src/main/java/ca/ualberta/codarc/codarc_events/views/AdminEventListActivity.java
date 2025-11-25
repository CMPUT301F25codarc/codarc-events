package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.AdminEventListAdapter;
import ca.ualberta.codarc.codarc_events.controllers.DeleteEventController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Displays a list of all events for administrators to browse and manage.
 * Provides view functionality with delete option for each event.
 */
public class AdminEventListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminEventListAdapter adapter;
    private ProgressBar loadingView;
    private TextView emptyStateView;
    private TextView errorStateView;

    private EventDB eventDB;
    private DeleteEventController deleteController;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_list);

        deviceId = Identity.getOrCreateDeviceId(this);
        eventDB = new EventDB();
        deleteController = new DeleteEventController();

        recyclerView = findViewById(R.id.rv_admin_events);
        loadingView = findViewById(R.id.pb_loading);
        emptyStateView = findViewById(R.id.tv_empty_state);
        errorStateView = findViewById(R.id.tv_error_state);

        ImageButton backButton = findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminEventListAdapter(event -> handleDeleteClick(event));
        recyclerView.setAdapter(adapter);

        // Set up error state retry
        if (errorStateView != null) {
            errorStateView.setOnClickListener(v -> loadEvents());
        }

        loadEvents();
    }

    /**
     * Loads all events from Firestore and displays them in the RecyclerView.
     */
    private void loadEvents() {
        showLoading(true);
        showEmptyState(false);
        showErrorState(false);

        // Use one-time fetch instead of listener to avoid memory leaks
        eventDB.getAllEventsOnce(new EventDB.Callback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (events == null || events.isEmpty()) {
                        adapter.setItems(null);
                        showEmptyState(true);
                    } else {
                        adapter.setItems(events);
                        showEmptyState(false);
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("AdminEventListActivity", "Failed to load events", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    showErrorState(true);
                    showEmptyState(false);
                });
            }
        });
    }

    /**
     * Handles the delete button click for an event.
     * Shows a confirmation dialog before proceeding with deletion.
     *
     * @param event the event to delete
     */
    private void handleDeleteClick(Event event) {
        if (event == null || event.getId() == null) {
            return;
        }

        String eventName = event.getName() != null ? event.getName() : "this event";
        String message = getString(R.string.admin_delete_event_confirm, eventName);

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_remove_events_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    deleteEvent(event.getId());
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    /**
     * Deletes an event using the DeleteEventController.
     *
     * @param eventId the event ID to delete
     */
    private void deleteEvent(String eventId) {
        showLoading(true);
        deleteController.deleteEvent(eventId, deviceId, new DeleteEventController.Callback() {
            @Override
            public void onResult(DeleteEventController.DeleteEventResult result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (result.isSuccess()) {
                        Toast.makeText(AdminEventListActivity.this,
                                R.string.admin_delete_event_success, Toast.LENGTH_SHORT).show();
                        // Reload events to refresh the list
                        loadEvents();
                    } else {
                        Toast.makeText(AdminEventListActivity.this,
                                result.getErrorMessage() != null ? result.getErrorMessage() :
                                        getString(R.string.admin_delete_event_error),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * Shows or hides the loading indicator.
     *
     * @param show true to show, false to hide
     */
    private void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Shows or hides the empty state message.
     *
     * @param show true to show, false to hide
     */
    private void showEmptyState(boolean show) {
        if (emptyStateView != null) {
            emptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Shows or hides the error state message.
     *
     * @param show true to show, false to hide
     */
    private void showErrorState(boolean show) {
        if (errorStateView != null) {
            errorStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}

