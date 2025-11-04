package ca.ualberta.codarc.codarc_events.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.controllers.JoinWaitlistController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import ca.ualberta.codarc.codarc_events.views.EventDetailsActivity;
import ca.ualberta.codarc.codarc_events.views.ProfileCreationActivity;

/**
 * RecyclerView adapter for simple event cards.
 * Displays event info, waitlist count, and provides actions
 * for joining, viewing entrants, and viewing lottery criteria.
 */
public class EventCardAdapter extends RecyclerView.Adapter<EventCardAdapter.ViewHolder> {

    private final Context context;
    private final List<Event> events;
    private final JoinWaitlistController joinWaitlistController;

    /**
     * Creates an adapter for displaying event cards in a RecyclerView.
     *
     * @param context the activity context
     * @param events  the list of events to display
     */
    public EventCardAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
        this.joinWaitlistController = new JoinWaitlistController(new EventDB(), new EntrantDB());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event e = events.get(position);
        String eventId = e.getId();

        holder.title.setText(e.getName() != null ? e.getName() : "");
        holder.date.setText(e.getEventDateTime() != null ? e.getEventDateTime() : "");
        holder.status.setText(e.isOpen() ? "Status: Open" : "Status: Closed");

        // Waitlist count - use one-time fetch to avoid memory leaks
        holder.waitlistCount.setTag(eventId);
        holder.waitlistCount.setText("Waitlist: …");

        joinWaitlistController.getWaitlistCount(eventId, new EventDB.Callback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                Object tag = holder.waitlistCount.getTag();
                if (tag != null && tag.equals(eventId)) {
                    holder.waitlistCount.setText("Waitlist: " + count);
                }
            }

            @Override
            public void onError(@NonNull Exception ex) {
                Object tag = holder.waitlistCount.getTag();
                if (tag != null && tag.equals(eventId)) {
                    holder.waitlistCount.setText("Waitlist: N/A");
                }
            }
        });

        // Join Waitlist - use controller for proper business logic
        holder.joinBtn.setOnClickListener(v -> {
            String deviceId = Identity.getOrCreateDeviceId(v.getContext());
            Event event = events.get(position);
            joinWaitlistController.joinWaitlist(event, deviceId, new JoinWaitlistController.Callback() {
                @Override
                public void onResult(JoinWaitlistController.JoinResult result) {
                    if (result.needsProfileRegistration()) {
                        Intent intent = new Intent(context, ProfileCreationActivity.class);
                        context.startActivity(intent);
                    } else if (result.isSuccess()) {
                        Toast.makeText(context, result.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        // Lottery Info popup
        holder.lotteryInfoBtn.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(context)
                    .inflate(R.layout.dialog_lottery_info, null);

            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(context.getString(R.string.got_it), (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // Event Details
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailsActivity.class);
            intent.putExtra("event", e);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, status, waitlistCount;
        View joinBtn, lotteryInfoBtn;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_event_title);
            date = itemView.findViewById(R.id.tv_lottery_ends);
            status = itemView.findViewById(R.id.tv_entrants_info);
            waitlistCount = itemView.findViewById(R.id.tv_waitlist_count);
            joinBtn = itemView.findViewById(R.id.btn_join_list);
            lotteryInfoBtn = itemView.findViewById(R.id.btn_lottery_info);
        }
    }
}
