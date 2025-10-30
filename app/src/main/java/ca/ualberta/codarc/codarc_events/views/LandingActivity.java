package ca.ualberta.codarc.codarc_events.views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import ca.ualberta.codarc.codarc_events.R;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        MaterialButton continueBtn = findViewById(R.id.btn_continue);
        continueBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, UnifiedDashboardActivity.class);
            startActivity(intent);
        });

    }
}
