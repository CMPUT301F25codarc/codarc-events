package ca.ualberta.codarc.codarc_events.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.DateHelper;

/**
 * RecyclerView adapter for displaying events with images in the admin image list.
 * Each event card shows the poster image, event name, date, and a delete button.
 */
public class AdminImageListAdapter extends RecyclerView.Adapter<AdminImageListAdapter.ViewHolder> {

    private final List<Event> events;
    private final DeleteClickListener deleteClickListener;

    /**
     * Interface for handling delete button clicks.
     */
    public interface DeleteClickListener {
        /**
         * Called when the delete button is clicked for an event.
         *
         * @param event the event whose image should be deleted
         */
        void onDeleteClick(Event event);
    }

    /**
     * Creates an adapter for displaying events with images and delete functionality.
     *
     * @param deleteClickListener listener for delete button clicks
     */
    public AdminImageListAdapter(DeleteClickListener deleteClickListener) {
        this.events = new ArrayList<>();
        this.deleteClickListener = deleteClickListener;
    }

    /**
     * Updates the list of events to display.
     * Filters to only show events with non-null, non-empty posterUrl.
     *
     * @param events the new list of events (will be filtered)
     */
    public void setItems(List<Event> events) {
        this.events.clear();
        if (events != null) {
            for (Event event : events) {
                String posterUrl = event.getPosterUrl();
                if (posterUrl != null && !posterUrl.trim().isEmpty()) {
                    this.events.add(event);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Event event = events.get(position);

        holder.eventName.setText(event.getName() != null ? event.getName() : "");
        holder.eventDate.setText(DateHelper.formatEventDate(event.getEventDateTime()));

        // Load poster image using Glide
        String posterUrl = event.getPosterUrl();
        if (posterUrl != null && !posterUrl.trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(posterUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.sample_event_banner)
                    .error(R.drawable.sample_event_banner)
                    .centerCrop()
                    .into(holder.posterImage);
        } else {
            holder.posterImage.setImageResource(R.drawable.sample_event_banner);
        }

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
     * ViewHolder for event image items in the RecyclerView.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView posterImage;
        final TextView eventName;
        final TextView eventDate;
        final ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImage = itemView.findViewById(R.id.iv_poster);
            eventName = itemView.findViewById(R.id.tv_event_name);
            eventDate = itemView.findViewById(R.id.tv_event_date);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
    }
}