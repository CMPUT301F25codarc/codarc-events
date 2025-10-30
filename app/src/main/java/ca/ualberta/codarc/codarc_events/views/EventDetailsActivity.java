package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import com.google.android.material.button.MaterialButton;

/**
 * Event details screen. For Stage 1 this mostly hosts the Join button which
 * checks if a profile is registered. The actual join/leave logic will be added
 * as we progress the story.
 */
public class EventDetailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);
        MaterialButton joinBtn = findViewById(R.id.btn_join_waitlist);

        joinBtn.setOnClickListener(v -> {
            String deviceId = Identity.getOrCreateDeviceId(this);
            EntrantDB entrantDB = new EntrantDB();
            entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
                @Override
                public void onSuccess(Entrant entrant) {
                    boolean isRegistered = entrant != null && entrant.getIsRegistered();
                    if (!isRegistered) {
                        Intent intent = new Intent(EventDetailsActivity.this, ProfileCreationActivity.class);
                        startActivity(intent);
                    } else {
                        // Stage 1 join flow will be implemented later; for now, show a placeholder
                        Toast.makeText(EventDetailsActivity.this, "Ready to join (registered)", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(@androidx.annotation.NonNull Exception e) {
                    Toast.makeText(EventDetailsActivity.this, "Failed to check profile", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}


