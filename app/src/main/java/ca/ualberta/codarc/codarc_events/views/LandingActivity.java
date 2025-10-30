package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;

/**
 * Launcher activity that verifies identity and routes to the event browser.
 * Identity setup is a quick Firestore write ensuring the profile document
 * exists for this device.
 */
public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        // Stage 0: device identification
        String deviceId = Identity.getOrCreateDeviceId(this);
        EntrantDB entrantDB = new EntrantDB();
        entrantDB.ensureProfileDefaults(deviceId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                // Optional: brief confirmation toast per user story
                // Toast.makeText(LandingActivity.this, "Identity verified", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                // Keep minimal; show a simple toast
                Toast.makeText(LandingActivity.this, "Identity setup failed", Toast.LENGTH_SHORT).show();
            }
        });

        MaterialButton continueBtn = findViewById(R.id.btn_continue);
        continueBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventBrowserActivity.class);
            startActivity(intent);
        });

    }
}


