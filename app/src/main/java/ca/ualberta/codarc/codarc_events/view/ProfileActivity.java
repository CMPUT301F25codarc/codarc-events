package ca.ualberta.codarc.codarc_events.view;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import ca.ualberta.codarc.codarc_events.databinding.ActivityProfileBinding;
import ca.ualberta.codarc.codarc_events.data.ProfileStore;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private ProfileStore profileStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileStore = new ProfileStore(this);

        loadProfile();
        setupListeners();
    }

    private void loadProfile() {
        binding.etName.setText(profileStore.getName());
        binding.etEmail.setText(profileStore.getEmail());
        binding.etPhone.setText(profileStore.getPhone());
    }

    private void setupListeners() {
        binding.btnSave.setOnClickListener(v -> {
            profileStore.saveProfile(
                    binding.etName.getText().toString(),
                    binding.etEmail.getText().toString(),
                    binding.etPhone.getText().toString()
            );
            Toast.makeText(this, "Profile Saved ✅", Toast.LENGTH_SHORT).show();
            finish();
        });

        binding.btnClose.setOnClickListener(v -> finish());
    }
}
