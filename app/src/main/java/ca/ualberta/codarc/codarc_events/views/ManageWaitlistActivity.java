package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
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
import ca.ualberta.codarc.codarc_events.adapters.WaitlistAdapter;
import ca.ualberta.codarc.codarc_events.controllers.NotifyWaitlistController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;

/**
 * Displays list of entrants on the waitlist for an event.
 * Shows entrant names and request timestamps.
 * Allows organizer to send broadcast notifications to all waitlisted entrants.
 */
public class ManageWaitlistActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WaitlistAdapter adapter;
    private TextView emptyState;
    private MaterialButton btnNotifyWaitlist;
    private EventDB eventDB;
    private EntrantDB entrantDB;
    private NotifyWaitlistController notifyController;
    private String eventId;
    private List<WaitlistAdapter.WaitlistItem> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_waitlist);

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventDB = new EventDB();
        entrantDB = new EntrantDB();
        notifyController = new NotifyWaitlistController(eventDB, entrantDB);
        itemList = new ArrayList<>();

        recyclerView = findViewById(R.id.rv_entrants);
        emptyState = findViewById(R.id.tv_empty_state);
        btnNotifyWaitlist = findViewById(R.id.btn_notify_waitlist);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WaitlistAdapter(itemList);
        recyclerView.setAdapter(adapter);

        setupNotifyButton();
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
                    // use deviceId if can't get name
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

    private long parseTimestamp(Object requestTimeObj) {
        if (requestTimeObj == null) {
            Log.w("ManageWaitlistActivity", "Request time is null, using 0");
            return 0L;
        }

        if (requestTimeObj instanceof Timestamp) {
            Timestamp ts = (Timestamp) requestTimeObj;
            return ts.toDate().getTime();
        }

        if (requestTimeObj instanceof Long) {
            return (Long) requestTimeObj;
        }

        if (requestTimeObj instanceof Date) {
            return ((Date) requestTimeObj).getTime();
        }

        Log.w("ManageWaitlistActivity", "Unknown timestamp type: " + requestTimeObj.getClass().getName());
        return 0L;
    }

    private void sortByTime() {
        Collections.sort(itemList, (a, b) -> Long.compare(a.getRequestTime(), b.getRequestTime()));
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    /**
     * Sets up the notify button click listener.
     */
    private void setupNotifyButton() {
        btnNotifyWaitlist.setOnClickListener(v -> showNotifyDialog());
    }

    /**
     * Updates the notify button state based on waitlist count.
     * Button is enabled when waitlist has entries, disabled when empty.
     *
     * @param waitlistCount the number of entrants on the waitlist
     */
    private void updateNotifyButtonState(int waitlistCount) {
        btnNotifyWaitlist.setEnabled(waitlistCount > 0);
    }

    /**
     * Shows the dialog for composing and sending notification to waitlist.
     */
    private void showNotifyDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notify_waitlist, null);
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
                handleNotifyWaitlist(message, dialog);
            });
        });

        dialog.show();
    }

    /**
     * Handles sending notification to waitlist.
     * Delegates to controller for business logic.
     *
     * @param message the notification message
     * @param dialog the dialog to dismiss on success
     */
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
                // Keep dialog open on error so user can retry
            }
        });
    }
}
