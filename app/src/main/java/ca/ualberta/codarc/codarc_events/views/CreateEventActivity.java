package ca.ualberta.codarc.codarc_events.views;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Create Event screen that lets organizers fill event info.
 * Includes date/time pickers and writes to Firestore.
 */
public class CreateEventActivity extends AppCompatActivity {

    private TextInputEditText title, description, eventDateTime,
            regOpen, regClose;
    private EventDB eventDB;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        eventDB = new EventDB();

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        title = findViewById(R.id.et_title);
        description = findViewById(R.id.et_description);
        eventDateTime = findViewById(R.id.et_datetime);
        regOpen = findViewById(R.id.et_reg_open);
        regClose = findViewById(R.id.et_reg_close);

        Button createButton = findViewById(R.id.btn_create_event);
        Button cancelButton = findViewById(R.id.btn_cancel);

        eventDateTime.setOnClickListener(v -> showDateTimePicker(eventDateTime));
        regOpen.setOnClickListener(v -> showDateTimePicker(regOpen));
        regClose.setOnClickListener(v -> showDateTimePicker(regClose));

        cancelButton.setOnClickListener(v -> finish());
        createButton.setOnClickListener(v -> createEvent());
    }

    private void showDateTimePicker(TextInputEditText target) {
        Calendar c = Calendar.getInstance();

        new DatePickerDialog(this, (view, year, month, day) -> {
            new TimePickerDialog(this, (tView, hour, minute) -> {
                Calendar selected = Calendar.getInstance();
                selected.set(year, month, day, hour, minute, 0);

                String localDisplay = String.format(
                        "%04d-%02d-%02d %02d:%02d %s",
                        year, month + 1, day,
                        (hour % 12 == 0 ? 12 : hour % 12),
                        minute, (hour >= 12 ? "PM" : "AM"));

                SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                String lIso = iso.format(selected.getTime());

                target.setText(localDisplay);
                target.setTag(lIso);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }


    private String get(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void createEvent() {
        String name = get(title);
        String desc = get(description);
        String dateTime = get(eventDateTime);
        String open = get(regOpen);
        String close = get(regClose);

        if (name.isEmpty() || dateTime.isEmpty() || open.isEmpty() || close.isEmpty()) {
            Toast.makeText(this, "Fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = UUID.randomUUID().toString();
        String organizerId = Identity.getOrCreateDeviceId(this);

        Event event = new Event();
        event.setId(id);
        event.setName(name);
        event.setDescription(desc);
        event.setEventDateTime(dateTime);
        event.setRegistrationOpen(open);
        event.setRegistrationClose(close);
        event.setOrganizerId(organizerId);
        event.setOpen(true);

        progressBar.setVisibility(View.VISIBLE);

        eventDB.addEvent(event, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Toast.makeText(CreateEventActivity.this, "Event created", Toast.LENGTH_SHORT).show();
                // TODO: Store in firebase later
                generateEventQr(id);
                progressBar.setVisibility(View.GONE);
                finish();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(CreateEventActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // Logic here for later when we generate QR code and put it in Firebase
    private void generateEventQr(String eventId) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            String qrData = "event:" + eventId;
            Bitmap bitmap = encoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 400, 400);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to generate QR", Toast.LENGTH_SHORT).show();
        }
    }
}