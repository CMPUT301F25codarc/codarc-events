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
import ca.ualberta.codarc.codarc_events.adapters.WinnersAdapter;
import ca.ualberta.codarc.codarc_events.controllers.NotifyWinnersController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;

/**
 * Displays list of winners for an event.
 * Allows organizer to send broadcast notifications to all selected entrants.
 */
public class ViewWinnersActivity extends BaseEntrantListActivity {

    private WinnersAdapter adapter;
    private MaterialButton btnNotifyWinners;
    private NotifyWinnersController notifyController;
    private List<WinnersAdapter.WinnerItem> itemList;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_view_winners;
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
        adapter = new WinnersAdapter(itemList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected boolean needsOrganizerAccess() {
        return true;
    }

    @Override
    protected void initializeActivity() {
        notifyController = new NotifyWinnersController(eventDB, entrantDB);
        btnNotifyWinners = findViewById(R.id.btn_notify_winners);
        setupNotifyButton();
    }

    @Override
    protected void loadData() {
        loadWinners();
    }

    private void loadWinners() {
        eventDB.getWinners(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
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
                Log.e("ViewWinnersActivity", "Failed to load winners", e);
                Toast.makeText(ViewWinnersActivity.this, "Failed to load winners", Toast.LENGTH_SHORT).show();
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

    private void setupNotifyButton() {
        btnNotifyWinners.setOnClickListener(v -> showNotifyDialog());
    }

    /**
     * Updates the notify button state based on winners count.
     *
     * @param winnersCount the number of selected entrants (winners)
     */
    private void updateNotifyButtonState(int winnersCount) {
        btnNotifyWinners.setEnabled(winnersCount > 0);
    }

    private void showNotifyDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notify_winners, null);
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
                handleNotifyWinners(message, dialog);
            });
        });

        dialog.show();
    }

    /**
     * Handles sending notification to selected entrants.
     * Delegates to controller for business logic.
     *
     * @param message the notification message
     * @param dialog the dialog to dismiss on success
     */
    private void handleNotifyWinners(String message, AlertDialog dialog) {
        notifyController.notifyWinners(eventId, message, new NotifyWinnersController.NotifyWinnersCallback() {
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
                Toast.makeText(ViewWinnersActivity.this, resultMessage, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("ViewWinnersActivity", "Failed to notify winners", e);
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Failed to send notification";
                }
                Toast.makeText(ViewWinnersActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}

