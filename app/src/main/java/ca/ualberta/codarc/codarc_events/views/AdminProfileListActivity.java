package ca.ualberta.codarc.codarc_events.views;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.AdminProfileListAdapter;
import ca.ualberta.codarc.codarc_events.controllers.AdminRemoveProfileController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;

/**
 * Displays a list of all entrant profiles for administrators to browse and manage.
 * Provides view functionality with remove option for each profile.
 */
public class AdminProfileListActivity extends BaseAdminListActivity {

    private AdminProfileListAdapter adapter;
    private EntrantDB entrantDB;
    private AdminRemoveProfileController removeController;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_admin_profile_list;
    }

    @Override
    protected int getRecyclerViewId() {
        return R.id.rv_admin_profiles;
    }

    @Override
    protected int getLoadingViewId() {
        return R.id.pb_loading;
    }

    @Override
    protected int getEmptyStateId() {
        return R.id.tv_empty_state;
    }

    @Override
    protected int getErrorStateId() {
        return R.id.tv_error_state;
    }

    @Override
    protected void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminProfileListAdapter(entrant -> handleRemoveClick(entrant));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void initializeActivity() {
        entrantDB = new EntrantDB();
        removeController = new AdminRemoveProfileController();
    }

    @Override
    protected void loadData() {
        loadProfiles();
    }

    private void loadProfiles() {
        resetUIStates();

        entrantDB.getAllEntrants(new EntrantDB.Callback<List<Entrant>>() {
            @Override
            public void onSuccess(List<Entrant> profiles) {
                runOnUiThread(() -> {
                    if (profiles == null || profiles.isEmpty()) {
                        adapter.setItems(null);
                        handleLoadSuccess(false);
                    } else {
                        List<Entrant> nonBannedProfiles = filterBannedProfiles(profiles);
                        if (nonBannedProfiles.isEmpty()) {
                            adapter.setItems(null);
                            handleLoadSuccess(false);
                        } else {
                            adapter.setItems(nonBannedProfiles);
                            handleLoadSuccess(true);
                        }
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                handleLoadError(e, "AdminProfileListActivity");
            }
        });
    }

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

    private void removeProfile(String entrantDeviceId) {
        showLoading(true);
        removeController.removeProfile(entrantDeviceId, deviceId, new AdminRemoveProfileController.Callback() {
            @Override
            public void onResult(AdminRemoveProfileController.RemoveProfileResult result) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (result.isSuccess()) {
                        Toast.makeText(AdminProfileListActivity.this,
                                R.string.admin_remove_profile_success, Toast.LENGTH_SHORT).show();
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

}

