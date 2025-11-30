package ca.ualberta.codarc.codarc_events.views;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import ca.ualberta.codarc.codarc_events.utils.TextWatcherHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.WaitlistAdapter;
import ca.ualberta.codarc.codarc_events.controllers.NotifyEnrolledController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;

/**
 * Displays list of enrolled entrants for an event.
 * Allows organizer to send broadcast notifications to all enrolled entrants.
 */
public class ViewEnrolledActivity extends BaseEntrantListActivity {

    private WaitlistAdapter adapter;
    private MaterialButton btnNotifyEnrolled;
    private NotifyEnrolledController notifyController;
    private List<WaitlistAdapter.WaitlistItem> itemList;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_view_enrolled;
    }

    @Override
    protected int getRecyclerViewId() {
        return R.id.rv_entrants;
    }

    @Override
    protected int getEmptyStateId() {
        return R.id.tv_empty_state;
    }

    @Override
    protected void setupAdapter() {
        itemList = new ArrayList<>();
        adapter = new WaitlistAdapter(itemList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected boolean needsOrganizerAccess() {
        return true;
    }

    @Override
    protected void initializeActivity() {
        notifyController = new NotifyEnrolledController(eventDB, entrantDB);
        btnNotifyEnrolled = findViewById(R.id.btn_notify_enrolled);
        setupNotifyButton();
    }

    @Override
    protected void loadData() {
        loadEnrolled();
    }

    private void loadEnrolled() {
        eventDB.getEnrolled(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> entries) {
                if (entries == null || entries.isEmpty()) {
                    showEmptyState();
                    updateNotifyButtonState(0);
                    return;
                }
                updateNotifyButtonState(entries.size());
                fetchEntrantNames(entries);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("ViewEnrolledActivity", "Failed to load enrolled entrants", e);
                Toast.makeText(ViewEnrolledActivity.this, "Failed to load enrolled entrants", Toast.LENGTH_SHORT).show();
                updateNotifyButtonState(0);
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
            Object respondedAtObj = entry.get("respondedAt");

            entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
                @Override
                public void onSuccess(Entrant entrant) {
                    String name = deviceId;
                    if (entrant != null && entrant.getName() != null && !entrant.getName().isEmpty()) {
                        name = entrant.getName();
                    }
                    long timestamp = parseTimestamp(respondedAtObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, name, timestamp));

                    checkAndUpdateUI(completed, totalEntries);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    Log.w("ViewEnrolledActivity", "Failed to fetch profile for " + deviceId, e);
                    long timestamp = parseTimestamp(respondedAtObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, deviceId, timestamp));

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

    private void setupNotifyButton() {
        btnNotifyEnrolled.setOnClickListener(v -> showNotifyDialog());
    }

    /**
     * Updates the notify button state based on enrolled count.
     *
     * @param enrolledCount the number of enrolled entrants
     */
    private void updateNotifyButtonState(int enrolledCount) {
        if (btnNotifyEnrolled == null) return;
        btnNotifyEnrolled.setEnabled(enrolledCount > 0);
    }

    private void showNotifyDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notify_enrolled, null);
        TextInputEditText etMessage = dialogView.findViewById(R.id.et_message);
        TextView tvCharCount = dialogView.findViewById(R.id.tv_char_count);

        etMessage.addTextChangedListener(TextWatcherHelper.createCharCountWatcher(tvCharCount, 500));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Send", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String message = etMessage.getText() != null ? etMessage.getText().toString() : "";
                handleNotifyEnrolled(message, dialog);
            });
        });

        dialog.show();
    }

    /**
     * Handles sending notification to enrolled entrants.
     * Delegates to controller for business logic.
     *
     * @param message the notification message
     * @param dialog the dialog to dismiss on success
     */
    private void handleNotifyEnrolled(String message, AlertDialog dialog) {
        notifyController.notifyEnrolled(eventId, message, new NotifyEnrolledController.NotifyEnrolledCallback() {
            @Override
            public void onSuccess(int notifiedCount, int failedCount) {
                dialog.dismiss();
                String resultMessage;
                if (failedCount == 0) {
                    resultMessage = "Notification sent to " + notifiedCount + " entrant(s)";
                } else {
                    resultMessage = "Notification sent to " + notifiedCount + " entrant(s). " +
                            failedCount + " failed.";
                }
                Toast.makeText(ViewEnrolledActivity.this, resultMessage, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("ViewEnrolledActivity", "Failed to notify enrolled entrants", e);
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Failed to send notification";
                }
                Toast.makeText(ViewEnrolledActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}

