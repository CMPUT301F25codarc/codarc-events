package ca.ualberta.codarc.codarc_events.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.utils.DateHelper;
import ca.ualberta.codarc.codarc_events.controllers.JoinWaitlistController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.views.EventDetailsActivity;

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
        holder.date.setText(DateHelper.formatEventDate(e.getEventDateTime()));
        holder.status.setText(e.isOpen() ? context.getString(R.string.status_open) : context.getString(R.string.status_closed));

        // Display tags
        displayTags(holder, e);

        // Fetch and display waitlist count
        fetchAndDisplayWaitlistCount(holder, eventId);

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

    /**
     * Displays tags on the event card.
     *
     * @param holder the ViewHolder containing the tag chip group
     * @param event  the event to display tags for
     */
    private void displayTags(@NonNull ViewHolder holder, Event event) {
        holder.tagChipGroup.removeAllViews();

        if (event.getTags() == null || event.getTags().isEmpty()) {
            holder.tagChipGroup.setVisibility(View.GONE);
            return;
        }

        holder.tagChipGroup.setVisibility(View.VISIBLE);
        List<String> tags = event.getTags();
        int maxTags = Math.min(tags.size(), 4); // Show max 4 tags

        for (int i = 0; i < maxTags; i++) {
            String tag = tags.get(i);
            if (tag != null && !tag.trim().isEmpty()) {
                Chip chip = new Chip(context);
                chip.setText(tag);
                chip.setChipBackgroundColorResource(R.color.chip_background);
                chip.setTextColor(context.getColor(R.color.chip_text));
                chip.setTextSize(10f);
                chip.setClickable(false);
                chip.setFocusable(false);
                holder.tagChipGroup.addView(chip);
            }
        }

        // Show "+X more" indicator if there are more tags
        if (tags.size() > 4) {
            Chip moreChip = new Chip(context);
            moreChip.setText("+" + (tags.size() - 4) + " more");
            moreChip.setChipBackgroundColorResource(R.color.chip_background);
            moreChip.setTextColor(context.getColor(R.color.chip_text));
            moreChip.setTextSize(10f);
            moreChip.setClickable(false);
            moreChip.setFocusable(false);
            holder.tagChipGroup.addView(moreChip);
        }
    }

    /**
     * Fetches and displays the waitlist count for a specific event.
     * Uses tagging to ensure we only update the correct ViewHolder.
     *
     * @param holder  the ViewHolder to update
     * @param eventId the event ID to fetch the count for
     */
    private void fetchAndDisplayWaitlistCount(@NonNull ViewHolder holder, String eventId) {
        holder.waitlistCount.setTag(eventId);
        holder.waitlistCount.setText(context.getString(R.string.waitlist_loading));

        joinWaitlistController.getWaitlistCount(eventId, new EventDB.Callback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                Object tag = holder.waitlistCount.getTag();
                if (tag != null && tag.equals(eventId)) {
                    holder.waitlistCount.setText(context.getString(R.string.waitlist_count, count));
                }
            }

            @Override
            public void onError(@NonNull Exception ex) {
                Object tag = holder.waitlistCount.getTag();
                if (tag != null && tag.equals(eventId)) {
                    holder.waitlistCount.setText(context.getString(R.string.waitlist_unavailable));
                }
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, status, waitlistCount;
        View lotteryInfoBtn;
        ChipGroup tagChipGroup;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_event_title);
            date = itemView.findViewById(R.id.tv_lottery_ends);
            status = itemView.findViewById(R.id.tv_entrants_info);
            waitlistCount = itemView.findViewById(R.id.tv_waitlist_count);
            lotteryInfoBtn = itemView.findViewById(R.id.btn_lottery_info);
            tagChipGroup = itemView.findViewById(R.id.chip_group_tags);
        }
    }
}
