package ca.ualberta.codarc.codarc_events.views;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Handles creation, update, and deletion of an entrant's profile.
 *
 * <p>Users can view and edit their profile information, delete their profile,
 * or navigate back to the event dashboard. This activity loads existing profile
 * data if available and allows users to modify their name, email, and phone number.</p>
 * 
 * <p>In the refactored structure:
 * - Creates an Entrants document with profile data
 * - Sets isEntrant = true in the Users collection
 * </p>
 */
public class ProfileCreationActivity extends AppCompatActivity {

    private EntrantDB entrantDB;
    private UserDB userDB;
    private String deviceId;
    private EditText nameEt;
    private EditText emailEt;
    private EditText phoneEt;
    private MaterialButton saveBtn;
    private MaterialButton deleteBtn;
    private ImageView backBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_creation);

        // Initialize views
        nameEt = findViewById(R.id.et_name);
        emailEt = findViewById(R.id.et_email);
        phoneEt = findViewById(R.id.et_phone);
        saveBtn = findViewById(R.id.btn_create_profile);
        deleteBtn = findViewById(R.id.btn_delete_profile);
        backBtn = findViewById(R.id.iv_back);

        // Initialize Firestore helpers
        entrantDB = new EntrantDB();
        userDB = new UserDB();
        deviceId = Identity.getOrCreateDeviceId(this);

        // Load existing profile data if available
        loadProfile();

        // Handle Save button
        if (saveBtn != null) {
            saveBtn.setOnClickListener(v -> saveOrUpdateProfile());
        }

        // Handle Delete button
        if (deleteBtn != null) {
            deleteBtn.setOnClickListener(v -> confirmAndDeleteProfile());
        }

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
     * If no profile exists, fields remain empty for new profile creation.
     */
    private void loadProfile() {
        if (nameEt == null || emailEt == null || phoneEt == null) {
            return;
        }
        entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
            @Override
            public void onSuccess(Entrant entrant) {
                if (entrant != null) {
                    if (entrant.getName() != null) {
                        nameEt.setText(entrant.getName());
                    }
                    if (entrant.getEmail() != null) {
                        emailEt.setText(entrant.getEmail());
                    }
                    if (entrant.getPhone() != null) {
                        phoneEt.setText(entrant.getPhone());
                    }
                }
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                // Profile might not exist yet — ignore. User will create one.
            }
        });
    }

    /**
     * Validates input fields and saves/updates profile in Firestore.
     * Sets the isRegistered flag to true upon successful save.
     * 
     * In the refactored structure:
     * - First checks if Entrant document exists
     * - If not, creates it (first time) and sets isEntrant = true in Users
     * - If exists, just updates the profile data
     */
    private void saveOrUpdateProfile() {
        if (nameEt == null || emailEt == null || phoneEt == null) {
            return;
        }
        String name = nameEt.getText().toString().trim();
        String email = emailEt.getText().toString().trim();
        String phone = phoneEt.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameEt.setError("Name required");
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEt.setError("Valid email required");
            return;
        }

        Entrant entrant = new Entrant(deviceId, name, System.currentTimeMillis());
        entrant.setEmail(email);
        entrant.setPhone(phone);
        entrant.setIsRegistered(true);

        saveBtn.setEnabled(false);
        
        // Check if this is first time creating profile
        entrantDB.entrantExists(deviceId, new EntrantDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean exists) {
                if (!exists) {
                    // First time - create Entrant and set isEntrant flag in Users
                    createNewEntrantProfile(entrant);
                } else {
                    // Already exists - just update
                    updateExistingEntrantProfile(entrant);
                }
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                // If check fails, try to upsert anyway
                updateExistingEntrantProfile(entrant);
            }
        });
    }
    
    /**
     * Creates a new Entrant profile and sets isEntrant flag in Users collection.
     * This is called when user creates their profile for the first time.
     */
    private void createNewEntrantProfile(Entrant entrant) {
        entrantDB.createEntrant(entrant, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                // Set isEntrant = true in Users collection
                userDB.setEntrantRole(deviceId, true, new UserDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        Toast.makeText(ProfileCreationActivity.this, "Profile created successfully", Toast.LENGTH_SHORT).show();
                        saveBtn.setEnabled(true);
                        finish();
                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception e) {
                        // Profile created but role flag failed - not critical, continue
                        Toast.makeText(ProfileCreationActivity.this, "Profile created", Toast.LENGTH_SHORT).show();
                        saveBtn.setEnabled(true);
                        finish();
                        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                    }
                });
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                saveBtn.setEnabled(true);
                Toast.makeText(ProfileCreationActivity.this, "Error creating profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Updates an existing Entrant profile.
     */
    private void updateExistingEntrantProfile(Entrant entrant) {
        entrantDB.upsertProfile(deviceId, entrant, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Toast.makeText(ProfileCreationActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                saveBtn.setEnabled(true);
                finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                saveBtn.setEnabled(true);
                Toast.makeText(ProfileCreationActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows confirmation dialog before deleting the user's profile.
     * Prevents accidental deletion by requiring user confirmation.
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
     * Clears the profile information and sets registration status to false.
     * Preserves the device identity document to prevent waitlist join errors.
     */
    private void deleteProfile() {
        deleteBtn.setEnabled(false);
        entrantDB.deleteProfile(deviceId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Toast.makeText(ProfileCreationActivity.this, "Profile deleted", Toast.LENGTH_SHORT).show();
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                deleteBtn.setEnabled(true);
                Toast.makeText(ProfileCreationActivity.this, "Failed to delete profile", Toast.LENGTH_SHORT).show();
            }
        });
    }
}


