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
import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.annotation.NonNull;
import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.controllers.DeleteOwnProfileController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Handles creation, update, and deletion of an entrant's profile.
 */
public class ProfileCreationActivity extends AppCompatActivity {

    private EntrantDB entrantDB;
    private UserDB userDB;
    private DeleteOwnProfileController deleteOwnProfileController;
    private String deviceId;
    private EditText nameEt;
    private EditText emailEt;
    private EditText phoneEt;
    private SwitchMaterial switchNotificationEnabled;
    private MaterialButton saveBtn;
    private MaterialButton deleteBtn;
    private ImageView backBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_creation);

        nameEt = findViewById(R.id.et_name);
        emailEt = findViewById(R.id.et_email);
        phoneEt = findViewById(R.id.et_phone);
        switchNotificationEnabled = findViewById(R.id.switch_notification_enabled);
        saveBtn = findViewById(R.id.btn_create_profile);
        deleteBtn = findViewById(R.id.btn_delete_profile);
        backBtn = findViewById(R.id.iv_back);

        entrantDB = new EntrantDB();
        userDB = new UserDB();
        deleteOwnProfileController = new DeleteOwnProfileController();
        deviceId = Identity.getOrCreateDeviceId(this);

        loadProfile();
        setupNotificationToggle();

        if (saveBtn != null) {
            saveBtn.setOnClickListener(v -> saveOrUpdateProfile());
        }

        if (deleteBtn != null) {
            deleteBtn.setOnClickListener(v -> confirmAndDeleteProfile());
        }

        if (backBtn != null) {
            backBtn.setOnClickListener(v -> {
                onBackPressed();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }
    }

    private void loadProfile() {
        if (nameEt == null || emailEt == null || phoneEt == null) {
            return;
        }
        entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
            @Override
            public void onSuccess(Entrant entrant) {
                if (entrant != null && entrant.isBanned()) {
                    showBannedMessage();
                    return;
                }
                
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
                    if (switchNotificationEnabled != null) {
                        switchNotificationEnabled.setChecked(entrant.isNotificationEnabled());
                    }
                } else {
                    if (switchNotificationEnabled != null) {
                        loadNotificationPreference();
                    }
                }
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                if (switchNotificationEnabled != null) {
                    loadNotificationPreference();
                }
            }
        });
    }

    private void loadNotificationPreference() {
        entrantDB.getNotificationPreference(deviceId, new EntrantDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean enabled) {
                if (switchNotificationEnabled != null) {
                    switchNotificationEnabled.setChecked(enabled != null && enabled);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                if (switchNotificationEnabled != null) {
                    switchNotificationEnabled.setChecked(true);
                }
            }
        });
    }

    private void setupNotificationToggle() {
        if (switchNotificationEnabled == null) {
            return;
        }

        switchNotificationEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationPreference(isChecked);
        });
    }

    private void saveNotificationPreference(boolean enabled) {
        entrantDB.setNotificationPreference(deviceId, enabled, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Toast.makeText(ProfileCreationActivity.this,
                        R.string.notification_preference_updated, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(ProfileCreationActivity.this,
                        R.string.notification_preference_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows a message indicating the user is banned and disables profile editing.
     */
    private void showBannedMessage() {
        if (saveBtn != null) {
            saveBtn.setEnabled(false);
        }
        if (deleteBtn != null) {
            deleteBtn.setEnabled(false);
        }
        if (nameEt != null) {
            nameEt.setEnabled(false);
        }
        if (emailEt != null) {
            emailEt.setEnabled(false);
        }
        if (phoneEt != null) {
            phoneEt.setEnabled(false);
        }
        
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_banned_title)
                .setMessage(R.string.profile_banned_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    finish();
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                })
                .setCancelable(false)
                .show();
    }

    private void saveOrUpdateProfile() {
        if (nameEt == null || emailEt == null || phoneEt == null) {
            return;
        }
        
        entrantDB.isBanned(deviceId, new EntrantDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isBanned) {
                if (isBanned != null && isBanned) {
                    showBannedMessage();
                    return;
                }
                
                proceedWithSaveOrUpdate();
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                proceedWithSaveOrUpdate();
            }
        });
    }

    private void proceedWithSaveOrUpdate() {
        if (nameEt == null || emailEt == null || phoneEt == null) {
            return;
        }
        String name = nameEt.getText().toString().trim();
        String email = emailEt.getText().toString().trim();
        String phone = phoneEt.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameEt.setError(getString(R.string.profile_name_required));
            return;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEt.setError(getString(R.string.profile_email_required));
            return;
        }

        entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
            @Override
            public void onSuccess(Entrant existing) {
                Entrant entrant = new Entrant(deviceId, name, 
                    existing != null ? existing.getCreatedAtUtc() : System.currentTimeMillis());
                entrant.setEmail(email);
                entrant.setPhone(phone);
                entrant.setIsRegistered(true);
                if (existing != null && existing.isBanned()) {
                    entrant.setBanned(true);
                }
                if (switchNotificationEnabled != null) {
                    entrant.setNotificationEnabled(switchNotificationEnabled.isChecked());
                }

                saveBtn.setEnabled(false);
                
                entrantDB.entrantExists(deviceId, new EntrantDB.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean exists) {
                        if (!exists) {
                            createNewEntrantProfile(entrant);
                        } else {
                            updateExistingEntrantProfile(entrant);
                        }
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull Exception e) {
                        updateExistingEntrantProfile(entrant);
                    }
                });
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                Entrant entrant = new Entrant(deviceId, name, System.currentTimeMillis());
                entrant.setEmail(email);
                entrant.setPhone(phone);
                entrant.setIsRegistered(true);
                if (switchNotificationEnabled != null) {
                    entrant.setNotificationEnabled(switchNotificationEnabled.isChecked());
                }
                
                saveBtn.setEnabled(false);
                createNewEntrantProfile(entrant);
            }
        });
    }
    
    /**
     * Creates a new Entrant profile.
     */
    private void createNewEntrantProfile(Entrant entrant) {
        entrantDB.createEntrant(entrant, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
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
     * Clears the profile information and removes from all events.
     */
    private void deleteProfile() {
        deleteBtn.setEnabled(false);
        deleteOwnProfileController.deleteOwnProfile(deviceId, new DeleteOwnProfileController.Callback() {
            @Override
            public void onResult(DeleteOwnProfileController.DeleteProfileResult result) {
                runOnUiThread(() -> {
                    if (result.isSuccess()) {
                        Toast.makeText(ProfileCreationActivity.this, "Profile deleted", Toast.LENGTH_SHORT).show();
                        finish();
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    } else {
                        deleteBtn.setEnabled(true);
                        Toast.makeText(ProfileCreationActivity.this,
                                result.getErrorMessage() != null ? result.getErrorMessage() : "Failed to delete profile",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}

