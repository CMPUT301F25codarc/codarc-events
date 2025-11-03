package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.data.EventDB;

/**
 * Activity that displays the list of entrants currently on the waitlist
 * for a specific event. Fetches data from Firestore and shows it in a RecyclerView.
 */

public class ViewEntrantsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SimpleEntrantsAdapter adapter;
    private EventDB eventDB;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_entrants_activity);

        recyclerView = findViewById(R.id.rv_entrants);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        eventDB = new EventDB();
        eventId = getIntent().getStringExtra("eventId");

        adapter = new SimpleEntrantsAdapter();
        recyclerView.setAdapter(adapter);

        loadEntrants();

    }


    /**
     * Fetches the waitlist for the current event from Firestore
     * and updates the RecyclerView adapter.
     */
    private void loadEntrants() {
        eventDB.getWaitlist(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> waitlist) {
                adapter.updateList(waitlist);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(ViewEntrantsActivity.this, "Failed to load entrants", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Simple RecyclerView adapter to display entrants in the waitlist.
     * Shows each entrant's device ID and the time they requested to join.
     */
    static class SimpleEntrantsAdapter extends RecyclerView.Adapter<SimpleEntrantsAdapter.ViewHolder> {

        private List<Map<String, Object>> entrants;

        void updateList(List<Map<String, Object>> newList) {
            entrants = newList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(16, 16, 16, 16);
            tv.setTextSize(16);
            return new ViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> entry = entrants.get(position);
            String deviceId = (String) entry.get("deviceId");
            Object requestTime = entry.get("requestTime");
            holder.textView.setText("Device: " + deviceId + "\nRequested at: " + requestTime);
        }

        @Override
        public int getItemCount() {
            return entrants != null ? entrants.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(@NonNull TextView itemView) {
                super(itemView);
                textView = itemView;
            }
        }
    }
}
