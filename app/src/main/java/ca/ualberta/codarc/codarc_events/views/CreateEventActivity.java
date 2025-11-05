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

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.controllers.CreateEventController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.OrganizerDB;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Create Event screen that lets organizers fill event info.
 * Includes date/time pickers and writes to Firestore.
 * 
 * In the refactored structure:
 * - Creates event in Events collection
 * - Creates Organizer document (if first event)
 * - Sets isOrganizer = true in Users collection (if first event)
 * - Adds event to Organizer's events subcollection
 */
public class CreateEventActivity extends AppCompatActivity {

    private TextInputEditText title, description, eventDateTime,
            regOpen, regClose, location, capacity;
    private EventDB eventDB;
    private OrganizerDB organizerDB;
    private UserDB userDB;
    private CreateEventController controller;
    private ProgressBar progressBar;
    private String organizerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        eventDB = new EventDB();
        organizerDB = new OrganizerDB();
        userDB = new UserDB();
        organizerId = Identity.getOrCreateDeviceId(this);
        controller = new CreateEventController(eventDB, organizerId);

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        title = findViewById(R.id.et_title);
        description = findViewById(R.id.et_description);
        eventDateTime = findViewById(R.id.et_datetime);
        location = findViewById(R.id.et_location);
        regOpen = findViewById(R.id.et_reg_open);
        regClose = findViewById(R.id.et_reg_close);
        capacity = findViewById(R.id.et_capacity);

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
     * Uses CreateEventController to handle business logic.
     */
    private void createEvent() {
        String name = get(title);
        String desc = get(description);
        String dateTime = getDateValue(eventDateTime);
        String loc = get(location);
        String open = getDateValue(regOpen);
        String close = getDateValue(regClose);
        String capacityStr = get(capacity);

        // Use controller to validate and create event
        CreateEventController.CreateEventResult result = controller.validateAndCreateEvent(
                name, desc, dateTime, loc, open, close, capacityStr
        );

        if (!result.isValid()) {
            Toast.makeText(this, result.getErrorMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Persist event using controller
        Event event = result.getEvent();
        controller.persistEvent(event, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                // Event created successfully, now handle organizer setup
                handleOrganizerSetup(event);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("CreateEventActivity", "Failed to create event", e);
                Toast.makeText(CreateEventActivity.this, "Failed to create event. Please try again.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    
    /**
     * Handles organizer setup after event creation.
     * Checks if organizer document exists, creates it if needed,
     * sets isOrganizer flag in Users, and adds event to organizer's events.
     */
    private void handleOrganizerSetup(Event event) {
        organizerDB.organizerExists(organizerId, new OrganizerDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean exists) {
                if (!exists) {
                    // First event - create Organizer document and set role
                    createNewOrganizer(event);
                } else {
                    // Already an organizer - just add event to their list
                    addEventToOrganizer(event);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                // If check fails, try to create anyway (safer)
                createNewOrganizer(event);
            }
        });
    }
    
    /**
     * Creates a new Organizer document and sets isOrganizer flag in Users.
     * Then adds the event to the organizer's events subcollection.
     */
    private void createNewOrganizer(Event event) {
        organizerDB.createOrganizer(organizerId, new OrganizerDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                // Set isOrganizer = true in Users collection
                userDB.setOrganizerRole(organizerId, true, new UserDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        // Now add event to organizer's events
                        addEventToOrganizer(event);
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        // Organizer created but role flag failed - not critical
                        addEventToOrganizer(event);
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                // Organizer creation failed - still show success for event creation
                Log.e("CreateEventActivity", "Failed to create organizer document", e);
                Toast.makeText(CreateEventActivity.this, "Event created", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                finish();
            }
        });
    }
    
    /**
     * Adds the event to the organizer's events subcollection.
     */
    private void addEventToOrganizer(Event event) {
        organizerDB.addEventToOrganizer(organizerId, event.getId(), new OrganizerDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Toast.makeText(CreateEventActivity.this, "Event created successfully", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                finish();
            }

            @Override
            public void onError(@NonNull Exception e) {
                // Event created but couldn't add to organizer's list - still show success
                Log.e("CreateEventActivity", "Failed to add event to organizer's list", e);
                Toast.makeText(CreateEventActivity.this, "Event created", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                finish();
            }
        });
    }
}
