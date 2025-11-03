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

        MaterialButton manageBtn = findViewById(R.id.btn_manage_entrants);
        manageBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageEntrantsActivity.class);
            intent.putExtra("eventId", event.getId());
            startActivity(intent);
        });
    }
}
