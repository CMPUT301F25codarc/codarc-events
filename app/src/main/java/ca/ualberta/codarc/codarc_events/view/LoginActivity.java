package ca.ualberta.codarc.codarc_events.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ca.ualberta.codarc.codarc_events.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private String selectedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectedRole = getIntent().getStringExtra("role");
        
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        binding.btnLogin.setOnClickListener(v -> {
            if ("organizer".equals(selectedRole)) {
                Intent intent = new Intent(this, OrganizerDashboardActivity.class);
                startActivity(intent);
            } else if ("admin".equals(selectedRole)) {
                Intent intent = new Intent(this, AdminDashboardActivity.class);
                startActivity(intent);
            }
        });

        binding.btnDebugOrganizer.setOnClickListener(v -> {
            Intent intent = new Intent(this, OrganizerDashboardActivity.class);
            startActivity(intent);
        });

        binding.btnDebugAdmin.setOnClickListener(v -> {
            Toast.makeText(this, "Navigating to Admin Dashboard...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, AdminDashboardActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
