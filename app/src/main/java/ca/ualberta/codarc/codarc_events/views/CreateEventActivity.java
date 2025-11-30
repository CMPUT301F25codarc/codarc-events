package ca.ualberta.codarc.codarc_events.views;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.controllers.CreateEventController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.OrganizerDB;
import ca.ualberta.codarc.codarc_events.data.PosterStorage;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import ca.ualberta.codarc.codarc_events.utils.TagHelper;

/**
 * Create Event screen that lets organizers fill event info.
 * 
 * Note: The complex async orchestration for poster upload during event creation (create event → 
 * upload poster → update event with poster URL) was implemented with assistance from Claude Sonnet 4.5 (Anthropic).
 * The sophisticated error handling, state management, and multi-step async coordination were developed with LLM assistance.
 */
public class CreateEventActivity extends AppCompatActivity {

    private TextInputEditText title, description, eventDateTime,
            regOpen, regClose, location, capacity;
    private TextInputLayout tagInputLayout;
    private AppCompatAutoCompleteTextView tagInput;
    private ChipGroup tagChipGroup;
    private List<String> selectedTags;
    private EventDB eventDB;
    private OrganizerDB organizerDB;
    private UserDB userDB;
    private CreateEventController controller;
    private ProgressBar progressBar;
    private String organizerId;
    private ImageView ivPoster;
    private Button btnChoosePoster;
    private Uri selectedImageUri;
    private PosterStorage posterStorage;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        eventDB = new EventDB();
        organizerDB = new OrganizerDB();
        userDB = new UserDB();
        posterStorage = new PosterStorage();
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

        ivPoster = findViewById(R.id.iv_poster);
        btnChoosePoster = findViewById(R.id.btn_choose_poster);
        selectedImageUri = null;
        setupImagePicker();

        selectedTags = new ArrayList<>();
        tagInputLayout = findViewById(R.id.til_tag_input);
        tagInput = findViewById(R.id.et_tag_input);
        tagChipGroup = findViewById(R.id.chip_group_tags);
        setupTagInput();

        Button createButton = findViewById(R.id.btn_create_event);
        Button cancelButton = findViewById(R.id.btn_cancel);

        eventDateTime.setOnClickListener(v -> showDateTimePicker(eventDateTime));
        regOpen.setOnClickListener(v -> showDateTimePicker(regOpen));
        regClose.setOnClickListener(v -> showDateTimePicker(regClose));
        btnChoosePoster.setOnClickListener(v -> openImagePicker());

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
     *
     * @param input the TextInputEditText to read from
     * @return the ISO formatted date string if available, otherwise the displayed text
     */
    private String getDateValue(TextInputEditText input) {
        String tagValue = (String) input.getTag();
        if (tagValue != null && !tagValue.isEmpty()) {
            return tagValue;
        }
        return get(input);
    }
    private String get(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            selectedImageUri = imageUri;
                            ivPoster.setImageURI(imageUri);
                        }
                    }
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void createEvent() {
        checkBannedStatus();
    }

    private void checkBannedStatus() {
        organizerDB.isBanned(organizerId, new OrganizerDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean banned) {
                if (banned != null && banned) {
                    showBannedDialog();
                    return;
                }
                proceedWithEventCreation();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("CreateEventActivity", "Failed to check banned status", e);
                proceedWithEventCreation();
            }
        });
    }

    private void showBannedDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Account Banned")
                .setMessage("Your account has been banned. You cannot create events. Please contact an administrator if you believe this is an error.")
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void proceedWithEventCreation() {
        String name = get(title);
        String desc = get(description);
        String dateTime = getDateValue(eventDateTime);
        String loc = get(location);
        String open = getDateValue(regOpen);
        String close = getDateValue(regClose);
        String capacityStr = get(capacity);

        CreateEventController.CreateEventResult result = controller.validateAndCreateEvent(
                name, desc, dateTime, loc, open, close, capacityStr, selectedTags, null
        );

        if (!result.isValid()) {
            Toast.makeText(this, result.getErrorMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Event event = result.getEvent();
        createEventFirst(event);
    }

    private void createEventFirst(Event event) {
        controller.persistEvent(event, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                if (selectedImageUri != null) {
                    uploadPosterAndUpdateEvent(event);
                } else {
                    handleOrganizerSetup(event);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("CreateEventActivity", "Failed to create event", e);
                runOnUiThread(() -> {
                Toast.makeText(CreateEventActivity.this, "Failed to create event. Please try again.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void uploadPosterAndUpdateEvent(Event event) {
        posterStorage.uploadPoster(event.getId(), selectedImageUri, new PosterStorage.Callback<String>() {
            @Override
            public void onSuccess(String posterUrl) {
                event.setPosterUrl(posterUrl);
                updateEventWithPosterUrl(event, posterUrl);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("CreateEventActivity", "Failed to upload poster", e);
                final String errorMessage = (e.getMessage() != null && !e.getMessage().isEmpty())
                        ? e.getMessage()
                        : "Failed to upload poster. Please try again.";
                runOnUiThread(() -> {
                    Toast.makeText(CreateEventActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    handleOrganizerSetup(event);
                });
            }
        });
    }

    private void updateEventWithPosterUrl(Event event, String posterUrl) {
        event.setPosterUrl(posterUrl);

        controller.persistEvent(event, new EventDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                handleOrganizerSetup(event);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("CreateEventActivity", "Failed to update event with poster URL", e);
                runOnUiThread(() -> {
                    handleOrganizerSetup(event);
                });
            }
        });
    }
    
    private void handleOrganizerSetup(Event event) {
        organizerDB.organizerExists(organizerId, new OrganizerDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean exists) {
                if (!exists) {
                    createNewOrganizer(event);
                } else {
                    addEventToOrganizer(event);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                createNewOrganizer(event);
            }
        });
    }
    
    private void createNewOrganizer(Event event) {
        organizerDB.createOrganizer(organizerId, new OrganizerDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                userDB.setOrganizerRole(organizerId, true, new UserDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        addEventToOrganizer(event);
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        addEventToOrganizer(event);
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("CreateEventActivity", "Failed to create organizer document", e);
                Toast.makeText(CreateEventActivity.this, "Event created", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                finish();
            }
        });
    }
    
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
                Log.e("CreateEventActivity", "Failed to add event to organizer's list", e);
                Toast.makeText(CreateEventActivity.this, "Event created", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                finish();
            }
        });
    }

    /**
     * Sets up the tag input field with autocomplete functionality.
     * Supports both predefined tags (via autocomplete) and custom tags (by typing and pressing Enter).
     */
    private void setupTagInput() {
        List<String> predefinedTags = TagHelper.getPredefinedTags();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, predefinedTags);
        
        tagInput.setAdapter(adapter);
        tagInput.setThreshold(1);

        tagInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                addTagFromInput();
                return true;
            }
            return false;
        });

        tagInput.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < parent.getCount()) {
                String selectedTag = (String) parent.getItemAtPosition(position);
                if (selectedTag != null) {
                    addTag(selectedTag);
                    tagInput.setText("");
                }
            }
        });

        tagInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString();
                if (query.isEmpty()) {
                    adapter.getFilter().filter(null);
                } else {
                    List<String> matches = TagHelper.filterMatchingTags(query, predefinedTags);
                    ArrayAdapter<String> filteredAdapter = new ArrayAdapter<>(CreateEventActivity.this,
                            android.R.layout.simple_dropdown_item_1line, matches);
                    tagInput.setAdapter(filteredAdapter);
                }
            }
        });
    }

    /**
     * Adds a tag from the input field if it's not empty.
     */
    private void addTagFromInput() {
        String tagText = tagInput.getText() != null ? tagInput.getText().toString().trim() : "";
        if (!tagText.isEmpty()) {
            addTag(tagText);
            tagInput.setText("");
        }
    }

    /**
     * Adds a tag to the selected tags list and updates the UI.
     *
     * @param tag the tag string to add
     */
    private void addTag(String tag) {
        if (!controller.canAddTag(tag, selectedTags)) {
            Toast.makeText(this, "Tag already added", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedTags.add(tag);
        updateTagChips();
    }

    /**
     * Removes a tag from the selected tags list.
     *
     * @param tag the tag string to remove
     */
    private void removeTag(String tag) {
        selectedTags.remove(tag);
        updateTagChips();
    }

    /**
     * Updates the chip group to display all selected tags.
     */
    private void updateTagChips() {
        tagChipGroup.removeAllViews();

        for (String tag : selectedTags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> removeTag(tag));
            chip.setChipBackgroundColorResource(R.color.chip_background);
            chip.setTextColor(getColor(R.color.chip_text));
            tagChipGroup.addView(chip);
        }
    }
}
