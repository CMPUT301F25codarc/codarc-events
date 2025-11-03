package ca.ualberta.codarc.codarc_events.views;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.controllers.ProfileController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Handles creation, update, and deletion of an entrant's profile.
 * Users can view and edit their info, delete their profile,
 * or navigate back to the event dashboard.
 */
public class ProfileCreationActivity extends AppCompatActivity {

    private ProfileController profileController;
    private String deviceId;
    private EditText nameEt, emailEt, phoneEt;
    private MaterialButton createBtn, deleteBtn;
    private ImageView backBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_creation);

        // Initialize views
        nameEt = findViewById(R.id.et_name);
        emailEt = findViewById(R.id.et_email);
        phoneEt = findViewById(R.id.et_phone);
        createBtn = findViewById(R.id.btn_create_profile);
        deleteBtn = findViewById(R.id.btn_delete_profile);
        backBtn = findViewById(R.id.iv_back);

        // Initialize controller and get device ID
        EntrantDB entrantDB = new EntrantDB();
        profileController = new ProfileController(entrantDB);
        deviceId = Identity.getOrCreateDeviceId(this);

        // Load existing profile data
        loadProfile();

        // Handle Save button
        createBtn.setOnClickListener(v -> saveOrUpdateProfile());

        // Handle Delete button
        deleteBtn.setOnClickListener(v -> confirmAndDeleteProfile());

        // Handle Back button
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                onBackPressed();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }
    }

    /**
     * Loads existing profile info from Firestore and fills input fields.
     */
    private void loadProfile() {
        EntrantDB entrantDB = new EntrantDB();
        entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
            @Override
            public void onSuccess(Entrant entrant) {
                if (entrant != null) {
                    if (entrant.getName() != null) nameEt.setText(entrant.getName());
                    if (entrant.getEmail() != null) emailEt.setText(entrant.getEmail());
                    if (entrant.getPhone() != null) phoneEt.setText(entrant.getPhone());
                }
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                // Profile might not exist yet — ignore.
            }
        });
    }

    /**
     * Validates input fields and saves/updates profile in Firestore.
     * Delegates validation and persistence to ProfileController.
     */
    private void saveOrUpdateProfile() {
        String name = nameEt.getText().toString().trim();
        String email = emailEt.getText().toString().trim();
        String phone = phoneEt.getText().toString().trim();

        // Validate using controller
        ProfileController.ProfileResult result = profileController.validateAndCreateProfile(
                deviceId, name, email, phone);

        if (!result.isValid()) {
            String errorMessage = result.getErrorMessage();
            if (errorMessage.contains("Name")) {
                nameEt.setError("Name required");
            } else if (errorMessage.contains("Email")) {
                emailEt.setError("Valid email required");
            } else {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Entrant entrant = result.getEntrant();
        createBtn.setEnabled(false);
        profileController.saveProfile(deviceId, entrant, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Toast.makeText(ProfileCreationActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                createBtn.setEnabled(true);
                finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                createBtn.setEnabled(true);
                Toast.makeText(ProfileCreationActivity.this, "Error saving profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows confirmation dialog before deleting the user's profile.
     */
    private void confirmAndDeleteProfile() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete your profile? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the profile document from Firestore.
     * Delegates to ProfileController for business logic.
     */
    private void deleteProfile() {
        profileController.deleteProfile(deviceId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Toast.makeText(ProfileCreationActivity.this, "Profile deleted", Toast.LENGTH_SHORT).show();
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                Toast.makeText(ProfileCreationActivity.this, "Failed to delete profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}


