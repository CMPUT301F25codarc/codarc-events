package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.RegistrationHistoryAdapter;
import ca.ualberta.codarc.codarc_events.controllers.RegistrationHistoryController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.RegistrationHistoryEntry;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Displays the registration history for an entrant.
 */
public class RegistrationHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RegistrationHistoryAdapter adapter;
    private TextView emptyStateView;
    private ProgressBar loadingView;
    private ImageView backButton;

    private RegistrationHistoryController controller;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_history);

        deviceId = Identity.getOrCreateDeviceId(this);

        EntrantDB entrantDB = new EntrantDB();
        EventDB eventDB = new EventDB();
        controller = new RegistrationHistoryController(entrantDB, eventDB);

        recyclerView = findViewById(R.id.rv_history);
        emptyStateView = findViewById(R.id.tv_history_empty);
        loadingView = findViewById(R.id.pb_history_loading);
        backButton = findViewById(R.id.iv_back);

        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                finish();
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            });
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RegistrationHistoryAdapter();
        recyclerView.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        showLoading(true);
        controller.loadRegistrationHistory(deviceId, new RegistrationHistoryController.Callback() {
            @Override
            public void onResult(@NonNull RegistrationHistoryController.HistoryResult result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (result.isSuccess()) {
                        List<RegistrationHistoryEntry> entries = result.getEntries();
                        adapter.setItems(entries);
                        updateEmptyState(entries == null || entries.isEmpty());
                    } else {
                        String errorMessage = result.getErrorMessage();
                        if (errorMessage == null || errorMessage.isEmpty()) {
                            errorMessage = "Failed to load history. Please try again.";
                        }
                        Toast.makeText(RegistrationHistoryActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        updateEmptyState(true);
                    }
                });
            }
        });
    }

    private void showLoading(boolean show) {
        loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyState(boolean isEmpty) {
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}


