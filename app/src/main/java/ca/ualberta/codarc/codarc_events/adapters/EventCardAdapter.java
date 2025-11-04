package ca.ualberta.codarc.codarc_events.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.views.ProfileCreationActivity;
import ca.ualberta.codarc.codarc_events.views.EventDetailsActivity;

/**
 * RecyclerView adapter for simple event cards.
 * The adapter binds basic text fields and wires the Join action to the
 * profile gate (create profile if needed, otherwise go to details).
 */
public class EventCardAdapter extends RecyclerView.Adapter<EventCardAdapter.ViewHolder> {

    private final Context context;
    private final List<Event> events;

    /**
     * Creates an adapter for displaying event cards in a RecyclerView.
     *
     * @param context the activity context
     * @param events the list of events to display
     */
    public EventCardAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
    }

    /**
     * Creates a new ViewHolder for an event card.
     *
     * @param parent the ViewGroup into which the new View will be added
     * @param viewType the view type of the new View
     * @return a new ViewHolder that holds a View for an event card
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds event data to a ViewHolder and sets up click listeners.
     * Join button checks profile registration status; card click navigates to details.
     *
     * @param holder the ViewHolder to bind data to
     * @param position the position of the event in the list
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event e = events.get(position);
        holder.title.setText(e.getName() != null ? e.getName() : "");
        String eventDateTime = e.getEventDateTime();
        holder.date.setText(eventDateTime != null ? eventDateTime.toString() : "");
        holder.status.setText(e.isOpen() ? "Status: Open" : "Status: Closed");

        holder.joinBtn.setOnClickListener(v -> {
            String deviceId = Identity.getOrCreateDeviceId(v.getContext());
            EntrantDB entrantDB = new EntrantDB();
            entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
                @Override
                public void onSuccess(Entrant entrant) {
                    boolean isRegistered = (entrant != null && entrant.getIsRegistered());
                    if (!isRegistered) {
                        Intent intent = new Intent(context, ProfileCreationActivity.class);
                        context.startActivity(intent);
                    } else {
                        // For now, show Toast message as placeholder before implementation
                        Toast.makeText(context, "Joined waitlist (placeholder)", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(@NonNull Exception e1) {
                    Toast.makeText(context, "Failed to check profile", Toast.LENGTH_SHORT).show();
                }
            });
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailsActivity.class);
            intent.putExtra("event", events.get(position));
            context.startActivity(intent);
        });
    }

    /**
     * Returns the total number of events in the adapter's data set.
     *
     * @return the number of events
     */
    @Override
    public int getItemCount() { return events.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, status;
        View joinBtn;

        /**
         * Initializes ViewHolder with references to card views.
         *
         * @param itemView the root view of the event card layout
         */
        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_event_title);
            date = itemView.findViewById(R.id.tv_lottery_ends);
            status = itemView.findViewById(R.id.tv_entrants_info);
            joinBtn = itemView.findViewById(R.id.btn_join_list);
        }
    }
}