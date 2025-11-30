package ca.ualberta.codarc.codarc_events.views;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.List;
import java.util.stream.Collectors;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.AdminImageListAdapter;
import ca.ualberta.codarc.codarc_events.controllers.RemoveImageController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Displays a list of all events with images (posters) for administrators to browse and manage.
 * Provides gallery view with delete option for each image.
 */
public class AdminImageListActivity extends BaseAdminListActivity {

    private AdminImageListAdapter adapter;
    private EventDB eventDB;
    private RemoveImageController removeImageController;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_admin_image_list;
    }

    @Override
    protected int getRecyclerViewId() {
        return R.id.rv_admin_images;
    }

    @Override
    protected int getLoadingViewId() {
        return R.id.pb_loading;
    }

    @Override
    protected int getEmptyStateId() {
        return R.id.tv_empty_state;
    }

    @Override
    protected int getErrorStateId() {
        return R.id.tv_error_state;
    }

    @Override
    protected void setupRecyclerView() {
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new AdminImageListAdapter(event -> handleDeleteClick(event));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void initializeActivity() {
        eventDB = new EventDB();
        removeImageController = new RemoveImageController();
        }

    @Override
    protected void loadData() {
        loadEvents();
    }

    private void loadEvents() {
        resetUIStates();

        eventDB.getAllEventsOnce(new EventDB.Callback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> events) {
                runOnUiThread(() -> {
                    if (events == null || events.isEmpty()) {
                        adapter.setItems(null);
                        handleLoadSuccess(false);
                    } else {
                        List<Event> eventsWithImages = events.stream()
                                .filter(event -> event != null && event.getPosterUrl() != null && !event.getPosterUrl().trim().isEmpty())
                                .collect(Collectors.toList());
                        adapter.setItems(eventsWithImages);
                        handleLoadSuccess(!eventsWithImages.isEmpty());
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                handleLoadError(e, "AdminImageListActivity");
            }
        });
    }

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

}

