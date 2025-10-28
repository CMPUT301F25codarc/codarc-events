package ca.ualberta.codarc.codarc_events.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapter.EventCardAdapter;
import ca.ualberta.codarc.codarc_events.data.ProfileStore;
import ca.ualberta.codarc.codarc_events.data.WaitlistLocalRepository;
import ca.ualberta.codarc.codarc_events.databinding.ActivityEntrantDashboardBinding;
import ca.ualberta.codarc.codarc_events.model.EventCard;

public class EntrantDashboardActivity extends AppCompatActivity implements EventCardAdapter.OnEventCardClickListener {

    private ProfileStore profileStore;
    private WaitlistLocalRepository waitRepo;

    private ActivityEntrantDashboardBinding binding;

    private EventCardAdapter eventCardAdapter;
    private List<EventCard> eventCards; // full list

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEntrantDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileStore = new ProfileStore(this);
        waitRepo = new WaitlistLocalRepository(this);

        initializeData();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initializeData() {
        eventCards = new ArrayList<>();

        eventCards.add(new EventCard(
                "evt_swim_001",
                getString(R.string.swimming_lessons),
                getString(R.string.location_prefix) + getString(R.string.ymca_centre),
                getString(R.string.lottery_ends_prefix) + getString(R.string.oct_20),
                getString(R.string.entrants_134),
                waitRepo.isInWaitlist("evt_swim_001")
        ));

        eventCards.add(new EventCard(
                "evt_yoga_002",
                getString(R.string.yoga_wellness),
                getString(R.string.location_prefix) + getString(R.string.wellness_centre),
                getString(R.string.lottery_ends_prefix) + getString(R.string.oct_20),
                getString(R.string.entrants_134),
                waitRepo.isInWaitlist("evt_yoga_002")
        ));

        eventCards.add(new EventCard(
                "evt_skating_003",
                getString(R.string.winter_skating),
                getString(R.string.location_prefix) + getString(R.string.paharchura),
                getString(R.string.lottery_ends_prefix) + getString(R.string.oct_20),
                getString(R.string.entrants_134),
                waitRepo.isInWaitlist("evt_skating_003")
        ));
    }

    private void setupRecyclerView() {
        eventCardAdapter = new EventCardAdapter(eventCards, this);
        binding.rvEvents.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEvents.setAdapter(eventCardAdapter);
    }

    private void setupClickListeners() {

        // Back
        binding.btnBack.setOnClickListener(v -> finish());

        // ✅ Profile icon → open editable profile dialog
        binding.ivProfileSettings.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );


        // ✅ Search Filter
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<EventCard> filtered = new ArrayList<>();
                for (EventCard e : eventCards) {
                    if (e.getTitle().toLowerCase().contains(s.toString().toLowerCase())) {
                        filtered.add(e);
                    }
                }
                eventCardAdapter.updateEventCards(filtered);
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        // Bottom Nav
        binding.tabHistory.setOnClickListener(v ->
                Toast.makeText(this, "History tab selected", Toast.LENGTH_SHORT).show());

        binding.tabScanQr.setOnClickListener(v ->
                Toast.makeText(this, "QR Scanner coming soon", Toast.LENGTH_SHORT).show());

        binding.tabNotifications.setOnClickListener(v ->
                Toast.makeText(this, "Notifications tab selected", Toast.LENGTH_SHORT).show());
    }

    // ✅ Profile Edit Dialog (Reusable)
    private void showProfileDialog(EventCard eventCard) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_complete_profile, null);

        EditText name = view.findViewById(R.id.et_name);
        EditText email = view.findViewById(R.id.et_email);
        EditText phone = view.findViewById(R.id.et_phone);

        // Prefill existing values
        name.setText(profileStore.getName());
        email.setText(profileStore.getEmail());
        phone.setText(profileStore.getPhone());

        builder.setView(view)
                .setTitle("Your Profile")
                .setPositiveButton("Save", (d, w) -> {
                    profileStore.saveProfile(
                            name.getText().toString(),
                            email.getText().toString(),
                            phone.getText().toString()
                    );

                    if (eventCard != null) { onJoinListClick(eventCard); }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ✅ Entrant Actions
    @Override
    public void onViewEntrantsClick(EventCard eventCard) {
        Toast.makeText(this, "Viewing entrants for: " + eventCard.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onJoinListClick(EventCard eventCard) {
        if (!profileStore.isProfileComplete()) {
            showProfileDialog(eventCard);
            return;
        }

        waitRepo.joinWaitlist(eventCard.getEventId());
        eventCard.setJoined(true);
        eventCardAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Added to waiting list ✅", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLeaveListClick(EventCard eventCard) {
        waitRepo.leaveWaitlist(eventCard.getEventId());
        eventCard.setJoined(false);
        eventCardAdapter.notifyDataSetChanged();
        Toast.makeText(this, "Removed from waiting list ❌", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
