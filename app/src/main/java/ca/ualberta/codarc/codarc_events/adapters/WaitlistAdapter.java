package ca.ualberta.codarc.codarc_events.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ca.ualberta.codarc.codarc_events.R;

/**
 * Adapter for displaying waitlist entries in a RecyclerView.
 */
public class WaitlistAdapter extends RecyclerView.Adapter<WaitlistAdapter.ViewHolder> {

    private final List<WaitlistItem> items;

    public WaitlistAdapter(List<WaitlistItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant_waitlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaitlistItem item = items.get(position);
        holder.nameText.setText(item.getName() != null ? item.getName() : "");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        long requestTime = item.getRequestTime();
        String timeStr = (requestTime > 0) 
            ? format.format(new Date(requestTime))
            : "Unknown";
        holder.timeText.setText(timeStr);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView timeText;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_entrant_name);
            timeText = itemView.findViewById(R.id.tv_request_time);
        }
    }

    /**
     * Represents a waitlist entry with entrant info.
     */
    public static class WaitlistItem {
        private final String deviceId;
        private final String name;
        private final long requestTime;

        public WaitlistItem(String deviceId, String name, long requestTime) {
            this.deviceId = deviceId;
            this.name = name;
            this.requestTime = requestTime;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getName() {
            return name;
        }

        public long getRequestTime() {
            return requestTime;
        }
    }
}
