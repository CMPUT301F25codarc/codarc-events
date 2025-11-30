package ca.ualberta.codarc.codarc_events.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.models.RegistrationHistoryEntry;
import ca.ualberta.codarc.codarc_events.utils.DateHelper;

/**
 * RecyclerView adapter for displaying registration history entries.
 */
public class RegistrationHistoryAdapter extends RecyclerView.Adapter<RegistrationHistoryAdapter.ViewHolder> {

    private final List<RegistrationHistoryEntry> items = new ArrayList<>();

    public void setItems(List<RegistrationHistoryEntry> entries) {
        items.clear();
        if (entries != null) {
            items.addAll(entries);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_registration_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RegistrationHistoryEntry entry = items.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView eventNameView;
        private final TextView eventDateView;
        private final Chip statusChip;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventNameView = itemView.findViewById(R.id.tv_event_name);
            eventDateView = itemView.findViewById(R.id.tv_event_date);
            statusChip = itemView.findViewById(R.id.chip_selection_status);
        }

        void bind(RegistrationHistoryEntry entry) {
            if (entry == null) {
                return;
            }

            String eventName = entry.getEventName();
            if (eventName == null || eventName.isEmpty()) {
                eventName = itemView.getContext().getString(R.string.notification_unknown_event);
            }
            eventNameView.setText(eventName);

            String formattedDate = DateHelper.formatEventDate(entry.getEventDate());
            eventDateView.setText(formattedDate);

            String status = entry.getSelectionStatus();
            if (status == null || status.isEmpty()) {
                status = itemView.getContext().getString(R.string.registration_history_waitlisted);
            }
            statusChip.setText(status);

            int chipColorRes;
            int textColorRes;
            if ("Accepted".equals(status)) {
                chipColorRes = R.color.chip_background;
                textColorRes = android.R.color.holo_green_dark;
            } else if ("Cancelled".equals(status)) {
                chipColorRes = R.color.chip_background;
                textColorRes = android.R.color.darker_gray;
            } else if ("Invited".equals(status)) {
                chipColorRes = R.color.chip_background;
                textColorRes = android.R.color.holo_blue_dark;
            } else if ("Waitlisted".equals(status)) {
                chipColorRes = R.color.chip_background;
                textColorRes = android.R.color.holo_orange_dark;
            } else {
                chipColorRes = R.color.chip_background;
                textColorRes = android.R.color.darker_gray;
            }

            statusChip.setChipBackgroundColorResource(chipColorRes);
            statusChip.setTextColor(ContextCompat.getColor(itemView.getContext(), textColorRes));
        }
    }
}

