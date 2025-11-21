package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.res.ColorStateList;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.EventCardAdapter;
import ca.ualberta.codarc.codarc_events.controllers.FilterEventsController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.TagDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import ca.ualberta.codarc.codarc_events.utils.TagHelper;

/**
 * Displays the list of available events for entrants.
 *
 * <p>This activity:
 * <ul>
 *   <li>Initializes and subscribes to Firestore via {@link EventDB#getAllEvents(EventDB.Callback)}.</li>
 *   <li>Displays all events in a RecyclerView using {@link EventCardAdapter}.</li>
 *   <li>Allows navigation to the profile screen (via iv_profile).</li>
 *   <li>Allows organizers to create a new event (via btn_plus).</li>
 * </ul></p>
 */
public class EventBrowserActivity extends AppCompatActivity {

    private RecyclerView rvEvents;
    private final List<Event> eventList = new ArrayList<>();
    private final List<Event> allEvents = new ArrayList<>();
    private EventCardAdapter adapter;
    private EventDB eventDB;
    private FilterEventsController filterController;
    private FilterEventsController.FilterCriteria currentFilterCriteria;
    private ImageView filterIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_browser);

        // --- Ensure device has a unique ID (used as entrant identifier)
        String deviceId = Identity.getOrCreateDeviceId(this);
        new EntrantDB().getOrCreateEntrant(deviceId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) { }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("EventBrowserActivity", "Failed to ensure entrant profile", e);
            }
        });

        // --- RecyclerView setup for events
        rvEvents = findViewById(R.id.rv_events);
        if (rvEvents == null) {
            android.util.Log.e("EventBrowserActivity", "RecyclerView not found in layout");
            finish();
            return;
        }
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventCardAdapter(this, eventList);
        rvEvents.setAdapter(adapter);

        eventDB = new EventDB();
        filterController = new FilterEventsController();
        currentFilterCriteria = new FilterEventsController.FilterCriteria(null, false);
        loadEvents();

        // Filter icon click handler
        filterIcon = findViewById(R.id.iv_filter);
        if (filterIcon != null) {
            filterIcon.setOnClickListener(v -> showFilterDialog());
        }

        // --- "+" icon: opens CreateEventActivity for organizers
        ImageView plusIcon = findViewById(R.id.btn_plus);
        if (plusIcon != null) {
            plusIcon.setOnClickListener(v -> {
                Intent intent = new Intent(EventBrowserActivity.this, CreateEventActivity.class);
                startActivity(intent);
            });
        }

        // Profile icon: opens ProfileCreationActivity for profile management
        ImageView profileIcon = findViewById(R.id.iv_profile_settings);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> {
                Intent intent = new Intent(EventBrowserActivity.this, ProfileCreationActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }

        View notificationsTab = findViewById(R.id.tab_notifications);
        if (notificationsTab != null) {
            notificationsTab.setOnClickListener(v -> {
                Intent intent = new Intent(EventBrowserActivity.this, NotificationsActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh events when returning to this activity
        // This ensures waitlist counts are updated if user joined from EventDetailsActivity
        loadEvents();
    }

    /**
     * Loads all events from Firestore and updates the adapter list.
     */
    private void loadEvents() {
        eventDB.getAllEvents(new EventDB.Callback<List<Event>>() {
            @Override
            public void onSuccess(List<Event> value) {
                if (value != null) {
                    allEvents.clear();
                    allEvents.addAll(value);
                    applyCurrentFilters();
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("EventBrowserActivity", "Failed to load events", e);
            }
        });
    }

    /**
     * Shows the filter dialog for selecting tags and availability filter.
     */
    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filter_events, null);
        ChipGroup tagChipGroup = dialogView.findViewById(R.id.chip_group_filter_tags);
        MaterialCheckBox availableCheckbox = dialogView.findViewById(R.id.cb_available_only);
        MaterialButton applyButton = dialogView.findViewById(R.id.btn_apply_filters);
        MaterialButton clearButton = dialogView.findViewById(R.id.btn_clear_filters);

        // Initialize selected tags set
        Set<String> selectedTags = new HashSet<>();
        if (currentFilterCriteria != null && currentFilterCriteria.hasTagFilter()) {
            selectedTags.addAll(currentFilterCriteria.getSelectedTags());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Get all tags from tags collection (more efficient than iterating events)
        TagDB tagDB = new TagDB();
        tagDB.getAllTags(new TagDB.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> allTagsList) {
                // Sort tags
                List<String> sortedTags = new ArrayList<>(allTagsList);
                sortedTags.sort(String::compareToIgnoreCase);
                
                // Create chips for all available tags
                createTagChips(tagChipGroup, sortedTags, selectedTags);
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("EventBrowserActivity", "Failed to load tags, falling back to event iteration", e);
                // Fallback to old method if tags collection fails
                Set<String> allTags = TagHelper.collectAllUniqueTags(allEvents);
                List<String> sortedTags = new ArrayList<>(allTags);
                sortedTags.sort(String::compareToIgnoreCase);
                createTagChips(tagChipGroup, sortedTags, selectedTags);
            }
        });

        // Set availability checkbox state
        if (currentFilterCriteria != null) {
            availableCheckbox.setChecked(currentFilterCriteria.isAvailableOnly());
        }

        applyButton.setOnClickListener(v -> {
            List<String> selectedTagsList = new ArrayList<>(selectedTags);
            boolean availableOnly = availableCheckbox.isChecked();
            currentFilterCriteria = new FilterEventsController.FilterCriteria(selectedTagsList, availableOnly);
            applyCurrentFilters();
            updateFilterIcon();
            dialog.dismiss();
        });

        clearButton.setOnClickListener(v -> {
            currentFilterCriteria = new FilterEventsController.FilterCriteria(null, false);
            applyCurrentFilters();
            updateFilterIcon();
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Applies the current filter criteria to the event list.
     */
    private void applyCurrentFilters() {
        if (currentFilterCriteria == null || currentFilterCriteria.isEmpty()) {
            eventList.clear();
            eventList.addAll(allEvents);
            adapter.notifyDataSetChanged();
            return;
        }

        filterController.applyFiltersAsync(allEvents, currentFilterCriteria, eventDB,
                new FilterEventsController.Callback() {
                    @Override
                    public void onResult(FilterEventsController.FilterResult result) {
                        if (result.isSuccess()) {
                            eventList.clear();
                            eventList.addAll(result.getFilteredEvents());
                            adapter.notifyDataSetChanged();

                            if (eventList.isEmpty()) {
                                showEmptyState("No events match your filters");
                            } else {
                                hideEmptyState();
                            }
                        } else {
                            Toast.makeText(EventBrowserActivity.this,
                                    result.getErrorMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Updates the filter icon appearance based on whether filters are active.
     */
    private void updateFilterIcon() {
        if (filterIcon == null) {
            return;
        }

        boolean hasActiveFilters = currentFilterCriteria != null && !currentFilterCriteria.isEmpty();
        if (hasActiveFilters) {
            filterIcon.setAlpha(1.0f);
            filterIcon.setColorFilter(getColor(R.color.entrant_action_button_purple));
        } else {
            filterIcon.setAlpha(0.6f);
            filterIcon.setColorFilter(getColor(R.color.entrant_action_button_purple));
        }
    }

    /**
     * Shows an empty state message when no events match filters.
     *
     * @param message the message to display
     */
    private void showEmptyState(String message) {
        // For now, just show a toast. Could be enhanced with a proper empty state view.
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Hides the empty state message.
     */
    private void hideEmptyState() {
        // Empty state is handled via toast for now
    }

    /**
     * Creates tag chips in the filter dialog.
     *
     * @param tagChipGroup the ChipGroup to add chips to
     * @param sortedTags list of sorted tag strings
     * @param selectedTags set of currently selected tags
     */
    private void createTagChips(ChipGroup tagChipGroup, List<String> sortedTags, 
                                 Set<String> selectedTags) {
        tagChipGroup.removeAllViews();
        
        for (String tag : sortedTags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            boolean isSelected = selectedTags.contains(tag);
            chip.setChecked(isSelected);
            
            // Update chip appearance based on selection state
            updateChipAppearance(chip, isSelected);
            
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedTags.add(tag);
                } else {
                    selectedTags.remove(tag);
                }
                // Update appearance when checked state changes
                updateChipAppearance(chip, isChecked);
            });
            tagChipGroup.addView(chip);
        }
    }

    /**
     * Updates the chip appearance based on selection state.
     * Selected chips have a colored background, unselected chips have white background.
     *
     * @param chip the chip to update
     * @param isSelected true if the chip is selected, false otherwise
     */
    private void updateChipAppearance(Chip chip, boolean isSelected) {
        if (isSelected) {
            chip.setChipBackgroundColorResource(R.color.chip_background_selected);
            chip.setTextColor(getColor(R.color.chip_text_selected));
            chip.setChipStrokeWidth(0);
        } else {
            chip.setChipBackgroundColorResource(R.color.chip_background_unselected);
            chip.setTextColor(getColor(R.color.chip_text_unselected));
            chip.setChipStrokeWidth(1);
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.divider)));
        }
    }
}


