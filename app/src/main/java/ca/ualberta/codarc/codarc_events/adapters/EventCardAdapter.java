package ca.ualberta.codarc.codarc_events.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.models.Event;

public class EventCardAdapter extends RecyclerView.Adapter<EventCardAdapter.ViewHolder> {

    private final Context context;
    private final List<Event> events;

    public EventCardAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;
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
        holder.title.setText(e.getName());
        holder.date.setText("Date: " + e.getDate());
        holder.status.setText(e.getIsOpen() ? "Status: Open" : "Status: Closed");
    }

    @Override
    public int getItemCount() { return events.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date, status;
        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_event_title);
            date = itemView.findViewById(R.id.tv_lottery_ends);
            status = itemView.findViewById(R.id.tv_entrants_info);
        }
    }
}
