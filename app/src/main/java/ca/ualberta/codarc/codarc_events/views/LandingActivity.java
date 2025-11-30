package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import ca.ualberta.codarc.codarc_events.data.UserDB;

/**
 * Launcher activity that verifies identity and routes to the event browser.
 * 
 * In the refactored structure:
 * - Creates a User document in the users collection (base identity)
 * - User document has role flags (all false by default)
 * - Entrants and Organizers documents are created later when user performs actions
 */
public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        String deviceId = Identity.getOrCreateDeviceId(this);
        UserDB userDB = new UserDB();
        userDB.ensureUserExists(deviceId, new UserDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
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


