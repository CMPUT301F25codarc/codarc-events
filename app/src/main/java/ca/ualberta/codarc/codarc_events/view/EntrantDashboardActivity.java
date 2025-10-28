package ca.ualberta.codarc.codarc_events.view;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapter.EventCardAdapter;
import ca.ualberta.codarc.codarc_events.databinding.ActivityEntrantDashboardBinding;
import ca.ualberta.codarc.codarc_events.model.EventCard;

public class EntrantDashboardActivity extends AppCompatActivity implements EventCardAdapter.OnEventCardClickListener {

    private ActivityEntrantDashboardBinding binding;
    private EventCardAdapter eventCardAdapter;
    private List<EventCard> eventCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEntrantDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        initializeData();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initializeData() {
        eventCards = new ArrayList<>();
        
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
        binding.btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        binding.ivFilter.setOnClickListener(v -> {
            Toast.makeText(this, "Filter functionality coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.ivProfileSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Profile settings coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.tabHistory.setOnClickListener(v -> {
            Toast.makeText(this, "History tab selected", Toast.LENGTH_SHORT).show();
        });

        binding.tabScanQr.setOnClickListener(v -> {
            Toast.makeText(this, "QR Scanner coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.tabNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "Notifications tab selected", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onViewEntrantsClick(EventCard eventCard) {
        Toast.makeText(this, "Viewing entrants for: " + eventCard.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onJoinListClick(EventCard eventCard) {
        Toast.makeText(this, "Joining list for: " + eventCard.getTitle(), Toast.LENGTH_SHORT).show();
        eventCard.setJoined(true);
        eventCardAdapter.notifyDataSetChanged();
    }

    @Override
    public void onLeaveListClick(EventCard eventCard) {
        Toast.makeText(this, "Leaving list for: " + eventCard.getTitle(), Toast.LENGTH_SHORT).show();
        eventCard.setJoined(false);
        eventCardAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
