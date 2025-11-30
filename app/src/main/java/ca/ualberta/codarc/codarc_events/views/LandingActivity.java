package ca.ualberta.codarc.codarc_events.views;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import ca.ualberta.codarc.codarc_events.utils.NotificationChannelHelper;
import ca.ualberta.codarc.codarc_events.data.UserDB;

/**
 * Launcher activity that verifies identity and routes to the event browser.
 */
public class LandingActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        NotificationChannelHelper.createChannel(this);
        requestNotificationPermission();

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

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            // Permission result is handled gracefully - notifications will still work in background
            // even if permission is denied
        }
    }
}

