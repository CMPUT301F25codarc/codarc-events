package ca.ualberta.codarc.codarc_events.views;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

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

    /**
     * Shows a date picker followed by a time picker.
     * Formats the selected date/time for display and stores ISO format in the tag.
     *
     * @param target the TextInputEditText to populate with the selected date/time
     */
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

    /**
     * Retrieves date value from TextInputEditText, preferring ISO format stored in tag.
     * Falls back to displayed text if tag is not available.
     *
     * @param input the TextInputEditText to read from
     * @return the ISO formatted date string if available, otherwise the displayed text
     */
    private String getDateValue(TextInputEditText input) {
        String tagValue = (String) input.getTag();
        if (tagValue != null && !tagValue.isEmpty()) {
            return tagValue;  // use ISO format if present
        }
        return get(input);  // fallback to displayed text
    }
    private String get(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /**
     * Validates form inputs and creates a new event in Firestore.
     * Generates a unique ID, associates it with the current organizer,
     * and creates QR code data. Shows progress bar during async operation.
     */
    private void createEvent() {
        String name = get(title);
        String desc = get(description);
        String dateTime = getDateValue(eventDateTime);
        String open = getDateValue(regOpen);
        String close = getDateValue(regClose);

        if (name.isEmpty() || dateTime.isEmpty() || open.isEmpty() || close.isEmpty()) {
            Toast.makeText(this, "Fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = UUID.randomUUID().toString();
        String organizerId = Identity.getOrCreateDeviceId(this);
        String qrData = "event:" + id; // store QR data as a string

        Event event = new Event();
        event.setId(id);
        event.setName(name);
        event.setDescription(desc);
        event.setEventDateTime(dateTime);
        event.setRegistrationOpen(open);
        event.setRegistrationClose(close);
        event.setOrganizerId(organizerId);
        event.setQrCode(qrData);
        event.setOpen(true);

        progressBar.setVisibility(View.VISIBLE);

        eventDB.addEvent(event, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Toast.makeText(CreateEventActivity.this, "Event created", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                finish();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("CreateEventActivity", "Failed to create event", e);
                Toast.makeText(CreateEventActivity.this, "Failed to create event. Please try again.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}
