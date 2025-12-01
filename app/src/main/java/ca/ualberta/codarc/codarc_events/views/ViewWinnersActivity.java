package ca.ualberta.codarc.codarc_events.views;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
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
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.controllers.EventValidationHelper;
import ca.ualberta.codarc.codarc_events.utils.FCMHelper;

/**
 * Displays list of winners for an event.
 * Allows organizer to send broadcast notifications to all selected entrants.
 */
public class ViewWinnersActivity extends BaseEntrantListActivity {

    private WinnersAdapter adapter;
    private MaterialButton btnNotifyWinners;
    private NotifyWinnersController notifyController;
    private List<WinnersAdapter.WinnerItem> itemList;
    private Event currentEvent;

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
        adapter = new WinnersAdapter(itemList, this::cancelWinner);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected boolean needsOrganizerAccess() {
        return true;
    }

    @Override
    protected void initializeActivity() {
        FCMHelper fcmHelper = createFCMHelperIfConfigured();
        notifyController = new NotifyWinnersController(eventDB, entrantDB, fcmHelper);
        btnNotifyWinners = findViewById(R.id.btn_notify_winners);
        setupNotifyButton();
        setupBackButton();
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> finish());
    }

    private FCMHelper createFCMHelperIfConfigured() {
        String functionUrl = getString(R.string.fcm_function_url);
        if (functionUrl != null && !functionUrl.isEmpty() && 
            !functionUrl.contains("YOUR_REGION") && !functionUrl.contains("YOUR_PROJECT_ID")) {
            return new FCMHelper(functionUrl);
        }
        return null;
    }

    @Override
    protected void loadData() {
        loadWinners();
    }

    private void loadWinners() {
        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                loadWinnersList();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("ViewWinnersActivity", "Failed to load event", e);
                Toast.makeText(ViewWinnersActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWinnersList() {
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
        WinnerNameAggregator aggregator = new WinnerNameAggregator(totalEntries, () -> {
            adapter.notifyDataSetChanged();
            hideEmptyState();
        });

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
                    aggregator.onWinnerFetched();
                }

                @Override
                public void onError(@NonNull Exception e) {
                    long timestamp = parseTimestamp(invitedAtObj);
                    itemList.add(new WinnersAdapter.WinnerItem(deviceId, deviceId, timestamp, isEnrolled));
                    aggregator.onWinnerFetched();
                }
            });
        }
    }

    private static class WinnerNameAggregator {
        private final int total;
        private final Runnable onComplete;
        private int completed = 0;

        WinnerNameAggregator(int total, Runnable onComplete) {
            this.total = total;
            this.onComplete = onComplete;
        }

        synchronized void onWinnerFetched() {
            completed++;
            if (completed == total) {
                onComplete.run();
            }
        }
    }

    private void setupNotifyButton() {
        btnNotifyWinners.setOnClickListener(v -> showNotifyDialog());
    }

    private void cancelWinner(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, R.string.cancel_entrant_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEvent == null || !EventValidationHelper.hasRegistrationDeadlinePassed(currentEvent)) {
            Toast.makeText(this, R.string.cancel_entrant_before_deadline, Toast.LENGTH_SHORT).show();
            return;
        }

        eventDB.setEnrolledStatus(eventId, deviceId, false, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Toast.makeText(ViewWinnersActivity.this, R.string.cancel_entrant_success, Toast.LENGTH_SHORT).show();
                loadWinners();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("ViewWinnersActivity", "Failed to cancel entrant", e);
                Toast.makeText(ViewWinnersActivity.this, R.string.cancel_entrant_error, Toast.LENGTH_SHORT).show();
            }
        });
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

