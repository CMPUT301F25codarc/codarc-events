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
import ca.ualberta.codarc.codarc_events.models.Entrant;

/**
 * RecyclerView adapter for displaying entrant profiles in the admin profile list.
 * Each profile card includes a remove button for deleting profiles.
 */
public class AdminProfileListAdapter extends RecyclerView.Adapter<AdminProfileListAdapter.ViewHolder> {

    private final List<Entrant> profiles;
    private final RemoveClickListener removeClickListener;

    /**
     * Interface for handling remove button clicks.
     */
    public interface RemoveClickListener {
        /**
         * Called when the remove button is clicked for a profile.
         *
         * @param entrant the entrant profile to remove
         */
        void onRemoveClick(Entrant entrant);
    }

    /**
     * Creates an adapter for displaying profiles with remove functionality.
     *
     * @param removeClickListener listener for remove button clicks
     */
    public AdminProfileListAdapter(RemoveClickListener removeClickListener) {
        this.profiles = new ArrayList<>();
        this.removeClickListener = removeClickListener;
    }

    public void setItems(List<Entrant> profiles) {
        this.profiles.clear();
        if (profiles != null) {
            this.profiles.addAll(profiles);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_profile, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entrant entrant = profiles.get(position);
        
        if (entrant == null) {
            return;
        }
        
        String displayName = entrant.getName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = entrant.getDeviceId() != null ? entrant.getDeviceId() : "Unknown";
        }
        holder.profileName.setText(displayName);
        
        // Display email
        String email = entrant.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            holder.email.setText(email);
            holder.email.setVisibility(View.VISIBLE);
        } else {
            holder.email.setVisibility(View.GONE);
        }
        
        String phone = entrant.getPhone();
        if (phone != null && !phone.trim().isEmpty()) {
            holder.phone.setText(phone);
            holder.phone.setVisibility(View.VISIBLE);
        } else {
            holder.phone.setVisibility(View.GONE);
        }
        
        if (entrant.getIsRegistered()) {
            holder.registrationStatus.setText(R.string.profile_registered);
            holder.registrationStatus.setVisibility(View.VISIBLE);
        } else {
            holder.registrationStatus.setText(R.string.profile_not_registered);
            holder.registrationStatus.setVisibility(View.VISIBLE);
        }
        
        if (entrant.isBanned()) {
            holder.bannedStatus.setText(R.string.profile_banned);
            holder.bannedStatus.setVisibility(View.VISIBLE);
        } else {
            holder.bannedStatus.setVisibility(View.GONE);
        }
        
        holder.removeButton.setOnClickListener(v -> {
            if (removeClickListener != null) {
                removeClickListener.onRemoveClick(entrant);
            }
        });
    }

    @Override
    public int getItemCount() {
        return profiles.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView profileName;
        final TextView email;
        final TextView phone;
        final TextView registrationStatus;
        final TextView bannedStatus;
        final ImageButton removeButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            profileName = itemView.findViewById(R.id.tv_profile_name);
            email = itemView.findViewById(R.id.tv_email);
            phone = itemView.findViewById(R.id.tv_phone);
            registrationStatus = itemView.findViewById(R.id.tv_registration_status);
            bannedStatus = itemView.findViewById(R.id.tv_banned_status);
            removeButton = itemView.findViewById(R.id.btn_remove);
        }
    }
}

