package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.CancelledAdapter;
import ca.ualberta.codarc.codarc_events.adapters.WaitlistAdapter;
import ca.ualberta.codarc.codarc_events.controllers.NotifyCancelledController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Displays list of cancelled entrants and allows drawing replacements and notifying them.
 */
public class ViewCancelledActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CancelledAdapter adapter;
    private TextView emptyState;
    private EventDB eventDB;
    private EntrantDB entrantDB;
    private NotifyCancelledController notifyController;
    private String eventId;
    private List<WaitlistAdapter.WaitlistItem> itemList;
    private MaterialButton btnNotifyCancelled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_cancelled);

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventDB = new EventDB();
        entrantDB = new EntrantDB();
        notifyController = new NotifyCancelledController(eventDB, entrantDB);
        itemList = new ArrayList<>();

        recyclerView = findViewById(R.id.rv_entrants);
        emptyState = findViewById(R.id.tv_empty_state);
        btnNotifyCancelled = findViewById(R.id.btn_notify_cancelled);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CancelledAdapter(itemList, deviceId -> showReplaceDialog(deviceId));
        recyclerView.setAdapter(adapter);

        setupNotifyButton();

        verifyOrganizerAccess();
        loadCancelled();
    }

    private void verifyOrganizerAccess() {
        String deviceId = Identity.getOrCreateDeviceId(this);

        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null || event.getOrganizerId() == null || !event.getOrganizerId().equals(deviceId)) {
                    runOnUiThread(() -> {
                        Toast.makeText(ViewCancelledActivity.this, "Only event organizer can access this", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ViewCancelledActivity.this, "Failed to verify access", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
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
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, name, timestamp));

                    checkAndUpdateUI(completed, totalEntries);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    long timestamp = parseTimestamp(invitedAtObj);
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

    private long parseTimestamp(Object timestampObj) {
        if (timestampObj == null) {
            Log.w("ViewCancelledActivity", "Timestamp is null, using 0");
            return 0L;
        }

        if (timestampObj instanceof Timestamp) {
            Timestamp ts = (Timestamp) timestampObj;
            return ts.toDate().getTime();
        }

        if (timestampObj instanceof Long) {
            return (Long) timestampObj;
        }

        if (timestampObj instanceof Date) {
            return ((Date) timestampObj).getTime();
        }

        Log.w("ViewCancelledActivity", "Unknown timestamp type: " + timestampObj.getClass().getName());
        return 0L;
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

    private void showEmptyState() {
        recyclerView.setVisibility(android.view.View.GONE);
        emptyState.setVisibility(android.view.View.VISIBLE);
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(android.view.View.VISIBLE);
        emptyState.setVisibility(android.view.View.GONE);
    }

    /**
     * Sets up the notify button click listener.
     */
    private void setupNotifyButton() {
        btnNotifyCancelled.setOnClickListener(v -> showNotifyDialog());
    }

    /**
     * Updates the notify button state based on cancelled entrants count.
     * Button is enabled when cancelled entrants exist, disabled when empty.
     *
     * @param cancelledCount the number of cancelled entrants
     */
    private void updateNotifyButtonState(int cancelledCount) {
        if (btnNotifyCancelled == null) return;
        btnNotifyCancelled.setEnabled(cancelledCount > 0);
    }

    /**
     * Shows the dialog for composing and sending notification to cancelled entrants.
     */
    private void showNotifyDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notify_cancelled, null);
        TextInputEditText etMessage = dialogView.findViewById(R.id.et_message);
        TextView tvCharCount = dialogView.findViewById(R.id.tv_char_count);

        // Update character counter as user types
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s != null ? s.length() : 0;
                tvCharCount.setText(length + "/500");
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Send", null)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .create();

        // Override positive button to validate before dismissing
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String message = etMessage.getText() != null ? etMessage.getText().toString() : "";
                handleNotifyCancelled(message, dialog);
            });
        });

        dialog.show();
    }

    /**
     * Handles sending notification to cancelled entrants.
     * Delegates to controller for business logic.
     *
     * @param message the notification message
     * @param dialog the dialog to dismiss on success
     */
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
                // Keep dialog open on error so user can retry
            }
        });
    }
}

