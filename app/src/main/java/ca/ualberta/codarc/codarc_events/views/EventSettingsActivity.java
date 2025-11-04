package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Settings page for event organizers.
 */
public class EventSettingsActivity extends AppCompatActivity {

    private Event event;

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
    }
}
