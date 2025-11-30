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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.WaitlistAdapter;
import ca.ualberta.codarc.codarc_events.controllers.NotifyWaitlistController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;

/**
 * Displays list of entrants on the waitlist for an event.
 */
public class ManageWaitlistActivity extends BaseEntrantListActivity {

    private WaitlistAdapter adapter;
    private MaterialButton btnNotifyWaitlist;
    private NotifyWaitlistController notifyController;
    private List<WaitlistAdapter.WaitlistItem> itemList;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_manage_waitlist;
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
    protected void initializeActivity() {
        notifyController = new NotifyWaitlistController(eventDB, entrantDB);
        btnNotifyWaitlist = findViewById(R.id.btn_notify_waitlist);
        setupNotifyButton();
    }

    @Override
    protected void loadData() {
        loadWaitlist();
    }

    private void loadWaitlist() {
        eventDB.getWaitlist(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
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
                Log.e("ManageWaitlistActivity", "Failed to load waitlist", e);
                Toast.makeText(ManageWaitlistActivity.this, "Failed to load entrants", Toast.LENGTH_SHORT).show();
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
            Object requestTimeObj = entry.get("requestTime");

            entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
                @Override
                public void onSuccess(Entrant entrant) {
                    String name = deviceId;
                    if (entrant != null && entrant.getName() != null && !entrant.getName().isEmpty()) {
                        name = entrant.getName();
                    }
                    long timestamp = parseTimestamp(requestTimeObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, name, timestamp));
                    
                    checkAndUpdateUI(completed, totalEntries);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    long timestamp = parseTimestamp(requestTimeObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, deviceId, timestamp));
                    
                    checkAndUpdateUI(completed, totalEntries);
                }
            });
        }
    }

    private void checkAndUpdateUI(int[] completed, int totalEntries) {
        completed[0]++;
        if (completed[0] == totalEntries) {
            sortByTime();
            adapter.notifyDataSetChanged();
            hideEmptyState();
        }
    }


    private void sortByTime() {
        Collections.sort(itemList, (a, b) -> Long.compare(a.getRequestTime(), b.getRequestTime()));
    }


    private void setupNotifyButton() {
        btnNotifyWaitlist.setOnClickListener(v -> showNotifyDialog());
    }

    private void updateNotifyButtonState(int waitlistCount) {
        btnNotifyWaitlist.setEnabled(waitlistCount > 0);
    }

    private void showNotifyDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notify_waitlist, null);
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
                handleNotifyWaitlist(message, dialog);
            });
        });

        dialog.show();
    }

    private void handleNotifyWaitlist(String message, AlertDialog dialog) {
        notifyController.notifyWaitlist(eventId, message, new NotifyWaitlistController.NotifyWaitlistCallback() {
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
                Toast.makeText(ManageWaitlistActivity.this, resultMessage, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("ManageWaitlistActivity", "Failed to notify waitlist", e);
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Failed to send notification";
                }
                Toast.makeText(ManageWaitlistActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
