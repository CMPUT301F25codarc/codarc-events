package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import ca.ualberta.codarc.codarc_events.R;

/**
 * QR scanning. Registered and visible to keep navigation paths
 * intact; camera integration will arrive with the organizer QR story.
 */
public class QRScannerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
    }
}


