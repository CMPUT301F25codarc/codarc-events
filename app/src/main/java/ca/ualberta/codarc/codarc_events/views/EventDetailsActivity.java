package ca.ualberta.codarc.codarc_events.views;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.controllers.JoinWaitlistController;
import ca.ualberta.codarc.codarc_events.controllers.LeaveWaitlistController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * Event details screen. Displays event info and regenerates QR from stored data.
 */
public class EventDetailsActivity extends AppCompatActivity {

    private Event event;
    private EventDB eventDB;
    private EntrantDB entrantDB;
    private JoinWaitlistController joinController;
    private LeaveWaitlistController leaveController;
    private MaterialButton joinBtn;
    private MaterialButton leaveBtn;
    private ImageButton settingsBtn;
    private String deviceId;

    /**
     * Initializes the event details screen, populating UI from the Event passed via Intent.
     * Displays event information and regenerates the QR code from stored data.
     *
     * @param savedInstanceState previously saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Initialize fields
        this.event = (Event) getIntent().getSerializableExtra("event");
        if (event == null) {
            Log.e("EventDetailsActivity", "Event not found in Intent");
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        this.eventDB = new EventDB();
        this.entrantDB = new EntrantDB();
        this.deviceId = Identity.getOrCreateDeviceId(this);
        this.joinController = new JoinWaitlistController(eventDB, entrantDB);
        this.leaveController = new LeaveWaitlistController(eventDB);
        this.joinBtn = findViewById(R.id.btn_join_waitlist);
        this.leaveBtn = findViewById(R.id.btn_leave_waitlist);

        // UI references
        TextView title = findViewById(R.id.event_title);
        TextView desc = findViewById(R.id.event_desc);
        TextView dateTime = findViewById(R.id.event_datetime);
        TextView regWindow = findViewById(R.id.event_reg_window);
        ImageView qrImage = findViewById(R.id.event_qr);

        title.setText(event.getName() != null ? event.getName() : "");
        desc.setText(event.getDescription() != null ? event.getDescription() : "");
        String eventDateTime = event.getEventDateTime();
        dateTime.setText(eventDateTime != null ? eventDateTime : "");
        
        TextView location = findViewById(R.id.event_location);
        String eventLocation = event.getLocation();
        location.setText("Location: " + (eventLocation != null && !eventLocation.isEmpty() ? eventLocation : "TBD"));
        
        String regOpen = event.getRegistrationOpen();
        String regClose = event.getRegistrationClose();
        regWindow.setText("Registration: " + (regOpen != null ? regOpen : "") + " → " + (regClose != null ? regClose : ""));

        // Generate QR code with null safety
        try {
            String qrData = event.getQrCode();
            if (qrData == null || qrData.isEmpty()) {
                qrData = "event:" + event.getId();
                Log.w("EventDetailsActivity", "QR code missing, using fallback: " + qrData);
            }
            BarcodeEncoder encoder = new BarcodeEncoder();
            Bitmap qrBitmap = encoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 600, 600);
            qrImage.setImageBitmap(qrBitmap);
        } catch (Exception e) {
            Log.e("EventDetailsActivity", "Failed to generate QR code", e);
            Toast.makeText(this, "Failed to display QR code", Toast.LENGTH_SHORT).show();
        }

        // Initially hide leave button
        leaveBtn.setVisibility(View.GONE);

        // Set up button handlers
        joinBtn.setOnClickListener(v -> showJoinConfirmation());
        leaveBtn.setOnClickListener(v -> showLeaveConfirmation());

        // Check waitlist status on load
        checkWaitlistStatus();

        // Show settings icon if organizer
        setupOrganizerSettings();
    }

    private void setupOrganizerSettings() {
        settingsBtn = findViewById(R.id.btn_event_settings);
        if (settingsBtn == null) {
            return;
        }

        if (event.getOrganizerId() != null && event.getOrganizerId().equals(deviceId)) {
            settingsBtn.setVisibility(View.VISIBLE);
            settingsBtn.setOnClickListener(v -> {
                Intent intent = new Intent(this, EventSettingsActivity.class);
                intent.putExtra("event", event);
                startActivity(intent);
            });
        } else {
            settingsBtn.setVisibility(View.GONE);
        }
    }

    /**
     * Checks if current user is on the waitlist and updates UI accordingly.
     */
    private void checkWaitlistStatus() {
        if (event == null || deviceId == null) {
            return;
        }

        eventDB.isEntrantOnWaitlist(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isOnWaitlist) {
                runOnUiThread(() -> updateButtonVisibility(isOnWaitlist));
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("EventDetailsActivity", "Failed to check waitlist status", e);
                runOnUiThread(() -> updateButtonVisibility(false));
            }
        });
    }

    /**
     * Updates button visibility based on waitlist status.
     */
    private void updateButtonVisibility(boolean isOnWaitlist) {
        if (isOnWaitlist) {
            joinBtn.setVisibility(View.GONE);
            leaveBtn.setVisibility(View.VISIBLE);
        } else {
            joinBtn.setVisibility(View.VISIBLE);
            leaveBtn.setVisibility(View.GONE);
        }
    }

    /**
     * Shows confirmation dialog for joining waitlist.
     */
    private void showJoinConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Join Waitlist")
                .setMessage("Are you sure you want to join the waitlist for this event?")
                .setPositiveButton("Join", (dialog, which) -> performJoin())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Performs the join waitlist operation with validation.
     * Uses JoinWaitlistController to handle business logic.
     */
    private void performJoin() {
        joinController.joinWaitlist(event, deviceId, new JoinWaitlistController.Callback() {
            @Override
            public void onResult(JoinWaitlistController.JoinResult result) {
                runOnUiThread(() -> {
                    if (result.needsProfileRegistration()) {
                        Intent intent = new Intent(EventDetailsActivity.this, ProfileCreationActivity.class);
                        startActivity(intent);
                        return;
                    }

                    if (result.isSuccess()) {
                        Toast.makeText(EventDetailsActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                        checkWaitlistStatus();
                    } else {
                        Toast.makeText(EventDetailsActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * Shows confirmation dialog for leaving waitlist.
     */
    private void showLeaveConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Waitlist")
                .setMessage("Are you sure you want to leave the waitlist for this event?")
                .setPositiveButton("Leave", (dialog, which) -> performLeave())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Performs the leave waitlist operation.
     * Uses LeaveWaitlistController to handle business logic.
     */
    private void performLeave() {
        leaveController.leaveWaitlist(event, deviceId, new LeaveWaitlistController.Callback() {
            @Override
            public void onResult(LeaveWaitlistController.LeaveResult result) {
                runOnUiThread(() -> {
                    Toast.makeText(EventDetailsActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                    if (result.isSuccess()) {
                        checkWaitlistStatus();
                    }
                });
            }
        });
    }

}
