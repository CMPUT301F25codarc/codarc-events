package ca.ualberta.codarc.codarc_events.views;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.AdminEventListAdapter;
import ca.ualberta.codarc.codarc_events.controllers.DeleteEventController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Displays a list of all events for administrators to browse and manage.
 * Provides view functionality with delete option for each event.
 */
public class AdminEventListActivity extends BaseAdminListActivity {

    private AdminEventListAdapter adapter;
    private EventDB eventDB;
    private DeleteEventController deleteController;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_admin_event_list;
    }

    @Override
    protected int getRecyclerViewId() {
        return R.id.rv_admin_events;
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
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminEventListAdapter(event -> handleDeleteClick(event));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void initializeActivity() {
        eventDB = new EventDB();
        deleteController = new DeleteEventController();
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
                        adapter.setItems(events);
                        handleLoadSuccess(true);
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                handleLoadError(e, "AdminEventListActivity");
            }
        });
    }

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

}

