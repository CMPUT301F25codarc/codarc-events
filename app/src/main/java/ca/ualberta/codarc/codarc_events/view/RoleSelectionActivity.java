package ca.ualberta.codarc.codarc_events.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import ca.ualberta.codarc.codarc_events.databinding.ActivityRoleSelectionBinding;

public class RoleSelectionActivity extends AppCompatActivity {

    private ActivityRoleSelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoleSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        binding.cardOrganizer.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("role", "organizer");
            startActivity(intent);
        });

        binding.cardEntrant.setOnClickListener(v -> {
            Intent intent = new Intent(this, EntrantDashboardActivity.class);
            startActivity(intent);
        });

        binding.cardAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("role", "admin");
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
