package ca.ualberta.codarc.codarc_events.views;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.AdminNotificationLogAdapter;
import ca.ualberta.codarc.codarc_events.controllers.NotificationLogController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;

/**
 * Displays a list of all notification logs for administrators to review.
 * Shows all notifications sent by organizers to entrants.
 */
public class AdminNotificationLogActivity extends BaseAdminListActivity {

    private AdminNotificationLogAdapter adapter;
    private NotificationLogController controller;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_admin_notification_log;
    }

    @Override
    protected int getRecyclerViewId() {
        return R.id.rv_admin_notification_logs;
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
        adapter = new AdminNotificationLogAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void initializeActivity() {
        EntrantDB entrantDB = new EntrantDB();
        EventDB eventDB = new EventDB();
        controller = new NotificationLogController(entrantDB, eventDB);
    }

    @Override
    protected void loadData() {
        resetUIStates();

        controller.loadNotificationLogs(new NotificationLogController.NotificationLogCallback() {
            @Override
            public void onSuccess(List<Map<String, Object>> logs) {
                runOnUiThread(() -> {
                    if (logs == null || logs.isEmpty()) {
                        adapter.setItems(null);
                        handleLoadSuccess(false);
                    } else {
                        adapter.setItems(logs);
                        handleLoadSuccess(true);
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                handleLoadError(e, "AdminNotificationLogActivity");
            }
        });
    }
}
