package ca.ualberta.codarc.codarc_events.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.model.OrganizerEventCard;

public class OrganizerEventCardAdapter extends RecyclerView.Adapter<OrganizerEventCardAdapter.OrganizerEventCardViewHolder> {
    
    private List<OrganizerEventCard> eventCards;
    private OnOrganizerEventCardClickListener listener;

    public interface OnOrganizerEventCardClickListener {
        void onViewEntrantsClick(OrganizerEventCard eventCard);
        void onRunLotteryClick(OrganizerEventCard eventCard);
        void onNotifyClick(OrganizerEventCard eventCard);
        void onEventMenuClick(OrganizerEventCard eventCard);
    }

    public OrganizerEventCardAdapter(List<OrganizerEventCard> eventCards, OnOrganizerEventCardClickListener listener) {
        this.eventCards = eventCards;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrganizerEventCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_organizer_event_card, parent, false);
        return new OrganizerEventCardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrganizerEventCardViewHolder holder, int position) {
        OrganizerEventCard eventCard = eventCards.get(position);
        holder.bind(eventCard);
    }

    @Override
    public int getItemCount() {
        return eventCards.size();
    }

    public void updateEventCards(List<OrganizerEventCard> newEventCards) {
        this.eventCards = newEventCards;
        notifyDataSetChanged();
    }

    class OrganizerEventCardViewHolder extends RecyclerView.ViewHolder {
        private TextView tvEventTitle;
        private TextView tvEventDates;
        private TextView tvRegistrationStatus;
        private TextView tvRegistrationCloses;
        private ImageButton btnEventMenu;
        private LinearLayout btnViewEntrants;
        private LinearLayout btnRunLottery;
        private LinearLayout btnNotify;

        public OrganizerEventCardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tv_event_title);
            tvEventDates = itemView.findViewById(R.id.tv_event_dates);
            tvRegistrationStatus = itemView.findViewById(R.id.tv_registration_status);
            tvRegistrationCloses = itemView.findViewById(R.id.tv_registration_closes);
            btnEventMenu = itemView.findViewById(R.id.btn_event_menu);
            btnViewEntrants = itemView.findViewById(R.id.btn_view_entrants);
            btnRunLottery = itemView.findViewById(R.id.btn_run_lottery);
            btnNotify = itemView.findViewById(R.id.btn_notify);
        }

        public void bind(OrganizerEventCard eventCard) {
            tvEventTitle.setText(eventCard.getTitle());
            tvEventDates.setText(eventCard.getDates());
            tvRegistrationStatus.setText(eventCard.getRegistrationStatus());
            tvRegistrationCloses.setText(eventCard.getRegistrationCloses());

            btnEventMenu.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventMenuClick(eventCard);
                }
            });

            btnViewEntrants.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewEntrantsClick(eventCard);
                }
            });

            btnRunLottery.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRunLotteryClick(eventCard);
                }
            });

            btnNotify.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotifyClick(eventCard);
                }
            });
        }
    }
}
