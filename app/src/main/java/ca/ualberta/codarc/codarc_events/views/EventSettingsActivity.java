package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Settings screen for event organizer.
 * Access to manage waitlist and other event settings.
 */
public class EventSettingsActivity extends AppCompatActivity {

    private Event event;
    private String deviceId;

    /**
     * Sets up settings screen and verifies organizer permission.
     *
     * @param savedInstanceState saved state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_settings);

        // Get event from Intent
        event = (Event) getIntent().getSerializableExtra("event");
        if (event == null) {
            Log.e("EventSettingsActivity", "Event not found in Intent");
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        deviceId = Identity.getOrCreateDeviceId(this);

        // Verify organizer permission
        if (event.getOrganizerId() == null || !event.getOrganizerId().equals(deviceId)) {
            Log.w("EventSettingsActivity", "Unauthorized access attempt");
            Toast.makeText(this, "You are not authorized to access these settings", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
    }

    private void setupUI() {
        MaterialButton manageWaitlistBtn = findViewById(R.id.btn_manage_waitlist);
        manageWaitlistBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageWaitlistActivity.class);
            intent.putExtra("eventId", event.getId());
            startActivity(intent);
        });
    }
}

