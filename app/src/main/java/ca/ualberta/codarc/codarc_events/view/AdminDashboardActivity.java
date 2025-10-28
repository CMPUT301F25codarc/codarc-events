package ca.ualberta.codarc.codarc_events.view;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ca.ualberta.codarc.codarc_events.databinding.ActivityAdminDashboardBinding;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> {
            Toast.makeText(this, "Returning to login...", Toast.LENGTH_SHORT).show();
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        binding.ivProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Admin profile settings coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.cardActiveEvents.setOnClickListener(v -> {
            Toast.makeText(this, "Viewing active events", Toast.LENGTH_SHORT).show();
        });

        binding.cardTotalUsers.setOnClickListener(v -> {
            Toast.makeText(this, "Viewing total users", Toast.LENGTH_SHORT).show();
        });

        binding.cardPendingImages.setOnClickListener(v -> {
            Toast.makeText(this, "Viewing pending images", Toast.LENGTH_SHORT).show();
        });

        binding.cardReports.setOnClickListener(v -> {
            Toast.makeText(this, "Viewing reports", Toast.LENGTH_SHORT).show();
        });

        binding.cardBrowseEvents.setOnClickListener(v -> {
            Toast.makeText(this, "Browsing events", Toast.LENGTH_SHORT).show();
        });

        binding.cardUserProfiles.setOnClickListener(v -> {
            Toast.makeText(this, "Managing user profiles", Toast.LENGTH_SHORT).show();
        });

        binding.cardImageReview.setOnClickListener(v -> {
            Toast.makeText(this, "Reviewing images", Toast.LENGTH_SHORT).show();
        });

        binding.cardNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "Viewing notification logs", Toast.LENGTH_SHORT).show();
        });

        binding.tabHistory.setOnClickListener(v -> {
            Toast.makeText(this, "History tab selected", Toast.LENGTH_SHORT).show();
        });

        binding.tabStats.setOnClickListener(v -> {
            Toast.makeText(this, "Stats tab selected", Toast.LENGTH_SHORT).show();
        });

        binding.tabNotifications.setOnClickListener(v -> {
            Toast.makeText(this, "Notifications tab selected", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}