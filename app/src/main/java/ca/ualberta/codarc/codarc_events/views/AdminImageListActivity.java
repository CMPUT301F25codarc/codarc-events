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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.AdminImageListAdapter;
import ca.ualberta.codarc.codarc_events.controllers.RemoveImageController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Displays a list of all events with images (posters) for administrators to browse and manage.
 * Provides gallery view with delete option for each image.
 */
public class AdminImageListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminImageListAdapter adapter;
    private ProgressBar loadingView;
    private TextView emptyStateView;
    private TextView errorStateView;

    private EventDB eventDB;
    private RemoveImageController removeImageController;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_image_list);

        deviceId = Identity.getOrCreateDeviceId(this);
        eventDB = new EventDB();
        removeImageController = new RemoveImageController();

        recyclerView = findViewById(R.id.rv_admin_images);
        loadingView = findViewById(R.id.pb_loading);
        emptyStateView = findViewById(R.id.tv_empty_state);
        errorStateView = findViewById(R.id.tv_error_state);

        ImageButton backButton = findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Use GridLayoutManager for gallery view (2 columns)
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AdminImageListAdapter(event -> handleDeleteClick(event));
        recyclerView.setAdapter(adapter);

        // Set up error state retry
        if (errorStateView != null) {
            errorStateView.setOnClickListener(v -> loadEvents());
        }

        loadEvents();
    }

    /**
     * Loads all events from Firestore, filters to only those with images,
     * and displays them in the RecyclerView.
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
                        // Check if adapter has items after filtering
                        if (adapter.getItemCount() == 0) {
                            showEmptyState(true);
                        } else {
                            showEmptyState(false);
                        }
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("AdminImageListActivity", "Failed to load events", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    showErrorState(true);
                    showEmptyState(false);
                });
            }
        });
    }

    /**
     * Handles the delete button click for an event image.
     * Shows a confirmation dialog before proceeding with deletion.
     *
     * @param event the event whose image should be deleted
     */
    private void handleDeleteClick(Event event) {
        if (event == null || event.getId() == null) {
            return;
        }

        String eventName = event.getName() != null ? event.getName() : "this event";
        String message = getString(R.string.admin_delete_image_confirm, eventName);

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_remove_images_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    removeImage(event.getId());
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    /**
     * Removes an event image using the RemoveImageController.
     *
     * @param eventId the event ID whose image should be removed
     */
    private void removeImage(String eventId) {
        showLoading(true);
        removeImageController.removeImage(eventId, deviceId, new RemoveImageController.Callback() {
            @Override
            public void onResult(RemoveImageController.RemoveImageResult result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (result.isSuccess()) {
                        Toast.makeText(AdminImageListActivity.this,
                                R.string.admin_delete_image_success, Toast.LENGTH_SHORT).show();
                        // Reload events to refresh the list
                        loadEvents();
                    } else {
                        Toast.makeText(AdminImageListActivity.this,
                                result.getErrorMessage() != null ? result.getErrorMessage() :
                                        getString(R.string.admin_delete_image_error),
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