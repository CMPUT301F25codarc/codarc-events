package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.utils.Identity;

/**
 * Base class for admin list activities.
 * Provides common functionality for loading, displaying, and managing admin lists.
 */
public abstract class BaseAdminListActivity extends AppCompatActivity {

    protected RecyclerView recyclerView;
    protected ProgressBar loadingView;
    protected TextView emptyStateView;
    protected TextView errorStateView;
    protected String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());

        deviceId = Identity.getOrCreateDeviceId(this);

        recyclerView = findViewById(getRecyclerViewId());
        loadingView = findViewById(getLoadingViewId());
        emptyStateView = findViewById(getEmptyStateId());
        errorStateView = findViewById(getErrorStateId());

        setupBackButton();
        setupRecyclerView();
        setupErrorStateClickListener();

        initializeActivity();
        loadData();
    }

    /**
     * Returns the layout resource ID for this activity.
     */
    protected abstract int getLayoutResourceId();

    /**
     * Returns the RecyclerView resource ID.
     */
    protected abstract int getRecyclerViewId();

    /**
     * Returns the loading ProgressBar resource ID.
     */
    protected abstract int getLoadingViewId();

    /**
     * Returns the empty state TextView resource ID.
     */
    protected abstract int getEmptyStateId();

    /**
     * Returns the error state TextView resource ID.
     */
    protected abstract int getErrorStateId();

    /**
     * Sets up the RecyclerView with layout manager and adapter.
     */
    protected abstract void setupRecyclerView();

    /**
     * Called after basic setup is complete, before loadData().
     * Override to perform activity-specific initialization.
     */
    protected void initializeActivity() {
        // Default: no additional initialization
    }

    /**
     * Loads the data for this activity.
     */
    protected abstract void loadData();

    /**
     * Sets up the back button click listener.
     */
    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.btn_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    /**
     * Sets up the error state click listener to retry loading.
     */
    private void setupErrorStateClickListener() {
        if (errorStateView != null) {
            errorStateView.setOnClickListener(v -> loadData());
        }
    }

    /**
     * Shows or hides the loading indicator.
     *
     * @param show true to show, false to hide
     */
    protected void showLoading(boolean show) {
        if (loadingView != null) {
            loadingView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Shows or hides the empty state message.
     *
     * @param show true to show, false to hide
     */
    protected void showEmptyState(boolean show) {
        if (emptyStateView != null) {
            emptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Shows or hides the error state message.
     *
     * @param show true to show, false to hide
     */
    protected void showErrorState(boolean show) {
        if (errorStateView != null) {
            errorStateView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Resets all UI states before loading data.
     */
    protected void resetUIStates() {
        showLoading(true);
        showEmptyState(false);
        showErrorState(false);
    }

    /**
     * Handles successful data load.
     *
     * @param hasItems whether there are items to display after filtering
     */
    protected void handleLoadSuccess(boolean hasItems) {
        showLoading(false);
        if (hasItems) {
            showEmptyState(false);
        } else {
            showEmptyState(true);
        }
    }

    /**
     * Handles data load error.
     */
    protected void handleLoadError(@NonNull Exception e, String tag) {
        android.util.Log.e(tag, "Failed to load data", e);
        runOnUiThread(() -> {
            showLoading(false);
            showErrorState(true);
            showEmptyState(false);
        });
    }
}
