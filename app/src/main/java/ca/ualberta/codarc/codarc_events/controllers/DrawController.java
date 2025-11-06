package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EventDB;

/**
 * Handles lottery draw - selects winners and replacement pool.
 */
public class DrawController {

    public interface DrawCallback {
        void onSuccess(List<String> winnerIds, List<String> replacementIds);
        void onError(@NonNull Exception e);
    }

    public interface CountCallback {
        void onSuccess(int count);
        void onError(@NonNull Exception e);
    }

    private final EventDB eventDB;
    private static final int DEFAULT_REPLACEMENT_POOL_SIZE = 3;

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

    // Runs lottery with default 3 replacements
    public void runDraw(String eventId, int numWinners, DrawCallback cb) {
        runDraw(eventId, numWinners, DEFAULT_REPLACEMENT_POOL_SIZE, cb);
    }
    
    // Runs lottery with custom replacement pool size
    public void runDraw(String eventId, int numWinners, int replacementPoolSize, DrawCallback cb) {
        if (eventId == null || eventId.isEmpty()) {
            cb.onError(new IllegalArgumentException("eventId is empty"));
            return;
        }
        if (numWinners <= 0) {
            cb.onError(new IllegalArgumentException("Number of winners must be > 0"));
            return;
        }
        if (replacementPoolSize < 0) {
            cb.onError(new IllegalArgumentException("Replacement pool size cannot be negative"));
            return;
        }

        eventDB.getWaitlist(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> waitlist) {
                if (waitlist == null || waitlist.isEmpty()) {
                    cb.onError(new RuntimeException("No entrants found"));
                    return;
                }

                // Shuffle for random selection
                Collections.shuffle(waitlist);

                int total = waitlist.size();
                int winnerCount = Math.min(numWinners, total);
                
                // Calculate how many replacements we can actually select
                int remainingAfterWinners = total - winnerCount;
                int replacementCount = Math.min(replacementPoolSize, remainingAfterWinners);

                // Extract winners
                List<String> winners = new ArrayList<>(winnerCount);
                for (int i = 0; i < winnerCount; i++) {
                    Object id = waitlist.get(i).get("deviceId");
                    if (id != null) winners.add(id.toString());
                }

                // Extract replacement pool (next N after winners)
                List<String> replacements = new ArrayList<>(replacementCount);
                for (int i = winnerCount; i < winnerCount + replacementCount; i++) {
                    Object id = waitlist.get(i).get("deviceId");
                    if (id != null) replacements.add(id.toString());
                }

                // Mark winners and create replacement pool in Firebase
                eventDB.markWinners(eventId, winners, replacements, new EventDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void ignore) {
                        cb.onSuccess(winners, replacements);
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

