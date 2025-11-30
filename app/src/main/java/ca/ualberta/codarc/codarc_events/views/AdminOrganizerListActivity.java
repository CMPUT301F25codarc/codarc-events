package ca.ualberta.codarc.codarc_events.views;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.AdminOrganizerListAdapter;
import ca.ualberta.codarc.codarc_events.controllers.RemoveOrganizerController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.OrganizerDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.Organizer;
import ca.ualberta.codarc.codarc_events.models.OrganizerWithEvents;

/**
 * Displays a list of all organizers for administrators to browse and manage.
 * Provides view functionality with delete option for each organizer.
 */
public class AdminOrganizerListActivity extends BaseAdminListActivity {

    private AdminOrganizerListAdapter adapter;
    private OrganizerDB organizerDB;
    private EventDB eventDB;
    private RemoveOrganizerController removeController;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_admin_organizer_list;
    }

    @Override
    protected int getRecyclerViewId() {
        return R.id.rv_admin_organizers;
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
        adapter = new AdminOrganizerListAdapter(organizerWithEvents -> handleRemoveClick(organizerWithEvents));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void initializeActivity() {
        organizerDB = new OrganizerDB();
        eventDB = new EventDB();
        removeController = new RemoveOrganizerController();
    }

    @Override
    protected void loadData() {
        loadOrganizers();
    }

    private void loadOrganizers() {
        resetUIStates();

        organizerDB.getAllOrganizers(new OrganizerDB.Callback<List<Organizer>>() {
            @Override
            public void onSuccess(List<Organizer> organizers) {
                if (organizers == null || organizers.isEmpty()) {
                    runOnUiThread(() -> {
                        adapter.setItems(null);
                        handleLoadSuccess(false);
                    });
                    return;
                }

                loadEventsForOrganizers(organizers);
            }

            @Override
            public void onError(@NonNull Exception e) {
                handleLoadError(e, "AdminOrganizerListActivity");
            }
        });
    }

    private void loadEventsForOrganizers(List<Organizer> organizers) {
        final int total = organizers.size();
        final List<OrganizerWithEvents> organizersWithEvents = new ArrayList<>();
        final OrganizerEventsAggregator aggregator = new OrganizerEventsAggregator(total, organizersWithEvents);

        if (total == 0) {
            runOnUiThread(() -> {
                adapter.setItems(null);
                handleLoadSuccess(false);
            });
            return;
        }

        for (Organizer organizer : organizers) {
            String organizerId = organizer.getDeviceId();
            eventDB.getEventsByOrganizer(organizerId, 3, new EventDB.Callback<List<Event>>() {
                @Override
                public void onSuccess(List<Event> events) {
                    synchronized (organizersWithEvents) {
                        organizersWithEvents.add(new OrganizerWithEvents(organizer, events));
                    }
                    aggregator.onOrganizerProcessed();
                }

                @Override
                public void onError(@NonNull Exception e) {
                    synchronized (organizersWithEvents) {
                        organizersWithEvents.add(new OrganizerWithEvents(organizer, new ArrayList<>()));
                    }
                    aggregator.onOrganizerProcessed();
                }
            });
        }
    }

    private void handleRemoveClick(OrganizerWithEvents organizerWithEvents) {
        if (organizerWithEvents == null || organizerWithEvents.getOrganizer() == null) {
            return;
        }

        String deviceId = organizerWithEvents.getOrganizer().getDeviceId();
        if (deviceId == null) {
            return;
        }

        String message = "Are you sure you want to delete this organizer? This will delete all their events and notifications.";

        new AlertDialog.Builder(this)
                .setTitle("Delete Organizer")
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    banOrganizer(deviceId);
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void banOrganizer(String organizerDeviceId) {
        showLoading(true);
        removeController.banOrganizer(organizerDeviceId, deviceId, new RemoveOrganizerController.Callback() {
            @Override
            public void onResult(RemoveOrganizerController.RemoveOrganizerResult result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (result.isSuccess()) {
                        Toast.makeText(AdminOrganizerListActivity.this,
                                "Organizer deleted successfully", Toast.LENGTH_SHORT).show();
                        loadOrganizers();
                    } else {
                        Toast.makeText(AdminOrganizerListActivity.this,
                                result.getErrorMessage() != null ? result.getErrorMessage() :
                                        "Failed to delete organizer. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private class OrganizerEventsAggregator {
        private final int total;
        private final List<OrganizerWithEvents> organizersWithEvents;
        private int completed;

        OrganizerEventsAggregator(int total, List<OrganizerWithEvents> organizersWithEvents) {
            this.total = total;
            this.organizersWithEvents = organizersWithEvents;
            this.completed = 0;
        }

        synchronized void onOrganizerProcessed() {
            completed++;
            if (completed == total) {
                runOnUiThread(() -> {
                    if (organizersWithEvents.isEmpty()) {
                        adapter.setItems(null);
                        handleLoadSuccess(false);
                    } else {
                        adapter.setItems(organizersWithEvents);
                        handleLoadSuccess(true);
                    }
                });
            }
        }
    }
}
