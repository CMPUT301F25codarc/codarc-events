package ca.ualberta.codarc.codarc_events.views;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        eventDB = new EventDB();

        title = findViewById(R.id.et_title);
        description = findViewById(R.id.et_description);
        eventDateTime = findViewById(R.id.et_datetime);
        regOpen = findViewById(R.id.et_reg_open);
        regClose = findViewById(R.id.et_reg_close);

        Button createButton = findViewById(R.id.btn_create_event);
        Button cancelButton = findViewById(R.id.btn_cancel);

        eventDateTime.setOnClickListener(v -> showDateTimePicker(eventDateTime));
        regOpen.setOnClickListener(v -> showDatePicker(regOpen));
        regClose.setOnClickListener(v -> showDatePicker(regClose));

        cancelButton.setOnClickListener(v -> finish());
        createButton.setOnClickListener(v -> createEvent());
    }

    private void showDateTimePicker(TextInputEditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format("%04d-%02d-%02d", year, month + 1, day);
            new TimePickerDialog(this, (tView, hour, minute) -> {
                String amPm = hour >= 12 ? "PM" : "AM";
                int displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
                String time = String.format("%02d:%02d %s", displayHour, minute, amPm);
                target.setText(date + " " + time);
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                target.setText(String.format("%04d-%02d-%02d", year, month + 1, day)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(TextInputEditText target) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            String amPm = hour >= 12 ? "PM" : "AM";
            int displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
            target.setText(String.format("%02d:%02d %s", displayHour, minute, amPm));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
    }

    private String get(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void createEvent() {
        String name = get(title);
        String desc = get(description);
        String dateTime = get(eventDateTime); // combined date + time picker result
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
        event.setEventDateTime(dateTime);     // ← single field instead of setEventDate + setEventTime
        event.setRegistrationOpen(open);
        event.setRegistrationClose(close);
        event.setOpen(true);

        eventDB.addEvent(event, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Toast.makeText(CreateEventActivity.this, "Event created", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(CreateEventActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}