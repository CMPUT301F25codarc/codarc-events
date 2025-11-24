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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.controllers.JoinWaitlistController;
import ca.ualberta.codarc.codarc_events.controllers.LeaveWaitlistController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.DateHelper;
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
        ImageView eventBanner = findViewById(R.id.event_banner);

        title.setText(event.getName() != null ? event.getName() : "");
        desc.setText(event.getDescription() != null ? event.getDescription() : "");
        String eventDateTime = event.getEventDateTime();
        dateTime.setText(DateHelper.formatEventDate(eventDateTime));

        // Load poster image if available
        loadPosterImage(eventBanner, event.getPosterUrl());
        
        // Make poster clickable to view full screen (only if poster exists)
        if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
            eventBanner.setClickable(true);
            eventBanner.setFocusable(true);
            eventBanner.setContentDescription("Event Poster - Tap to view full screen");
            eventBanner.setOnClickListener(v -> openFullScreenImage(event.getPosterUrl()));
        }

        TextView location = findViewById(R.id.event_location);
        String eventLocation = event.getLocation();
        location.setText("Location: " + (eventLocation != null && !eventLocation.isEmpty() ? eventLocation : "TBD"));

        String regOpen  = DateHelper.formatEventDate(event.getRegistrationOpen());
        String regClose = DateHelper.formatEventDate(event.getRegistrationClose());
        regWindow.setText("Registration: " + (regOpen != null ? regOpen : "") + " → " + (regClose != null ? regClose : ""));

        // Display tags
        displayTags();

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

        // Show settings icon if organizer, hide join button
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
     * Checks if current user can join waitlist and updates UI accordingly.
     */
    private void checkWaitlistStatus() {
        if (event == null || deviceId == null) {
            return;
        }

        // Hide join button if user is the organizer
        if (event.getOrganizerId() != null && event.getOrganizerId().equals(deviceId)) {
            runOnUiThread(() -> {
                joinBtn.setVisibility(View.GONE);
                leaveBtn.setVisibility(View.GONE);
            });
            return;
        }

        eventDB.isEntrantOnWaitlist(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isOnWaitlist) {
                if (isOnWaitlist) {
                    runOnUiThread(() -> {
                        joinBtn.setVisibility(View.GONE);
                        leaveBtn.setVisibility(View.VISIBLE);
                    });
                } else {
                    eventDB.canJoinWaitlist(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean canJoin) {
                            runOnUiThread(() -> {
                                if (canJoin) {
                                    joinBtn.setVisibility(View.VISIBLE);
                                    leaveBtn.setVisibility(View.GONE);
                                } else {
                                    joinBtn.setVisibility(View.GONE);
                                    leaveBtn.setVisibility(View.GONE);
                                }
                            });
                        }

                        @Override
                        public void onError(@NonNull Exception e) {
                            Log.e("EventDetailsActivity", "Failed to check if can join", e);
                            runOnUiThread(() -> {
                                // Double-check organizer status before showing button
                                if (event.getOrganizerId() != null && event.getOrganizerId().equals(deviceId)) {
                                    joinBtn.setVisibility(View.GONE);
                                    leaveBtn.setVisibility(View.GONE);
                                } else {
                                    joinBtn.setVisibility(View.VISIBLE);
                                    leaveBtn.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("EventDetailsActivity", "Failed to check waitlist status", e);
                runOnUiThread(() -> {
                    // Double-check organizer status before showing button
                    if (event.getOrganizerId() != null && event.getOrganizerId().equals(deviceId)) {
                        joinBtn.setVisibility(View.GONE);
                        leaveBtn.setVisibility(View.GONE);
                    } else {
                        joinBtn.setVisibility(View.VISIBLE);
                        leaveBtn.setVisibility(View.GONE);
                    }
                });
            }
        });
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

    /**
     * Loads and displays the event poster image using Glide.
     * Shows placeholder if no poster URL is available or if loading fails.
     *
     * @param imageView the ImageView to display the poster in
     * @param posterUrl the URL of the poster image (can be null)
     */
    private void loadPosterImage(ImageView imageView, String posterUrl) {
        if (posterUrl != null && !posterUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(posterUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.sample_event_banner)
                    .error(R.drawable.sample_event_banner)
                    .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                        @Override
                        public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e,
                                                   Object model, Target<android.graphics.drawable.Drawable> target,
                                                   boolean isFirstResource) {
                            Log.w("EventDetailsActivity", "Failed to load poster image", e);
                            return false; // Let Glide handle error (show placeholder)
                        }

                        @Override
                        public boolean onResourceReady(android.graphics.drawable.Drawable resource,
                                                      Object model, Target<android.graphics.drawable.Drawable> target,
                                                      com.bumptech.glide.load.DataSource dataSource,
                                                      boolean isFirstResource) {
                            return false; // Let Glide handle display
                        }
                    })
                    .into(imageView);
        } else {
            // No poster URL, use placeholder
            imageView.setImageResource(R.drawable.sample_event_banner);
        }
    }

    /**
     * Opens the full-screen image viewer to display the poster.
     *
     * @param imageUrl the URL of the image to display
     */
    private void openFullScreenImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        Intent intent = new Intent(this, FullScreenImageActivity.class);
        intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_URL, imageUrl);
        startActivity(intent);
    }

    /**
     * Displays tags on the event details page.
     */
    private void displayTags() {
        ChipGroup tagGroup = findViewById(R.id.chip_group_tags);
        if (tagGroup == null) {
            return;
        }

        if (event.getTags() == null || event.getTags().isEmpty()) {
            tagGroup.setVisibility(View.GONE);
            return;
        }

        tagGroup.setVisibility(View.VISIBLE);
        tagGroup.removeAllViews();

        for (String tag : event.getTags()) {
            if (tag != null && !tag.trim().isEmpty()) {
                Chip chip = new Chip(this);
                chip.setText(tag);
                chip.setChipBackgroundColorResource(R.color.chip_background);
                chip.setTextColor(getColor(R.color.chip_text));
                chip.setClickable(false);
                chip.setFocusable(false);
                tagGroup.addView(chip);
            }
        }
    }

}
