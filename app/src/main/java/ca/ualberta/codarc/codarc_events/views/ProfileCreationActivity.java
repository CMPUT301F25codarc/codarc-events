package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import com.google.firebase.firestore.FieldValue;

/**
 * Simple form that collects name, email, and optional phone.
 * On save we mark the profile as registered so the user can join events.
 */
public class ProfileCreationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_creation);

        EditText nameEt = findViewById(R.id.et_name);
        EditText emailEt = findViewById(R.id.et_email);
        EditText phoneEt = findViewById(R.id.et_phone);
        MaterialButton createBtn = findViewById(R.id.btn_create_profile);

        String deviceId = Identity.getOrCreateDeviceId(this);
        EntrantDB entrantDB = new EntrantDB();

        // try to load existing profile data
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
                // if profile doesn't exist yet that's fine, user will create one
            }
        });

        createBtn.setOnClickListener(v -> {
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

            Entrant entrant = new Entrant(deviceId, name, FieldValue.serverTimestamp());
            entrant.setEmail(email);
            entrant.setPhone(phone);
            entrant.setIsRegistered(true);

            createBtn.setEnabled(false);
            entrantDB.upsertProfile(deviceId, entrant, new EntrantDB.Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                    Toast.makeText(ProfileCreationActivity.this, "Profile created", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(@androidx.annotation.NonNull Exception e) {
                    createBtn.setEnabled(true);
                    Toast.makeText(ProfileCreationActivity.this, "Couldn't save profile", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}


