package ca.ualberta.codarc.codarc_events.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.utils.DateTimeUtils;

/**
 * Adapter for waitlist entries.
 * Shows name and request time.
 */
public class WaitlistAdapter extends RecyclerView.Adapter<WaitlistAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> entries;

    /**
     * Creates adapter for waitlist entries.
     *
     * @param context activity context
     * @param entries list of entries with deviceId, name, and requestTime
     */
    public WaitlistAdapter(Context context, List<Map<String, Object>> entries) {
        this.context = context;
        this.entries = entries;
    }

    /**
     * Creates ViewHolder for waitlist entry.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_entrant_waitlist, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds entry data to ViewHolder.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> entry = entries.get(position);
        String name = (String) entry.get("name");
        Object requestTime = entry.get("requestTime");

        holder.nameText.setText(name != null ? name : "Unknown");

        String formattedTime = DateTimeUtils.formatTimestamp(requestTime);
        holder.timeText.setText(formattedTime);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView timeText;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_entrant_name);
            timeText = itemView.findViewById(R.id.tv_request_time);
        }
    }
}

