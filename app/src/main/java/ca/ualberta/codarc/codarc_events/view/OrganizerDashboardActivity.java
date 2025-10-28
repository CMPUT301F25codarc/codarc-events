package ca.ualberta.codarc.codarc_events.view;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapter.OrganizerEventCardAdapter;
import ca.ualberta.codarc.codarc_events.databinding.ActivityOrganizerDashboardBinding;
import ca.ualberta.codarc.codarc_events.model.OrganizerEventCard;

public class OrganizerDashboardActivity extends AppCompatActivity implements OrganizerEventCardAdapter.OnOrganizerEventCardClickListener {

    private ActivityOrganizerDashboardBinding binding;
    private OrganizerEventCardAdapter eventCardAdapter;
    private List<OrganizerEventCard> eventCards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrganizerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        initializeData();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initializeData() {
        eventCards = new ArrayList<>();
        
        eventCards.add(new OrganizerEventCard(
                getString(R.string.swim_lessons_beginners),
                getString(R.string.sep_10_dec_5),
                getString(R.string.registered_45_60),
                getString(R.string.registration_closes_format, getString(R.string.dec_15)),
                "event_1"
        ));
        
        eventCards.add(new OrganizerEventCard(
                getString(R.string.yoga_wellness_organizer),
                getString(R.string.sep_29_dec_1),
                getString(R.string.registered_32_50),
                getString(R.string.registration_closes_format, getString(R.string.dec_31)),
                "event_2"
        ));
        
        eventCards.add(new OrganizerEventCard(
                getString(R.string.winter_skating_organizer),
                getString(R.string.sep_16_dec_31),
                getString(R.string.registered_120_200),
                getString(R.string.registration_closes_format, getString(R.string.jan_21)),
                "event_3"
        ));
    }

    private void setupRecyclerView() {
        eventCardAdapter = new OrganizerEventCardAdapter(eventCards, this);
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEvents.setAdapter(eventCardAdapter);
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        binding.ivProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Profile settings coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.tvViewAll.setOnClickListener(v -> {
            Toast.makeText(this, "View all events coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.btnCreateEvent.setOnClickListener(v -> {
            Toast.makeText(this, "Create Event functionality coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.tabDashboard.setOnClickListener(v -> {
            Toast.makeText(this, "Dashboard tab selected", Toast.LENGTH_SHORT).show();
        });

        binding.tabEntrants.setOnClickListener(v -> {
            Toast.makeText(this, "Entrants tab selected", Toast.LENGTH_SHORT).show();
        });

        binding.tabManage.setOnClickListener(v -> {
            Toast.makeText(this, "Manage tab selected", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onViewEntrantsClick(OrganizerEventCard eventCard) {
        Toast.makeText(this, "Viewing entrants for: " + eventCard.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRunLotteryClick(OrganizerEventCard eventCard) {
        Toast.makeText(this, "Running lottery for: " + eventCard.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNotifyClick(OrganizerEventCard eventCard) {
        Toast.makeText(this, "Sending notifications for: " + eventCard.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEventMenuClick(OrganizerEventCard eventCard) {
        Toast.makeText(this, "Event menu for: " + eventCard.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}