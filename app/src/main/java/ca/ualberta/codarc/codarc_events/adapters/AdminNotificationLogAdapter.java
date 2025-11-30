package ca.ualberta.codarc.codarc_events.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;

/**
 * RecyclerView adapter for displaying notification logs in the admin notification log list.
 * Read-only view showing timestamp, recipient, event name, and message preview.
 */
public class AdminNotificationLogAdapter extends RecyclerView.Adapter<AdminNotificationLogAdapter.ViewHolder> {

    private final List<Map<String, Object>> logs;
    private static final int MAX_MESSAGE_PREVIEW_LENGTH = 50;

    public AdminNotificationLogAdapter() {
        this.logs = new ArrayList<>();
    }

    public void setItems(List<Map<String, Object>> logs) {
        this.logs.clear();
        if (logs != null) {
            this.logs.addAll(logs);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_notification_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> log = logs.get(position);
        if (log == null) {
            return;
        }

        Object timestampObj = log.get("createdAt");
        long timestamp = parseTimestamp(timestampObj);
        if (timestamp > 0) {
            holder.timestamp.setText(formatTimestamp(timestamp));
        } else {
            holder.timestamp.setText("Unknown time");
        }

        Object recipientObj = log.get("entrantDeviceId");
        String recipient = recipientObj != null ? recipientObj.toString() : "Unknown";
        holder.recipient.setText("Recipient: " + recipient);

        Object eventNameObj = log.get("eventName");
        String eventName = eventNameObj != null ? eventNameObj.toString() : "Unknown Event";
        holder.eventName.setText(eventName);

        Object messageObj = log.get("message");
        String message = messageObj != null ? messageObj.toString() : "";
        if (message.length() > MAX_MESSAGE_PREVIEW_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_PREVIEW_LENGTH) + "...";
        }
        holder.message.setText(message);

        Object categoryObj = log.get("category");
        String category = categoryObj != null ? categoryObj.toString() : "";
        if (category != null && !category.isEmpty()) {
            holder.category.setText(formatCategory(category));
            holder.category.setVisibility(View.VISIBLE);
        } else {
            holder.category.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    private long parseTimestamp(Object timestampObj) {
        if (timestampObj == null) {
            return 0L;
        }

        if (timestampObj instanceof Timestamp) {
            Timestamp ts = (Timestamp) timestampObj;
            return ts.toDate().getTime();
        }

        if (timestampObj instanceof Long) {
            return (Long) timestampObj;
        }

        if (timestampObj instanceof Number) {
            return ((Number) timestampObj).longValue();
        }

        if (timestampObj instanceof Date) {
            return ((Date) timestampObj).getTime();
        }

        return 0L;
    }

    private String formatTimestamp(long timestamp) {
        try {
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
            return format.format(new Date(timestamp));
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }

    private String formatCategory(String category) {
        if (category == null || category.isEmpty()) {
            return "Notification";
        }
        return category.replace("_", " ").toLowerCase();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView timestamp;
        final TextView recipient;
        final TextView eventName;
        final TextView message;
        final TextView category;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            timestamp = itemView.findViewById(R.id.tv_timestamp);
            recipient = itemView.findViewById(R.id.tv_recipient);
            eventName = itemView.findViewById(R.id.tv_event_name);
            message = itemView.findViewById(R.id.tv_message);
            category = itemView.findViewById(R.id.tv_category);
        }
    }
}
