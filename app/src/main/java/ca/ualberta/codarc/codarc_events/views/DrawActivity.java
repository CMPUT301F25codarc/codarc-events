package ca.ualberta.codarc.codarc_events.views;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.controllers.DrawController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Organizer screen to run the lottery draw.
 */
public class DrawActivity extends AppCompatActivity {

    private EditText etNumWinners;
    private TextView tvResultSummary, tvEntrantCount, tvEventName;
    private MaterialButton btnRunDraw;
    private ImageView ivBack;

    private DrawController drawController;
    private String eventId, eventName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);

        drawController = new DrawController(new EventDB());
        eventId = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");

        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        verifyOrganizerAccess();

        etNumWinners = findViewById(R.id.et_num_winners);
        tvResultSummary = findViewById(R.id.tv_result_summary);
        tvEntrantCount = findViewById(R.id.tv_total_registrants);
        tvEventName = findViewById(R.id.tv_event_name);
        btnRunDraw = findViewById(R.id.btn_run_draw);
        ivBack = findViewById(R.id.iv_back);

        tvEventName.setText(eventName != null ? eventName : "Run Lottery Draw");

        loadEntrantCount();

        btnRunDraw.setOnClickListener(v -> showConfirmDialog());

        ivBack.setOnClickListener(v -> finish());
    }

    private void showConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Draw")
                .setMessage("Press confirm to run lottery draw.")
                .setPositiveButton("Confirm", (d, w) -> runDraw())
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
}

    private void loadEntrantCount() {
        drawController.loadEntrantCount(eventId, new DrawController.CountCallback() {
            @Override
            public void onSuccess(int count) {
                runOnUiThread(() -> tvEntrantCount.setText("Total Entrants: " + count));
            }

            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(DrawActivity.this,
                                "Error loading entrants: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void runDraw() {
        String input = etNumWinners.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Enter number of winners", Toast.LENGTH_SHORT).show();
            return;
        }

        int numWinners;
        try {
            numWinners = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
            return;
        }

        drawController.runDraw(eventId, numWinners, new DrawController.DrawCallback() {
            @Override
            public void onSuccess(List<String> winners, List<String> replacements) {
                runOnUiThread(() -> {
                    String summary = String.format("✅ Winners drawn: %d\n📋 Replacement pool: %d\n📧 Notifications sent automatically", 
                            winners.size(), replacements.size());
                    tvResultSummary.setText(summary);
                    Toast.makeText(DrawActivity.this, 
                            "Lottery complete! " + winners.size() + " winners selected. Notifications sent automatically.", 
                            Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(DrawActivity.this,
                                "Error running draw: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void verifyOrganizerAccess() {
        EventDB eventDB = new EventDB();
        String deviceId = Identity.getOrCreateDeviceId(this);
        
        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null || event.getOrganizerId() == null || !event.getOrganizerId().equals(deviceId)) {
                    runOnUiThread(() -> {
                        Toast.makeText(DrawActivity.this, "Only event organizer can access this", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(DrawActivity.this, "Failed to verify access", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
}

