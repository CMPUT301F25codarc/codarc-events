package ca.ualberta.codarc.codarc_events.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ca.ualberta.codarc.codarc_events.R;

/**
 * Adapter for displaying winners with response status.
 */
public class WinnersAdapter extends RecyclerView.Adapter<WinnersAdapter.ViewHolder> {

    private final List<WinnerItem> items;
    private final WinnerActionListener actionListener;

    public interface WinnerActionListener {
        void onCancelRequested(String deviceId);
    }

    public WinnersAdapter(List<WinnerItem> items, WinnerActionListener actionListener) {
        this.items = items;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant_winner, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WinnerItem item = items.get(position);
        holder.nameText.setText(item.getName() != null ? item.getName() : "");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        long requestTime = item.getRequestTime();
        String timeStr = (requestTime > 0)
                ? format.format(new Date(requestTime))
                : "Unknown";
        holder.timeText.setText(timeStr);

        String statusText;
        int nameColor;
        Boolean isEnrolled = item.getIsEnrolled();
        if (isEnrolled == null) {
            statusText = "Pending";
            nameColor = Color.BLACK;
        } else if (Boolean.TRUE.equals(isEnrolled)) {
            statusText = "Accepted";
            nameColor = Color.parseColor("#4CAF50");
        } else {
            statusText = "Declined";
            nameColor = Color.parseColor("#F44336");
        }

        holder.statusText.setText(statusText);
        holder.nameText.setTextColor(nameColor);

        boolean isPending = isEnrolled == null;
        holder.cancelButton.setVisibility(isPending ? View.VISIBLE : View.GONE);
        holder.cancelButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onCancelRequested(item.getDeviceId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView timeText;
        TextView statusText;
        MaterialButton cancelButton;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_entrant_name);
            timeText = itemView.findViewById(R.id.tv_request_time);
            statusText = itemView.findViewById(R.id.tv_status);
            cancelButton = itemView.findViewById(R.id.btn_cancel_winner);
        }
    }

    public static class WinnerItem {
        private final String deviceId;
        private final String name;
        private final long requestTime;
        private final Boolean isEnrolled;

        public WinnerItem(String deviceId, String name, long requestTime, @Nullable Boolean isEnrolled) {
            this.deviceId = deviceId;
            this.name = name;
            this.requestTime = requestTime;
            this.isEnrolled = isEnrolled;
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

        @Nullable
        public Boolean getIsEnrolled() {
            return isEnrolled;
        }
    }
}

