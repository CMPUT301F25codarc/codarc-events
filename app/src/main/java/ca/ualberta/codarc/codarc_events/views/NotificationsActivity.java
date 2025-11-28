package ca.ualberta.codarc.codarc_events.views;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.NotificationAdapter;
import ca.ualberta.codarc.codarc_events.controllers.InvitationResponseController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.NotificationEntry;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Displays the entrant notification inbox and allows responding to invitations.
 */
public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private TextView emptyStateView;
    private ProgressBar loadingView;

    private EntrantDB entrantDB;
    private EventDB eventDB;
    private InvitationResponseController invitationController;

    private final List<NotificationEntry> notifications = new ArrayList<>();
    private final Map<String, String> eventNameCache = new HashMap<>();

    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        deviceId = Identity.getOrCreateDeviceId(this);

        entrantDB = new EntrantDB();
        eventDB = new EventDB();
        invitationController = new InvitationResponseController(eventDB, entrantDB);

        recyclerView = findViewById(R.id.rv_notifications);
        emptyStateView = findViewById(R.id.tv_notifications_empty);
        loadingView = findViewById(R.id.pb_notifications_loading);
        ImageButton backButton = findViewById(R.id.btn_notifications_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(
                new NotificationAdapter.NotificationActionListener() {
                    @Override
                    public void onAccept(@NonNull NotificationEntry entry) {
                        handleInvitationResponse(entry, true);
                    }

                    @Override
                    public void onDecline(@NonNull NotificationEntry entry) {
                        handleInvitationResponse(entry, false);
                    }
                },
                new NotificationAdapter.NotificationClickListener() {
                    @Override
                    public void onNotificationClick(@NonNull NotificationEntry entry) {
                        navigateToEventDetails(entry);
                    }
                }
        );
        recyclerView.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        showLoading(true);
        entrantDB.getNotifications(deviceId, new EntrantDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> value) {
                runOnUiThread(() -> {
                    showLoading(false);
                    notifications.clear();
                    if (value == null || value.isEmpty()) {
                        adapter.setItems(notifications);
                        updateEmptyState(true);
                        return;
                    }
                    for (Map<String, Object> map : value) {
                        notifications.add(mapToEntry(map));
                    }
                    adapter.setItems(notifications);
                    updateEmptyState(notifications.isEmpty());
                    resolveEventNames();
                    cleanupDeletedEventNotifications();
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(NotificationsActivity.this, R.string.notification_response_error, Toast.LENGTH_SHORT).show();
                    updateEmptyState(true);
                });
            }
        });
    }

    private NotificationEntry mapToEntry(Map<String, Object> data) {
        NotificationEntry entry = new NotificationEntry();
        if (data == null) {
            return entry;
        }
        Object id = data.get("id");
        if (id != null) entry.setId(id.toString());
        Object eventId = data.get("eventId");
        if (eventId != null) entry.setEventId(eventId.toString());
        Object message = data.get("message");
        entry.setMessage(message != null ? message.toString() : "");
        Object category = data.get("category");
        if (category != null) entry.setCategory(category.toString());
        Object createdAt = data.get("createdAt");
        if (createdAt instanceof Number) {
            entry.setCreatedAt(((Number) createdAt).longValue());
        }
        Object read = data.get("read");
        entry.setRead(read instanceof Boolean && (Boolean) read);
        Object response = data.get("response");
        if (response != null) entry.setResponse(response.toString());
        Object respondedAt = data.get("respondedAt");
        if (respondedAt instanceof Number) {
            entry.setRespondedAt(((Number) respondedAt).longValue());
        }
        return entry;
    }

    private void resolveEventNames() {
        for (NotificationEntry entry : notifications) {
            String eventId = entry.getEventId();
            if (eventId == null || eventId.isEmpty()) {
                continue;
            }
            if (eventNameCache.containsKey(eventId)) {
                entry.setEventName(eventNameCache.get(eventId));
                adapter.updateItem(entry);
                continue;
            }
            eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
                @Override
                public void onSuccess(Event event) {
                    String name = event != null ? event.getName() : null;
                    if (name == null || name.isEmpty()) {
                        name = getString(R.string.notification_unknown_event);
                    }
                    eventNameCache.put(eventId, name);
                    entry.setEventName(name);
                    runOnUiThread(() -> adapter.updateItem(entry));
                }

                @Override
                public void onError(@NonNull Exception e) {
                    String fallback = getString(R.string.notification_unknown_event);
                    eventNameCache.put(eventId, fallback);
                    entry.setEventName(fallback);
                    runOnUiThread(() -> adapter.updateItem(entry));
                }
            });
        }
    }

    private void handleInvitationResponse(NotificationEntry entry, boolean accept) {
        if (entry == null || entry.getId() == null) {
            return;
        }
        entry.setProcessing(true);
        adapter.updateItem(entry);

        InvitationResponseController.ResponseCallback callback = new InvitationResponseController.ResponseCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    entry.setProcessing(false);
                    entry.setRead(true);
                    entry.setResponse(accept ? "accepted" : "declined");
                    adapter.updateItem(entry);
                    showResultDialog(accept);
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    entry.setProcessing(false);
                    adapter.updateItem(entry);
                    Toast.makeText(NotificationsActivity.this, R.string.notification_response_error, Toast.LENGTH_SHORT).show();
                });
            }
        };

        if (accept) {
            invitationController.acceptInvitation(entry.getEventId(), deviceId, entry.getId(), callback);
        } else {
            invitationController.declineInvitation(entry.getEventId(), deviceId, entry.getId(), callback);
        }
    }

    private void showResultDialog(boolean accepted) {
        int messageRes = accepted ? R.string.notification_accept_success : R.string.notification_decline_success;
        new AlertDialog.Builder(this)
                .setMessage(messageRes)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void showLoading(boolean show) {
        loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    /**
     * Cleans up notifications for events that have been deleted.
     * Checks if each notification's eventId exists in Firestore and removes
     * notifications for non-existent events.
     */
    private void cleanupDeletedEventNotifications() {
        // Create a copy to avoid ConcurrentModificationException
        List<NotificationEntry> entriesToCheck = new ArrayList<>(notifications);
        List<NotificationEntry> entriesToRemove = new ArrayList<>();
        
        for (NotificationEntry entry : entriesToCheck) {
            String eventId = entry.getEventId();
            if (eventId == null || eventId.isEmpty()) {
                continue;
            }
            
            eventDB.eventExists(eventId, new EventDB.Callback<Boolean>() {
                @Override
                public void onSuccess(Boolean exists) {
                    if (!exists) {
                        // Event doesn't exist, mark for removal
                        String notificationId = entry.getId();
                        if (notificationId != null && !notificationId.isEmpty()) {
                            entriesToRemove.add(entry);
                            // Remove from database
                            entrantDB.removeNotification(deviceId, notificationId, new EntrantDB.Callback<Void>() {
                                @Override
                                public void onSuccess(Void value) {
                                    runOnUiThread(() -> {
                                        // Remove from list and update adapter
                                        synchronized (notifications) {
                                            notifications.remove(entry);
                                        }
                                        adapter.setItems(new ArrayList<>(notifications));
                                        updateEmptyState(notifications.isEmpty());
                                    });
                                }
                                
                                @Override
                                public void onError(@NonNull Exception e) {
                                    Log.w("NotificationsActivity", "Failed to remove notification for deleted event", e);
                                }
                            });
                        }
                    }
                }
                
                @Override
                public void onError(@NonNull Exception e) {
                    // Log error but continue - don't fail cleanup if check fails
                    Log.w("NotificationsActivity", "Failed to check if event exists: " + eventId, e);
                }
            });
        }
    }

    /**
     * Navigates to EventDetailsActivity when a notification is clicked.
     * Fetches the event from Firestore using the notification's eventId.
     *
     * @param entry the notification entry that was clicked
     */
    private void navigateToEventDetails(@NonNull NotificationEntry entry) {
        String eventId = entry.getEventId();
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null) {
                    Toast.makeText(NotificationsActivity.this, "Event not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(NotificationsActivity.this, EventDetailsActivity.class);
                intent.putExtra("event", event);
                startActivity(intent);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("NotificationsActivity", "Failed to load event for navigation", e);
                Toast.makeText(NotificationsActivity.this, "Failed to load event details", Toast.LENGTH_SHORT).show();
            }
        });
    }
}