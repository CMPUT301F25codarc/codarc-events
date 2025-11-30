package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.User;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import ca.ualberta.codarc.codarc_events.utils.TagHelper;

/**
 * Displays the list of available events for entrants.
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

        String deviceId = Identity.getOrCreateDeviceId(this);
        new EntrantDB().getOrCreateEntrant(deviceId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) { }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("EventBrowserActivity", "Failed to ensure entrant profile", e);
            }
        });

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

        filterIcon = findViewById(R.id.iv_filter);
        if (filterIcon != null) {
            filterIcon.setOnClickListener(v -> showFilterDialog());
        }

        ImageView plusIcon = findViewById(R.id.btn_plus);
        if (plusIcon != null) {
            plusIcon.setOnClickListener(v -> {
                Intent intent = new Intent(EventBrowserActivity.this, CreateEventActivity.class);
                startActivity(intent);
            });
        }

        ImageView profileIcon = findViewById(R.id.iv_profile_settings);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> {
                Intent intent = new Intent(EventBrowserActivity.this, ProfileCreationActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }

        View historyTab = findViewById(R.id.tab_history);
        if (historyTab != null) {
            historyTab.setOnClickListener(v -> {
                Intent intent = new Intent(EventBrowserActivity.this, RegistrationHistoryActivity.class);
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

        View scanQRTab = findViewById(R.id.tab_scan_qr);
        if (scanQRTab != null) {
            scanQRTab.setOnClickListener(v -> {
                Intent intent = new Intent(EventBrowserActivity.this, QRScannerActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }

        // Admin lock button: check admin status and show menu
        ImageView adminLockButton = findViewById(R.id.btn_admin);
        if (adminLockButton != null) {
            adminLockButton.setOnClickListener(v -> checkAdminAndShowMenu());
        }
    }

    private void checkAdminAndShowMenu() {
        String deviceId = Identity.getOrCreateDeviceId(this);
        UserDB userDB = new UserDB();
        userDB.getUser(deviceId, new UserDB.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.isAdmin()) {
                    showAdminMenu();
                } else {
                    Toast.makeText(EventBrowserActivity.this, 
                        R.string.admin_access_required, Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("EventBrowserActivity", "Failed to check admin status", e);
                Toast.makeText(EventBrowserActivity.this, 
                    R.string.admin_check_status_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAdminMenu() {
        PopupMenu popupMenu = new PopupMenu(this, findViewById(R.id.btn_admin));
        popupMenu.getMenuInflater().inflate(R.menu.menu_admin, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_admin_remove_events) {
                Intent intent = new Intent(this, AdminEventListActivity.class);
                startActivity(intent);
                return true;
            }
            if (item.getItemId() == R.id.menu_admin_remove_profiles) {
                Intent intent = new Intent(this, AdminProfileListActivity.class);
                startActivity(intent);
                return true;
            }
            if (item.getItemId() == R.id.menu_admin_remove_images) {
                Intent intent = new Intent(this, AdminImageListActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

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

        TagDB tagDB = new TagDB();
        tagDB.getAllTags(new TagDB.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> allTagsList) {
                List<String> sortedTags = new ArrayList<>(allTagsList);
                sortedTags.sort(String::compareToIgnoreCase);
                
                createTagChips(tagChipGroup, sortedTags, selectedTags);
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("EventBrowserActivity", "Failed to load tags, falling back to event iteration", e);
                Set<String> allTags = TagHelper.collectAllUniqueTags(allEvents);
                List<String> sortedTags = new ArrayList<>(allTags);
                sortedTags.sort(String::compareToIgnoreCase);
                createTagChips(tagChipGroup, sortedTags, selectedTags);
            }
        });

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


