package ca.ualberta.codarc.codarc_events.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.models.NotificationEntry;

/**
 * RecyclerView adapter responsible for binding entrant notifications.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface NotificationActionListener {
        void onAccept(@NonNull NotificationEntry entry);
        void onDecline(@NonNull NotificationEntry entry);
    }

    public interface NotificationClickListener {
        void onNotificationClick(@NonNull NotificationEntry entry);
    }

    private final List<NotificationEntry> items = new ArrayList<>();
    private final NotificationActionListener actionListener;
    private final NotificationClickListener clickListener;
    private final DateFormat dateFormat;

    /**
     * Creates a new NotificationAdapter.
     *
     * @param actionListener listener for accept/decline actions
     * @param clickListener listener for notification item clicks
     */
    public NotificationAdapter(NotificationActionListener actionListener, NotificationClickListener clickListener) {
        this.actionListener = actionListener;
        this.clickListener = clickListener;
        this.dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
    }

    public void setItems(List<NotificationEntry> entries) {
        items.clear();
        if (entries != null) {
            items.addAll(entries);
        }
        notifyDataSetChanged();
    }

    public void updateItem(NotificationEntry entry) {
        if (entry == null) {
            return;
        }
        int index = items.indexOf(entry);
        if (index >= 0) {
            notifyItemChanged(index);
        } else {
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notifications, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationEntry entry = items.get(position);
        holder.bind(entry, actionListener, clickListener, dateFormat);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final View root;
        private final TextView titleView;
        private final TextView messageView;
        private final TextView timestampView;
        private final TextView statusView;
        private final Button acceptButton;
        private final Button declineButton;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.notification_root);
            titleView = itemView.findViewById(R.id.tv_notification_title);
            messageView = itemView.findViewById(R.id.tv_notification_message);
            timestampView = itemView.findViewById(R.id.tv_notification_time);
            statusView = itemView.findViewById(R.id.tv_notification_status);
            acceptButton = itemView.findViewById(R.id.btn_notification_accept);
            declineButton = itemView.findViewById(R.id.btn_notification_decline);
        }

        void bind(NotificationEntry entry,
                  NotificationActionListener actionListener,
                  NotificationClickListener clickListener,
                  DateFormat dateFormat) {
            String eventName = entry.getEventName();
            if (eventName == null || eventName.isEmpty()) {
                eventName = itemView.getContext().getString(R.string.notification_unknown_event);
            }
            titleView.setText(eventName);
            messageView.setText(entry.getMessage());

            if (entry.getCreatedAt() > 0) {
                timestampView.setText(dateFormat.format(new Date(entry.getCreatedAt())));
                timestampView.setVisibility(View.VISIBLE);
            } else {
                timestampView.setVisibility(View.GONE);
            }

            boolean isInvite = "winner".equalsIgnoreCase(entry.getCategory());
            String response = entry.getResponse();
            boolean hasResponded = response != null && !response.isEmpty();

            statusView.setVisibility(hasResponded ? View.VISIBLE : View.GONE);
            if (hasResponded) {
                int statusText;
                if ("accepted".equalsIgnoreCase(response)) {
                    statusText = R.string.notification_status_accepted;
                } else if ("declined".equalsIgnoreCase(response)) {
                    statusText = R.string.notification_status_declined;
                } else {
                    statusText = R.string.notification_status_acknowledged;
                }
                statusView.setText(statusText);
            }

            if (isInvite && !hasResponded) {
                acceptButton.setVisibility(View.VISIBLE);
                declineButton.setVisibility(View.VISIBLE);
            } else {
                acceptButton.setVisibility(View.GONE);
                declineButton.setVisibility(View.GONE);
            }

            boolean isProcessing = entry.isProcessing();
            acceptButton.setEnabled(!isProcessing);
            declineButton.setEnabled(!isProcessing);

            acceptButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onAccept(entry);
                }
            });
            declineButton.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDecline(entry);
                }
            });

            // Set click listener on root view to navigate to event details
            root.setOnClickListener(v -> {
                if (clickListener != null && entry.getEventId() != null && !entry.getEventId().isEmpty()) {
                    clickListener.onNotificationClick(entry);
                }
            });

            int backgroundColorRes = entry.isRead()
                    ? R.color.notification_card_read
                    : R.color.notification_card_unread;
            root.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), backgroundColorRes));
        }
    }
}