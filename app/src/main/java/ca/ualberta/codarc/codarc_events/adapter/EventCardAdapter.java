package ca.ualberta.codarc.codarc_events.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.model.EventCard;

public class EventCardAdapter extends RecyclerView.Adapter<EventCardAdapter.EventCardViewHolder> {
    
    private List<EventCard> eventCards;
    private OnEventCardClickListener listener;

    public interface OnEventCardClickListener {
        void onViewEntrantsClick(EventCard eventCard);
        void onJoinListClick(EventCard eventCard);
        void onLeaveListClick(EventCard eventCard);
    }

    public EventCardAdapter(List<EventCard> eventCards, OnEventCardClickListener clickListener) {
        this.eventCards = eventCards;
        this.listener = clickListener;
    }

    @NonNull
    @Override
    public EventCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_card, parent, false);
        return new EventCardViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EventCardViewHolder holder, int position) {
        EventCard eventCard = eventCards.get(position);
        holder.bind(eventCard);
    }

    @Override
    public int getItemCount() {
        return eventCards.size();
    }

    public void updateEventCards(List<EventCard> newEventCards) {
        this.eventCards.clear();
        this.eventCards.addAll(newEventCards);
        notifyDataSetChanged();
    }

    static class EventCardViewHolder extends RecyclerView.ViewHolder {
        private TextView tvEventTitle;
        private TextView tvEventLocation;
        private TextView tvLotteryEnds;
        private TextView tvEntrantsInfo;
        private LinearLayout btnViewEntrants;
        private LinearLayout btnJoinList;
        private LinearLayout btnLeaveList;

        public EventCardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTitle = itemView.findViewById(R.id.tv_event_title);
            tvEventLocation = itemView.findViewById(R.id.tv_event_location);
            tvLotteryEnds = itemView.findViewById(R.id.tv_lottery_ends);
            tvEntrantsInfo = itemView.findViewById(R.id.tv_entrants_info);
            btnViewEntrants = itemView.findViewById(R.id.btn_view_entrants);
            btnJoinList = itemView.findViewById(R.id.btn_join_list);
            btnLeaveList = itemView.findViewById(R.id.btn_leave_list);
        }

        public void bind(EventCard eventCard) {
            tvEventTitle.setText(eventCard.getTitle());
            tvEventLocation.setText(eventCard.getLocation());
            tvLotteryEnds.setText(eventCard.getLotteryEnds());
            tvEntrantsInfo.setText(eventCard.getEntrantsInfo());

            btnViewEntrants.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewEntrantsClick(eventCard);
                }
            });

            btnJoinList.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onJoinListClick(eventCard);
                }
            });

            btnLeaveList.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLeaveListClick(eventCard);
                }
            });
        }
    }
}
