package ca.ualberta.codarc.codarc_events.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.OrganizerWithEvents;

/**
 * RecyclerView adapter for displaying organizers in the admin organizer list.
 * Each organizer card includes device ID, last 3 events, and a delete button.
 */
public class AdminOrganizerListAdapter extends RecyclerView.Adapter<AdminOrganizerListAdapter.ViewHolder> {

    private final List<OrganizerWithEvents> organizers;
    private final RemoveClickListener removeClickListener;

    /**
     * Interface for handling remove button clicks.
     */
    public interface RemoveClickListener {
        /**
         * Called when the remove button is clicked for an organizer.
         *
         * @param organizerWithEvents the organizer to remove
         */
        void onRemoveClick(OrganizerWithEvents organizerWithEvents);
    }

    /**
     * Creates an adapter for displaying organizers with remove functionality.
     *
     * @param removeClickListener listener for remove button clicks
     */
    public AdminOrganizerListAdapter(RemoveClickListener removeClickListener) {
        this.organizers = new ArrayList<>();
        this.removeClickListener = removeClickListener;
    }

    public void setItems(List<OrganizerWithEvents> organizers) {
        this.organizers.clear();
        if (organizers != null) {
            this.organizers.addAll(organizers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_organizer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OrganizerWithEvents organizerWithEvents = organizers.get(position);
        
        if (organizerWithEvents == null || organizerWithEvents.getOrganizer() == null) {
            return;
        }
        
        String deviceId = organizerWithEvents.getOrganizer().getDeviceId();
        holder.deviceId.setText(deviceId != null ? deviceId : "Unknown");
        
        List<Event> events = organizerWithEvents.getRecentEvents();
        if (events != null && !events.isEmpty()) {
            holder.eventsContainer.setVisibility(View.VISIBLE);
            holder.eventsContainer.removeAllViews();
            
            for (Event event : events) {
                TextView eventView = new TextView(holder.itemView.getContext());
                String eventName = event.getName();
                if (eventName == null || eventName.trim().isEmpty()) {
                    eventName = "Unnamed Event";
                }
                eventView.setText(eventName);
                eventView.setTextSize(14);
                eventView.setPadding(0, 8, 0, 8);
                holder.eventsContainer.addView(eventView);
            }
        } else {
            holder.eventsContainer.setVisibility(View.GONE);
        }
        
        holder.removeButton.setOnClickListener(v -> {
            if (removeClickListener != null) {
                removeClickListener.onRemoveClick(organizerWithEvents);
            }
        });
    }

    @Override
    public int getItemCount() {
        return organizers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView deviceId;
        final ViewGroup eventsContainer;
        final ImageButton removeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceId = itemView.findViewById(R.id.tv_organizer_device_id);
            eventsContainer = itemView.findViewById(R.id.container_events);
            removeButton = itemView.findViewById(R.id.btn_delete);
        }
    }
}
