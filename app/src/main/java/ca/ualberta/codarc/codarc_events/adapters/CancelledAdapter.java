package ca.ualberta.codarc.codarc_events.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ca.ualberta.codarc.codarc_events.R;

/**
 * Adapter for displaying cancelled entrants with replacement button.
 */
public class CancelledAdapter extends RecyclerView.Adapter<CancelledAdapter.ViewHolder> {

    private final List<WaitlistAdapter.WaitlistItem> items;
    private final OnReplaceClickListener replaceListener;

    public interface OnReplaceClickListener {
        void onReplaceClick(String deviceId);
    }

    public CancelledAdapter(List<WaitlistAdapter.WaitlistItem> items, OnReplaceClickListener replaceListener) {
        this.items = items;
        this.replaceListener = replaceListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_entrant_cancelled, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaitlistAdapter.WaitlistItem item = items.get(position);
        holder.nameText.setText(item.getName() != null ? item.getName() : "");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        long requestTime = item.getRequestTime();
        String timeStr = (requestTime > 0)
                ? format.format(new Date(requestTime))
                : "Unknown";
        holder.timeText.setText(timeStr);

        holder.replaceBtn.setOnClickListener(v -> {
            if (replaceListener != null) {
                replaceListener.onReplaceClick(item.getDeviceId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView timeText;
        MaterialButton replaceBtn;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.tv_entrant_name);
            timeText = itemView.findViewById(R.id.tv_request_time);
            replaceBtn = itemView.findViewById(R.id.btn_replace);
        }
    }
}

