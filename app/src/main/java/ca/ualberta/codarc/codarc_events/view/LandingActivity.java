package ca.ualberta.codarc.codarc_events.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import ca.ualberta.codarc.codarc_events.databinding.ActivityLandingBinding;

public class LandingActivity extends AppCompatActivity {

    private ActivityLandingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLandingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.btnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(this, RoleSelectionActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
