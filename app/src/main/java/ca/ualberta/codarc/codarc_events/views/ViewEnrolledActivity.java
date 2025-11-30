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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Locale;

import android.content.Intent;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.WaitlistAdapter;
import ca.ualberta.codarc.codarc_events.controllers.NotifyEnrolledController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.FCMHelper;

/**
 * Displays list of enrolled entrants for an event.
 * Allows organizer to send broadcast notifications to all enrolled entrants.
 */
public class ViewEnrolledActivity extends BaseEntrantListActivity {

    private WaitlistAdapter adapter;
    private MaterialButton btnNotifyEnrolled;
    private MaterialButton btnExportCsv;
    private NotifyEnrolledController notifyController;
    private List<WaitlistAdapter.WaitlistItem> itemList;
    private Event currentEvent;

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
        FCMHelper fcmHelper = createFCMHelperIfConfigured();
        notifyController = new NotifyEnrolledController(eventDB, entrantDB, fcmHelper);
        btnNotifyEnrolled = findViewById(R.id.btn_notify_enrolled);
        btnExportCsv = findViewById(R.id.btn_export_csv);
        setupNotifyButton();
        setupExportButton();
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
        loadEnrolled();
        setupExportButton();
    }

    private void loadEnrolled() {
        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                currentEvent = event;
                loadEnrolledList();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("ViewEnrolledActivity", "Failed to load event", e);
                Toast.makeText(ViewEnrolledActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEnrolledList() {
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
        EntrantNameAggregator aggregator = new EntrantNameAggregator(totalEntries, () -> {
            adapter.notifyDataSetChanged();
            hideEmptyState();
        });

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
                    String email = (entrant != null && entrant.getEmail() != null) ? entrant.getEmail() : "";
                    long timestamp = parseTimestamp(respondedAtObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, name, timestamp, email));

                    aggregator.onEntrantFetched();
                }

                @Override
                public void onError(@NonNull Exception e) {
                    Log.w("ViewEnrolledActivity", "Failed to fetch profile for " + deviceId, e);
                    long timestamp = parseTimestamp(respondedAtObj);
                    itemList.add(new WaitlistAdapter.WaitlistItem(deviceId, deviceId, timestamp, ""));

                    aggregator.onEntrantFetched();
                }
            });
        }
    }

    private static class EntrantNameAggregator {
        private final int total;
        private final Runnable onComplete;
        private int completed = 0;

        EntrantNameAggregator(int total, Runnable onComplete) {
            this.total = total;
            this.onComplete = onComplete;
        }

        synchronized void onEntrantFetched() {
            completed++;
            if (completed == total) {
                onComplete.run();
            }
        }
    }

    private void setupNotifyButton() {
        btnNotifyEnrolled.setOnClickListener(v -> showNotifyDialog());
    }

    private void setupExportButton() {
        if (btnExportCsv != null) {
            btnExportCsv.setOnClickListener(v -> exportAsCsv());
        }
    }

    private void exportAsCsv() {
        if (itemList == null || itemList.isEmpty()) {
            Toast.makeText(this, R.string.export_csv_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentEvent == null) {
            Toast.makeText(this, R.string.export_csv_event_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvBuilder = new StringBuilder();
        
        csvBuilder.append("Event Details\n");
        csvBuilder.append("Event Name,").append(escapeCsvField(currentEvent.getName())).append("\n");
        csvBuilder.append("Event Date,").append(escapeCsvField(currentEvent.getEventDateTime())).append("\n");
        csvBuilder.append("Location,").append(escapeCsvField(currentEvent.getLocation())).append("\n");
        csvBuilder.append("Registration Close,").append(escapeCsvField(currentEvent.getRegistrationClose())).append("\n");
        csvBuilder.append("\n");
        
        csvBuilder.append("Name,Email,DeviceId,RespondedAt\n");
        
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        for (WaitlistAdapter.WaitlistItem item : itemList) {
            String time = item.getRequestTime() > 0
                    ? format.format(new Date(item.getRequestTime()))
                    : "";
            String name = escapeCsvField(item.getName());
            String email = escapeCsvField(item.getEmail());
            String deviceId = escapeCsvField(item.getDeviceId());
            
            csvBuilder.append(name).append(",")
                    .append(email).append(",")
                    .append(deviceId).append(",")
                    .append(escapeCsvField(time))
                    .append("\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Enrolled entrants - " + currentEvent.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, csvBuilder.toString());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_csv_button)));
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        String escaped = field.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
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

    private void setupExportButton() {
        if (exportButton != null) {
            exportButton.setOnClickListener(v -> shareAsCsv());
        }
    }

    private void shareAsCsv() {
        if (itemList == null || itemList.isEmpty()) {
            Toast.makeText(this, R.string.export_csv_error, Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Name,DeviceId,RespondedAt\n");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        for (WaitlistAdapter.WaitlistItem item : itemList) {
            String time = item.getRequestTime() > 0
                    ? format.format(new Date(item.getRequestTime()))
                    : "";
            csvBuilder.append('"').append(item.getName().replace("\"", "\"\""))
                    .append("\",")
                    .append('"').append(item.getDeviceId()).append("\",")
                    .append('"').append(time).append('"')
                    .append("\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Enrolled entrants");
        shareIntent.putExtra(Intent.EXTRA_TEXT, csvBuilder.toString());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.export_csv_button)));
    }
}
