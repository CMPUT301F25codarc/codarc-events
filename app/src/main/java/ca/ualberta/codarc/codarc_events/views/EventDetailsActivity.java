package ca.ualberta.codarc.codarc_events.views;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
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
    private MaterialButton joinBtn;
    private MaterialButton leaveBtn;
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
        this.deviceId = Identity.getOrCreateDeviceId(this);
        this.joinBtn = findViewById(R.id.btn_join_waitlist);
        this.leaveBtn = findViewById(R.id.btn_leave_waitlist);

        // UI references
        TextView title = findViewById(R.id.event_title);
        TextView desc = findViewById(R.id.event_desc);
        TextView dateTime = findViewById(R.id.event_datetime);
        TextView regWindow = findViewById(R.id.event_reg_window);
        ImageView qrImage = findViewById(R.id.event_qr);

        title.setText(event.getName());
        desc.setText(event.getDescription());
        dateTime.setText(event.getEventDateTime());
        
        TextView location = findViewById(R.id.event_location);
        String eventLocation = event.getLocation();
        location.setText("Location: " + (eventLocation != null && !eventLocation.isEmpty() ? eventLocation : "TBD"));
        
        regWindow.setText("Registration: " + event.getRegistrationOpen() + " → " + event.getRegistrationClose());

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
     */
    private void performJoin() {
        // Check profile registration first
        EntrantDB entrantDB = new EntrantDB();
        entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
            @Override
            public void onSuccess(Entrant entrant) {
                boolean isRegistered = entrant != null && entrant.getIsRegistered();
                if (!isRegistered) {
                    runOnUiThread(() -> {
                        Intent intent = new Intent(EventDetailsActivity.this, ProfileCreationActivity.class);
                        startActivity(intent);
                    });
                    return;
                }

                // Check if already joined
                eventDB.isEntrantOnWaitlist(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean alreadyJoined) {
                        if (alreadyJoined) {
                            runOnUiThread(() -> {
                                Toast.makeText(EventDetailsActivity.this,
                                        "Already joined", Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }

                        // Validate conditions (registration window, capacity)
                        validateJoinConditions(event,
                                () -> {
                                    // All validations passed, join waitlist
                                    eventDB.joinWaitlist(event.getId(), deviceId, new EventDB.Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void value) {
                                            runOnUiThread(() -> {
                                                Toast.makeText(EventDetailsActivity.this,
                                                        "Joined successfully", Toast.LENGTH_SHORT).show();
                                                checkWaitlistStatus();
                                            });
                                        }

                                        @Override
                                        public void onError(@NonNull Exception e) {
                                            Log.e("EventDetailsActivity", "Failed to join waitlist", e);
                                            runOnUiThread(() -> {
                                                Toast.makeText(EventDetailsActivity.this,
                                                        "Failed to join. Please try again.", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    });
                                },
                                () -> {
                                    // Validation failed (error already shown)
                                }
                        );
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Log.e("EventDetailsActivity", "Failed to check join status", e);
                        runOnUiThread(() -> {
                            Toast.makeText(EventDetailsActivity.this,
                                    "Failed to check status. Please try again.", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("EventDetailsActivity", "Failed to check profile", e);
                runOnUiThread(() -> {
                    Toast.makeText(EventDetailsActivity.this,
                            "Failed to check profile", Toast.LENGTH_SHORT).show();
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
     */
    private void performLeave() {
        // Check if actually on waitlist
        eventDB.isEntrantOnWaitlist(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isOnWaitlist) {
                if (!isOnWaitlist) {
                    runOnUiThread(() -> {
                        Toast.makeText(EventDetailsActivity.this,
                                "You are not registered for this event", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Perform leave operation
                eventDB.leaveWaitlist(event.getId(), deviceId, new EventDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void value) {
                        runOnUiThread(() -> {
                            Toast.makeText(EventDetailsActivity.this,
                                    "You have left this event", Toast.LENGTH_SHORT).show();
                            checkWaitlistStatus();
                        });
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Log.e("EventDetailsActivity", "Failed to leave waitlist", e);
                        runOnUiThread(() -> {
                            Toast.makeText(EventDetailsActivity.this,
                                    "Failed to leave. Please try again.", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("EventDetailsActivity", "Failed to check leave status", e);
                runOnUiThread(() -> {
                    Toast.makeText(EventDetailsActivity.this,
                            "Failed to check status. Please try again.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Validates if current time is within the registration window.
     */
    private boolean isWithinRegistrationWindow(Event event) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            long now = System.currentTimeMillis();

            String regOpen = event.getRegistrationOpen();
            String regClose = event.getRegistrationClose();

            if (regOpen == null || regOpen.isEmpty() || regClose == null || regClose.isEmpty()) {
                return false;
            }

            long openTime = isoFormat.parse(regOpen).getTime();
            long closeTime = isoFormat.parse(regClose).getTime();

            return now >= openTime && now <= closeTime;
        } catch (java.text.ParseException e) {
            Log.e("EventDetailsActivity", "Error parsing registration window", e);
            return false;
        } catch (Exception e) {
            Log.e("EventDetailsActivity", "Unexpected error in registration window check", e);
            return false;
        }
    }

    /**
     * Validates all conditions for joining:
     * 1. Registration window is open
     * 2. Capacity limit not reached (if set)
     */
    private void validateJoinConditions(Event event, Runnable onValid, Runnable onInvalid) {
        // Check registration window
        if (!isWithinRegistrationWindow(event)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Registration window is closed", Toast.LENGTH_SHORT).show();
            });
            if (onInvalid != null) onInvalid.run();
            return;
        }

        // Check capacity (if maxCapacity is set)
        Integer maxCapacity = event.getMaxCapacity();
        if (maxCapacity != null && maxCapacity > 0) {
            eventDB.getWaitlistCount(event.getId(), new EventDB.Callback<Integer>() {
                @Override
                public void onSuccess(Integer currentCount) {
                    if (currentCount >= maxCapacity) {
                        runOnUiThread(() -> {
                            Toast.makeText(EventDetailsActivity.this,
                                    "Event is full", Toast.LENGTH_SHORT).show();
                        });
                        if (onInvalid != null) onInvalid.run();
                    } else {
                        if (onValid != null) onValid.run();
                    }
                }

                @Override
                public void onError(@NonNull Exception e) {
                    Log.e("EventDetailsActivity", "Failed to check capacity", e);
                    runOnUiThread(() -> {
                        Toast.makeText(EventDetailsActivity.this,
                                "Failed to check availability", Toast.LENGTH_SHORT).show();
                    });
                    if (onInvalid != null) onInvalid.run();
                }
            });
        } else {
            // No capacity limit, proceed
            if (onValid != null) onValid.run();
        }
    }
}
