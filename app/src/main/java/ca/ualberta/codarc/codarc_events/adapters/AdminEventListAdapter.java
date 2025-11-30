package ca.ualberta.codarc.codarc_events.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.controllers.EventValidationHelper;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.DateHelper;

/**
 * RecyclerView adapter for displaying events in the admin event list.
 * Each event card includes a delete button for removing events.
 */
public class AdminEventListAdapter extends RecyclerView.Adapter<AdminEventListAdapter.ViewHolder> {

    private final List<Event> events;
    private final DeleteClickListener deleteClickListener;

    /**
     * Interface for handling delete button clicks.
     */
    public interface DeleteClickListener {
        /**
         * Called when the delete button is clicked for an event.
         *
         * @param event the event to delete
         */
        void onDeleteClick(Event event);
    }

    /**
     * Creates an adapter for displaying events with delete functionality.
     *
     * @param deleteClickListener listener for delete button clicks
     */
    public AdminEventListAdapter(DeleteClickListener deleteClickListener) {
        this.events = new ArrayList<>();
        this.deleteClickListener = deleteClickListener;
    }

    public void setItems(List<Event> events) {
        this.events.clear();
        if (events != null) {
            this.events.addAll(events);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);
        
        holder.eventName.setText(event.getName() != null ? event.getName() : "");
        holder.eventDate.setText(DateHelper.formatEventDate(event.getEventDateTime()));
        
        if (event.getOrganizerId() != null && !event.getOrganizerId().isEmpty()) {
            holder.organizerId.setText("Organizer: " + event.getOrganizerId());
            holder.organizerId.setVisibility(View.VISIBLE);
        } else {
            holder.organizerId.setVisibility(View.GONE);
        }
        
        String registrationStatus = getRegistrationStatus(event);
        holder.registrationStatus.setText(registrationStatus);
        holder.registrationStatus.setVisibility(View.VISIBLE);
        
        holder.deleteButton.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Determines the registration status of an event.
     *
     * @param event the event to check
     * @return status string: "Open", "Closed", or "Not Started"
     */
    private String getRegistrationStatus(Event event) {
        if (event == null) {
            return "Unknown";
        }
        
        if (EventValidationHelper.isWithinRegistrationWindow(event)) {
            return "Open";
        }
        
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            long now = System.currentTimeMillis();
            String regOpen = event.getRegistrationOpen();
            
            if (regOpen != null && !regOpen.isEmpty()) {
                long openTime = isoFormat.parse(regOpen).getTime();
                if (now < openTime) {
                    return "Not Started";
                }
            }
        } catch (Exception e) {
            android.util.Log.w("AdminEventListAdapter", "Error parsing registration time", e);
        }
        
        return "Closed";
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView eventName;
        final TextView eventDate;
        final TextView organizerId;
        final TextView registrationStatus;
        final ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.tv_event_name);
            eventDate = itemView.findViewById(R.id.tv_event_date);
            organizerId = itemView.findViewById(R.id.tv_organizer_id);
            registrationStatus = itemView.findViewById(R.id.tv_registration_status);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
    }
}

