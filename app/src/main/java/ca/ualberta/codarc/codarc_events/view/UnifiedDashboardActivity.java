package ca.ualberta.codarc.codarc_events.view;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapter.EventCardAdapter;
import ca.ualberta.codarc.codarc_events.databinding.ActivityUnifiedDashboardBinding;
import ca.ualberta.codarc.codarc_events.model.EventCard;

// Main dashboard for all user types with role-based functionality
public class UnifiedDashboardActivity extends AppCompatActivity implements EventCardAdapter.OnEventCardClickListener {

    private ActivityUnifiedDashboardBinding binding;
    private EventCardAdapter eventCardAdapter;
    private List<EventCard> eventCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUnifiedDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        initializeData();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initializeData() {
        eventCards = new ArrayList<>();
        
        // Mock data for testing
        eventCards.add(new EventCard(
                getString(R.string.swimming_lessons),
                getString(R.string.location_prefix) + getString(R.string.ymca_centre),
                getString(R.string.lottery_ends_prefix) + getString(R.string.oct_20),
                getString(R.string.entrants_134),
                false
        ));
        
        eventCards.add(new EventCard(
                getString(R.string.yoga_wellness),
                getString(R.string.location_prefix) + getString(R.string.wellness_centre),
                getString(R.string.lottery_ends_prefix) + getString(R.string.oct_20),
                getString(R.string.entrants_134),
                false
        ));
        
        eventCards.add(new EventCard(
                getString(R.string.winter_skating),
                getString(R.string.location_prefix) + getString(R.string.paharchura),
                getString(R.string.lottery_ends_prefix) + getString(R.string.oct_20),
                getString(R.string.entrants_134),
                false
        ));
    }

    private void setupRecyclerView() {
        eventCardAdapter = new EventCardAdapter(eventCards, this);
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEvents.setAdapter(eventCardAdapter);
    }

    private void setupClickListeners() {
        binding.ivFilter.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.filter_coming_soon), Toast.LENGTH_SHORT).show();
        });

        binding.ivProfileSettings.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.profile_coming_soon), Toast.LENGTH_SHORT).show();
        });

        binding.btnPlus.setOnClickListener(v -> showOrganizerMenu());
        binding.btnAdmin.setOnClickListener(v -> showAdminMenu());

        binding.tabHistory.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.history_tab_selected), Toast.LENGTH_SHORT).show();
        });

        binding.tabScanQr.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.qr_scanner_coming_soon), Toast.LENGTH_SHORT).show();
        });

        binding.tabNotifications.setOnClickListener(v -> {
            Toast.makeText(this, getString(R.string.notifications_tab_selected), Toast.LENGTH_SHORT).show();
        });
    }

    private void showOrganizerMenu() {
        PopupMenu popup = new PopupMenu(this, binding.btnPlus);
        popup.getMenuInflater().inflate(R.menu.menu_organizer, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.menu_create_event) {
                Toast.makeText(this, getString(R.string.create_event_coming_soon), Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_my_events) {
                Toast.makeText(this, getString(R.string.my_events_coming_soon), Toast.LENGTH_SHORT).show();
                return true;
            }
            
            return false;
        });
        
        popup.show();
    }

    private void showAdminMenu() {
        PopupMenu popup = new PopupMenu(this, binding.btnAdmin);
        popup.getMenuInflater().inflate(R.menu.menu_admin, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.menu_admin_placeholder) {
                Toast.makeText(this, getString(R.string.admin_functions_placeholder), Toast.LENGTH_SHORT).show();
                return true;
            }
            
            return false;
        });
        
        popup.show();
    }

    @Override
    public void onViewEntrantsClick(EventCard eventCard) {
        Toast.makeText(this, getString(R.string.viewing_entrants_for, eventCard.getTitle()), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onJoinListClick(EventCard eventCard) {
        Toast.makeText(this, getString(R.string.joining_list_for, eventCard.getTitle()), Toast.LENGTH_SHORT).show();
        eventCard.setJoined(true);
        eventCardAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLeaveListClick(EventCard eventCard) {
        Toast.makeText(this, getString(R.string.leaving_list_for, eventCard.getTitle()), Toast.LENGTH_SHORT).show();
        eventCard.setJoined(false);
        eventCardAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}

