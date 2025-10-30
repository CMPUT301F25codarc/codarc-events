package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.utils.Identity;

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

        // Pre-fill if profile exists
        entrantDB.getProfile(deviceId, new EntrantDB.Callback<com.google.firebase.firestore.DocumentSnapshot>() {
            @Override
            public void onSuccess(com.google.firebase.firestore.DocumentSnapshot snapshot) {
                if (snapshot != null && snapshot.exists()) {
                    String name = snapshot.getString("name");
                    String email = snapshot.getString("email");
                    String phone = snapshot.getString("phone");
                    if (name != null) nameEt.setText(name);
                    if (email != null) emailEt.setText(email);
                    if (phone != null) phoneEt.setText(phone);
                }
            }

            @Override
            public void onError(@androidx.annotation.NonNull Exception e) {
                // ignore prefill errors; user can still enter data.
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

            Map<String, Object> fields = new HashMap<>();
            fields.put("name", name);
            fields.put("email", email);
            fields.put("phone", phone);
            fields.put("is_registered", true);

            createBtn.setEnabled(false);
            entrantDB.upsertProfile(deviceId, fields, new EntrantDB.Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                    Toast.makeText(ProfileCreationActivity.this, "Profile created", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(@androidx.annotation.NonNull Exception e) {
                    createBtn.setEnabled(true);
                    Toast.makeText(ProfileCreationActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}


