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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.CancelledAdapter;
import ca.ualberta.codarc.codarc_events.adapters.WaitlistAdapter;
import ca.ualberta.codarc.codarc_events.controllers.NotifyCancelledController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.utils.FCMHelper;

/**
 * Displays list of cancelled entrants and allows drawing replacements and notifying them.
 */
public class ViewCancelledActivity extends BaseEntrantListActivity {

    private CancelledAdapter adapter;
    private NotifyCancelledController notifyController;
    private List<WaitlistAdapter.WaitlistItem> itemList;
    private MaterialButton btnNotifyCancelled;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_view_cancelled;
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
        adapter = new CancelledAdapter(itemList, deviceId -> showReplaceDialog(deviceId));
        recyclerView.setAdapter(adapter);
    }

            @Override
    protected boolean needsOrganizerAccess() {
        return true;
            }

            @Override
    protected void initializeActivity() {
        FCMHelper fcmHelper = createFCMHelperIfConfigured();
        notifyController = new NotifyCancelledController(eventDB, entrantDB, fcmHelper);
        btnNotifyCancelled = findViewById(R.id.btn_notify_cancelled);
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
        loadCancelled();
    }

    private void loadCancelled() {
        eventDB.getCancelled(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
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
                Log.e("ViewCancelledActivity", "Failed to load cancelled entrants", e);
                Toast.makeText(ViewCancelledActivity.this, "Failed to load cancelled entrants", Toast.LENGTH_SHORT).show();
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

            entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
                @Override
                public void onSuccess(Entrant entrant) {
                    String name = deviceId;
                    if (entrant != null && entrant.getName() != null && !entrant.getName().isEmpty()) {
                        name = entrant.getName();
                    }
                    long timestamp = parseTimestamp(invitedAtObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, name, timestamp, ""));

                    checkAndUpdateUI(completed, totalEntries);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    long timestamp = parseTimestamp(invitedAtObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, deviceId, timestamp, ""));
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


    private void showReplaceDialog(String cancelledDeviceId) {
        new AlertDialog.Builder(this)
                .setTitle("Draw Replacement")
                .setMessage("Draw a replacement entrant from the waitlist?")
                .setPositiveButton("Draw", (d, w) -> drawReplacement(cancelledDeviceId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void drawReplacement(String cancelledDeviceId) {
        eventDB.getWaitlist(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> waitlist) {
                if (waitlist == null || waitlist.isEmpty()) {
                    Toast.makeText(ViewCancelledActivity.this, "No entrants available for replacement", Toast.LENGTH_SHORT).show();
                    return;
                }

                Collections.shuffle(waitlist);
                if (waitlist.isEmpty() || waitlist.get(0) == null) {
                    Toast.makeText(ViewCancelledActivity.this, "No valid entrants available", Toast.LENGTH_SHORT).show();
                    return;
                }

                Object deviceIdObj = waitlist.get(0).get("deviceId");
                if (deviceIdObj == null) {
                    Toast.makeText(ViewCancelledActivity.this, "Invalid entrant data", Toast.LENGTH_SHORT).show();
                    return;
                }
                String replacementId = deviceIdObj.toString();

                eventDB.markReplacement(eventId, replacementId, new EventDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void ignore) {
                        Toast.makeText(ViewCancelledActivity.this, "Replacement drawn successfully", Toast.LENGTH_SHORT).show();
                        loadCancelled();
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Toast.makeText(ViewCancelledActivity.this, "Failed to draw replacement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(ViewCancelledActivity.this, "Failed to load waitlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setupNotifyButton() {
        btnNotifyCancelled.setOnClickListener(v -> showNotifyDialog());
    }

    private void updateNotifyButtonState(int cancelledCount) {
        if (btnNotifyCancelled == null) return;
        btnNotifyCancelled.setEnabled(cancelledCount > 0);
    }

    private void showNotifyDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notify_cancelled, null);
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
                handleNotifyCancelled(message, dialog);
            });
        });

        dialog.show();
    }

    private void handleNotifyCancelled(String message, AlertDialog dialog) {
        notifyController.notifyCancelled(eventId, message, new NotifyCancelledController.NotifyCancelledCallback() {
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
                Toast.makeText(ViewCancelledActivity.this, resultMessage, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("ViewCancelledActivity", "Failed to notify cancelled entrants", e);
                String errorMessage = e.getMessage();
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Failed to send notification";
                }
                Toast.makeText(ViewCancelledActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}

