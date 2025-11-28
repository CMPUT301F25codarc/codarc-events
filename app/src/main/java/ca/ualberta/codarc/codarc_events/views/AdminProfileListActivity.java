package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.AdminProfileListAdapter;
import ca.ualberta.codarc.codarc_events.controllers.RemoveProfileController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Displays a list of all entrant profiles for administrators to browse and manage.
 * Provides view functionality with remove option for each profile.
 */
public class AdminProfileListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminProfileListAdapter adapter;
    private ProgressBar loadingView;
    private TextView emptyStateView;
    private TextView errorStateView;

    private EntrantDB entrantDB;
    private RemoveProfileController removeController;
    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile_list);

        deviceId = Identity.getOrCreateDeviceId(this);
        entrantDB = new EntrantDB();
        removeController = new RemoveProfileController();

        recyclerView = findViewById(R.id.rv_admin_profiles);
        loadingView = findViewById(R.id.pb_loading);
        emptyStateView = findViewById(R.id.tv_empty_state);
        errorStateView = findViewById(R.id.tv_error_state);

        ImageButton backButton = findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminProfileListAdapter(entrant -> handleRemoveClick(entrant));
        recyclerView.setAdapter(adapter);

        // Set up error state retry
        if (errorStateView != null) {
            errorStateView.setOnClickListener(v -> loadProfiles());
        }

        loadProfiles();
    }

    /**
     * Loads all profiles from Firestore and displays them in the RecyclerView.
     * Filters out banned profiles as they should not appear in the removal list.
     */
    private void loadProfiles() {
        showLoading(true);
        showEmptyState(false);
        showErrorState(false);

        entrantDB.getAllEntrants(new EntrantDB.Callback<List<Entrant>>() {
            @Override
            public void onSuccess(List<Entrant> profiles) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (profiles == null || profiles.isEmpty()) {
                        adapter.setItems(null);
                        showEmptyState(true);
                    } else {
                        // Filter out banned profiles
                        List<Entrant> nonBannedProfiles = filterBannedProfiles(profiles);
                        if (nonBannedProfiles.isEmpty()) {
                            adapter.setItems(null);
                            showEmptyState(true);
                        } else {
                            adapter.setItems(nonBannedProfiles);
                            showEmptyState(false);
                        }
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.e("AdminProfileListActivity", "Failed to load profiles", e);
                runOnUiThread(() -> {
                    showLoading(false);
                    showErrorState(true);
                    showEmptyState(false);
                });
            }
        });
    }

    /**
     * Filters out banned profiles from the list.
     * Banned profiles should not appear in the removal list as they are already banned.
     *
     * @param profiles the list of all profiles
     * @return list of non-banned profiles
     */
    private List<Entrant> filterBannedProfiles(List<Entrant> profiles) {
        List<Entrant> nonBannedProfiles = new java.util.ArrayList<>();
        if (profiles == null) {
            return nonBannedProfiles;
        }
        
        for (Entrant entrant : profiles) {
            if (entrant != null && !entrant.isBanned()) {
                nonBannedProfiles.add(entrant);
            }
        }
        
        return nonBannedProfiles;
    }

    /**
     * Handles the remove button click for a profile.
     * Shows a confirmation dialog before proceeding with removal.
     *
     * @param entrant the entrant profile to remove
     */
    private void handleRemoveClick(Entrant entrant) {
        if (entrant == null || entrant.getDeviceId() == null) {
            return;
        }

        String entrantName = entrant.getName();
        if (entrantName == null || entrantName.trim().isEmpty()) {
            entrantName = entrant.getDeviceId();
        }
        String message = getString(R.string.admin_remove_profile_confirm, entrantName);

        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_remove_profiles_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    removeProfile(entrant.getDeviceId());
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    /**
     * Removes a profile using the RemoveProfileController.
     *
     * @param entrantDeviceId the device ID of the entrant to remove
     */
    private void removeProfile(String entrantDeviceId) {
        showLoading(true);
        removeController.removeProfile(entrantDeviceId, deviceId, new RemoveProfileController.Callback() {
            @Override
            public void onResult(RemoveProfileController.RemoveProfileResult result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (result.isSuccess()) {
                        Toast.makeText(AdminProfileListActivity.this,
                                R.string.admin_remove_profile_success, Toast.LENGTH_SHORT).show();
                        // Reload profiles to refresh the list
                        loadProfiles();
                    } else {
                        Toast.makeText(AdminProfileListActivity.this,
                                result.getErrorMessage() != null ? result.getErrorMessage() :
                                        getString(R.string.admin_remove_profile_error),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * Shows or hides the loading indicator.
     *
     * @param show true to show, false to hide
     */
    private void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Shows or hides the empty state message.
     *
     * @param show true to show, false to hide
     */
    private void showEmptyState(boolean show) {
        if (emptyStateView != null) {
            emptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Shows or hides the error state message.
     *
     * @param show true to show, false to hide
     */
    private void showErrorState(boolean show) {
        if (errorStateView != null) {
            errorStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}