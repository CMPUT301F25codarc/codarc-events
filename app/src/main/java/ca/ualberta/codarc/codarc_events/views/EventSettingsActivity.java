package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.controllers.UpdatePosterController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.PosterStorage;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Settings page for event organizers.
 * Allows organizers to manage event settings including updating the event poster.
 */
public class EventSettingsActivity extends AppCompatActivity {

    private static final String TAG = "EventSettingsActivity";

    private Event event;
    private EventDB eventDB;
    private PosterStorage posterStorage;
    private UpdatePosterController updatePosterController;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private MaterialButton updatePosterBtn;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_settings);

        event = (Event) getIntent().getSerializableExtra("event");
        if (event == null) {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String deviceId = Identity.getOrCreateDeviceId(this);
        if (event.getOrganizerId() == null || !event.getOrganizerId().equals(deviceId)) {
            Toast.makeText(this, "Only event organizer can access settings", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        eventDB = new EventDB();
        posterStorage = new PosterStorage();
        updatePosterController = new UpdatePosterController(eventDB, posterStorage);

        setupImagePicker();

        updatePosterBtn = findViewById(R.id.btn_update_poster);
        progressBar = findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (updatePosterBtn != null) {
            updatePosterBtn.setOnClickListener(v -> openImagePicker());
        }

        MaterialButton manageWaitlistBtn = findViewById(R.id.btn_manage_waitlist);
        manageWaitlistBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageWaitlistActivity.class);
            intent.putExtra("eventId", event.getId());
            startActivity(intent);
        });

        MaterialButton runLotteryBtn = findViewById(R.id.btn_run_lottery);
        runLotteryBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, DrawActivity.class);
            intent.putExtra("eventId", event.getId());
            intent.putExtra("eventName", event.getName());
            startActivity(intent);
        });

        MaterialButton viewWinnersBtn = findViewById(R.id.btn_view_winners);
        viewWinnersBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewWinnersActivity.class);
            intent.putExtra("eventId", event.getId());
            startActivity(intent);
        });

        MaterialButton viewCancelledBtn = findViewById(R.id.btn_view_cancelled);
        viewCancelledBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewCancelledActivity.class);
            intent.putExtra("eventId", event.getId());
            startActivity(intent);
        });

        MaterialButton viewEnrolledBtn = findViewById(R.id.btn_view_enrolled);
        viewEnrolledBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ViewEnrolledActivity.class);
            intent.putExtra("eventId", event.getId());
            startActivity(intent);
        });

        MaterialButton viewMapBtn = findViewById(R.id.btn_view_map);
        viewMapBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantMapActivity.class);
            intent.putExtra("eventId", event.getId());
            startActivity(intent);
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            updatePoster(imageUri);
                        } else {
                            Toast.makeText(this, "Failed to select image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void updatePoster(Uri imageUri) {
        if (event == null || event.getId() == null) {
            Toast.makeText(this, "Event is invalid", Toast.LENGTH_SHORT).show();
            return;
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (updatePosterBtn != null) {
            updatePosterBtn.setEnabled(false);
        }

        updatePosterController.updatePoster(event, imageUri, new UpdatePosterController.Callback() {
            @Override
            public void onResult(UpdatePosterController.UpdatePosterResult result) {
                runOnUiThread(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    if (updatePosterBtn != null) {
                        updatePosterBtn.setEnabled(true);
                    }

                    if (result.isSuccess()) {
                        event = result.getUpdatedEvent();
                        Toast.makeText(EventSettingsActivity.this,
                                "Poster updated successfully", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Poster updated successfully for event: " + event.getId());
                    } else {
                        String errorMessage = result.getErrorMessage();
                        if (errorMessage == null || errorMessage.isEmpty()) {
                            errorMessage = "Failed to update poster. Please try again.";
                        }
                        Toast.makeText(EventSettingsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Failed to update poster: " + errorMessage);
                    }
                });
            }
        });
    }
}
