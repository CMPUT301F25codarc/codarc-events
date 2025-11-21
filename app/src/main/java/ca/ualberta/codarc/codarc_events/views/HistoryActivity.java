package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.adapters.HistoryAdapter;
import ca.ualberta.codarc.codarc_events.controllers.HistoryController;
import ca.ualberta.codarc.codarc_events.models.HistoryItem;
import ca.ualberta.codarc.codarc_events.utils.Identity;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private HistoryController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        rvHistory = findViewById(R.id.rv_history);
        progressBar = findViewById(R.id.progress_history);
        tvEmpty = findViewById(R.id.tv_empty_history);

        controller = new HistoryController();

        String deviceId = Identity.getOrCreateDeviceId(this);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        controller.getEntrantHistory(deviceId, new HistoryController.HistoryCallback() {
            @Override
            public void onHistoryLoaded(ArrayList<HistoryItem> historyList) {

                progressBar.setVisibility(View.GONE);

                if (historyList.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    rvHistory.setAdapter(new HistoryAdapter(historyList));
                }
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("Error: " + message);
            }
        });
    }
}
