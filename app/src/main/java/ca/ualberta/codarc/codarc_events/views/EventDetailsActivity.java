package ca.ualberta.codarc.codarc_events.views;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
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

        // UI references
        TextView title = findViewById(R.id.event_title);
        TextView desc = findViewById(R.id.event_desc);
        TextView dateTime = findViewById(R.id.event_datetime);
        TextView regWindow = findViewById(R.id.event_reg_window);
        ImageView qrImage = findViewById(R.id.event_qr);
        MaterialButton joinBtn = findViewById(R.id.btn_join_waitlist);

        Event event = (Event) getIntent().getSerializableExtra("event");
        if (event != null) {
            title.setText(event.getName());
            desc.setText(event.getDescription());
            dateTime.setText(event.getEventDateTime());
            regWindow.setText("Registration: " + event.getRegistrationOpen() + " → " + event.getRegistrationClose());

            try {
                String qrData = event.getQrCode();
                BarcodeEncoder encoder = new BarcodeEncoder();
                Bitmap qrBitmap = encoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 600, 600);
                qrImage.setImageBitmap(qrBitmap);
            } catch (Exception e) {
                Toast.makeText(this, "QR error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }

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
                        Toast.makeText(EventDetailsActivity.this, "Ready to join (registered)", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(@NonNull Exception e) {
                    Toast.makeText(EventDetailsActivity.this, "Failed to check profile", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
