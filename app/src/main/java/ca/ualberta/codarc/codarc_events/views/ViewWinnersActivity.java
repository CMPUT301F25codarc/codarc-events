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
import java.util.Date;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.WinnersAdapter;
import ca.ualberta.codarc.codarc_events.controllers.NotifyWinnersController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Displays list of winners for an event.
 * Allows organizer to send broadcast notifications to all selected entrants.
 */
public class ViewWinnersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WinnersAdapter adapter;
    private TextView emptyState;
    private MaterialButton btnNotifyWinners;
    private EventDB eventDB;
    private EntrantDB entrantDB;
    private NotifyWinnersController notifyController;
    private String eventId;
    private List<WinnersAdapter.WinnerItem> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_winners);

        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventDB = new EventDB();
        entrantDB = new EntrantDB();
        notifyController = new NotifyWinnersController(eventDB, entrantDB);
        itemList = new ArrayList<>();

        recyclerView = findViewById(R.id.rv_entrants);
        emptyState = findViewById(R.id.tv_empty_state);
        btnNotifyWinners = findViewById(R.id.btn_notify_winners);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WinnersAdapter(itemList, this::cancelWinner);
        recyclerView.setAdapter(adapter);

        setupNotifyButton();
        verifyOrganizerAccess();
        loadWinners();
    }

    private void verifyOrganizerAccess() {
        String deviceId = Identity.getOrCreateDeviceId(this);

        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null || event.getOrganizerId() == null || !event.getOrganizerId().equals(deviceId)) {
                    runOnUiThread(() -> {
                        Toast.makeText(ViewWinnersActivity.this, "Only event organizer can access this", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(ViewWinnersActivity.this, "Failed to verify access", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
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

    private long parseTimestamp(Object timestampObj) {
        if (timestampObj == null) {
            Log.w("ViewWinnersActivity", "Timestamp is null, using 0");
            return 0L;
        }
        if (timestampObj instanceof Timestamp) {
            return ((Timestamp) timestampObj).toDate().getTime();
        }
        if (timestampObj instanceof Long) {
            return (Long) timestampObj;
        }
        if (timestampObj instanceof Date) {
            return ((Date) timestampObj).getTime();
        }
        Log.w("ViewWinnersActivity", "Unknown timestamp type: " + timestampObj.getClass().getName());
        return 0L;
    }

    private void showEmptyState() {
        recyclerView.setVisibility(android.view.View.GONE);
        emptyState.setVisibility(android.view.View.VISIBLE);
    }

    private void hideEmptyState() {
        recyclerView.setVisibility(android.view.View.VISIBLE);
        emptyState.setVisibility(android.view.View.GONE);
    }

    private void cancelWinner(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            Toast.makeText(this, "Invalid entrant", Toast.LENGTH_SHORT).show();
            return;
        }

        eventDB.setEnrolledStatus(eventId, deviceId, false, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Toast.makeText(ViewWinnersActivity.this, "Entrant cancelled", Toast.LENGTH_SHORT).show();
                loadWinners();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(ViewWinnersActivity.this, "Failed to cancel entrant", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sets up the notify button click listener.
     */
    private void setupNotifyButton() {
        btnNotifyWinners.setOnClickListener(v -> showNotifyDialog());
    }

    /**
     * Updates the notify button state based on winners count.
     * Button is enabled when winners exist, disabled when empty.
     *
     * @param winnersCount the number of selected entrants (winners)
     */
    private void updateNotifyButtonState(int winnersCount) {
        btnNotifyWinners.setEnabled(winnersCount > 0);
    }

    /**
     * Shows the dialog for composing and sending notification to selected entrants.
     */
    private void showNotifyDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notify_winners, null);
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
                // Keep dialog open on error so user can retry
            }
        });
    }
}
