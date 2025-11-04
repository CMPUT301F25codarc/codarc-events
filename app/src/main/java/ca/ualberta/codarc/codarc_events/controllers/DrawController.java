package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EventDB;

/**
 * Controller for running the lottery draw.
 */
public class DrawController {

    public interface DrawCallback {
        void onSuccess(List<String> winnerIds);
        void onError(@NonNull Exception e);
    }

    public interface CountCallback {
        void onSuccess(int count);
        void onError(@NonNull Exception e);
    }

    private final EventDB eventDB;

    public DrawController(EventDB eventDB) {
        this.eventDB = eventDB;
    }

    public void loadEntrantCount(String eventId, CountCallback cb) {
        eventDB.getWaitlistCount(eventId, new EventDB.Callback<Integer>() {
            @Override
            public void onSuccess(Integer value) {
                cb.onSuccess(value);
            }

            @Override
            public void onError(@NonNull Exception e) {
                cb.onError(e);
            }
        });
    }

    /**
     * Runs the lottery draw.
     */
    public void runDraw(String eventId, int numWinners, DrawCallback cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        if (numWinners <= 0) {
            cb.onError(new IllegalArgumentException("Number of winners must be > 0"));
            return;
        }

        eventDB.getWaitlist(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> waitlist) {
                if (waitlist == null || waitlist.isEmpty()) {
                    cb.onError(new RuntimeException("No entrants found"));
                    return;
                }

                Collections.shuffle(waitlist);

                int total = waitlist.size();
                int winnerCount = Math.min(numWinners, total);

                List<String> winners = new ArrayList<>(winnerCount);

                for (int i = 0; i < winnerCount; i++) {
                    Object id = waitlist.get(i).get("deviceId");
                    if (id != null) winners.add(id.toString());
                }

                eventDB.markWinners(eventId, winners, new EventDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void ignore) {
                        cb.onSuccess(winners);
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        cb.onError(e);
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                cb.onError(e);
            }
        });
    }
}

