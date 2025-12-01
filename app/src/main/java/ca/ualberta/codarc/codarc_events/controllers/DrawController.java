package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

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
    private final EntrantDB entrantDB;
    private static final int DEFAULT_REPLACEMENT_POOL_SIZE = 3;

    public DrawController(EventDB eventDB) {
        this.eventDB = eventDB;
        this.entrantDB = new EntrantDB();
    }

    public DrawController(EventDB eventDB, EntrantDB entrantDB) {
        this.eventDB = eventDB;
        this.entrantDB = entrantDB;
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
     * Runs lottery with default replacement pool size.
     *
     * @param eventId the event ID
     * @param numWinners number of winners to select
     * @param cb callback for completion
     */
    public void runDraw(String eventId, int numWinners, DrawCallback cb) {
        runDraw(eventId, numWinners, DEFAULT_REPLACEMENT_POOL_SIZE, cb);
    }
    
    /**
     * Runs lottery with custom replacement pool size.
     *
     * @param eventId the event ID
     * @param numWinners number of winners to select
     * @param replacementPoolSize size of replacement pool
     * @param cb callback for completion
     */
    public void runDraw(String eventId, int numWinners, int replacementPoolSize, DrawCallback cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            if (numWinners <= 0) {
                throw new IllegalArgumentException("Number of winners must be > 0");
            }
            if (replacementPoolSize < 0) {
                throw new IllegalArgumentException("Replacement pool size cannot be negative");
            }
        } catch (IllegalArgumentException e) {
            cb.onError(e);
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
                
                int remainingAfterWinners = total - winnerCount;
                int replacementCount = Math.min(replacementPoolSize, remainingAfterWinners);

                List<String> winners = new ArrayList<>(winnerCount);
                for (int i = 0; i < winnerCount; i++) {
                    Object id = waitlist.get(i).get("deviceId");
                    if (id != null) winners.add(id.toString());
                }

                List<String> replacements = new ArrayList<>(replacementCount);
                for (int i = winnerCount; i < winnerCount + replacementCount; i++) {
                    Object id = waitlist.get(i).get("deviceId");
                    if (id != null) replacements.add(id.toString());
                }

                eventDB.markWinners(eventId, winners, replacements, new EventDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void ignore) {
                        sendWinnerNotifications(eventId, winners, new NotificationCallback() {
                            @Override
                            public void onComplete() {
                                cb.onSuccess(winners, replacements);
                            }
                        });
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

    /**
     * Sends winner notifications to all winners.
     */
    private void sendWinnerNotifications(String eventId, List<String> winnerIds, NotificationCallback cb) {
        if (winnerIds == null || winnerIds.isEmpty()) {
            cb.onComplete();
            return;
        }

        checkExistingNotifications(eventId, winnerIds, new NotificationCheckCallback() {
            @Override
            public void onChecked(List<String> winnersToNotify) {
                sendNotificationsToList(eventId, winnersToNotify, cb);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.w("DrawController", "Failed to check existing notifications, sending to all", e);
                sendNotificationsToList(eventId, winnerIds, cb);
            }
        });
    }

    // The following function is from Anthropic Claude Sonnet 4.5, "How do I implement notification deduplication logic that checks existing notifications before sending to prevent duplicates in Java with async coordination?", 2024-01-15
    /**
     * Checks which winners already have notifications for this event.
     */
    private void checkExistingNotifications(String eventId, List<String> winnerIds, NotificationCheckCallback cb) {
        if (winnerIds.isEmpty()) {
            cb.onChecked(new ArrayList<>());
            return;
        }

        List<String> winnersToNotify = Collections.synchronizedList(new ArrayList<>());
        NotificationChecker checker = new NotificationChecker(winnersToNotify, winnerIds.size(), cb);

        for (String winnerId : winnerIds) {
            checkSingleWinnerNotification(eventId, winnerId, winnersToNotify, checker);
        }
    }

    /**
     * Checks if a single winner already has a notification for this event.
     */
    private void checkSingleWinnerNotification(String eventId, String winnerId,
                                               List<String> winnersToNotify,
                                               NotificationChecker checker) {
        entrantDB.getNotifications(winnerId, new EntrantDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> notifications) {
                if (!hasWinnerNotification(notifications, eventId)) {
                    winnersToNotify.add(winnerId);
                }
                checker.onChecked();
            }

            @Override
            public void onError(@NonNull Exception e) {
                winnersToNotify.add(winnerId);
                checker.onChecked();
            }
        });
    }

    /**
     * Checks if notifications list contains a winner notification for the event.
     */
    private boolean hasWinnerNotification(List<Map<String, Object>> notifications, String eventId) {
        if (notifications == null) {
            return false;
        }
        for (Map<String, Object> notification : notifications) {
            String notifEventId = (String) notification.get("eventId");
            String category = (String) notification.get("category");
            if (eventId.equals(notifEventId) && "winner".equals(category)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sends notifications to a list of winners.
     */
    private void sendNotificationsToList(String eventId, List<String> winnerIds, NotificationCallback cb) {
        if (winnerIds == null || winnerIds.isEmpty()) {
            cb.onComplete();
            return;
        }

        String message = "Congratulations! You won. Proceed to signup.";
        NotificationSender sender = new NotificationSender(winnerIds.size(), cb);

        for (String winnerId : winnerIds) {
            sendSingleNotification(winnerId, eventId, message, sender);
        }
    }

    /**
     * Sends a notification to a single winner.
     */
    private void sendSingleNotification(String winnerId, String eventId, String message,
                                        NotificationSender sender) {
        entrantDB.addNotification(winnerId, eventId, message, "winner", new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                sender.onSuccess();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e("DrawController", "Failed to send notification to " + winnerId, e);
                sender.onError();
            }
        });
    }

    /**
     * Helper class to track notification checking completion.
     */
    private static class NotificationChecker {
        private final List<String> winnersToNotify;
        private final int total;
        private final NotificationCheckCallback callback;
        private int checked = 0;

        NotificationChecker(List<String> winnersToNotify, int total, NotificationCheckCallback callback) {
            this.winnersToNotify = winnersToNotify;
            this.total = total;
            this.callback = callback;
        }

        synchronized void onChecked() {
            checked++;
            if (checked == total) {
                callback.onChecked(new ArrayList<>(winnersToNotify));
            }
        }
    }

    /**
     * Helper class to track notification sending completion.
     */
    private static class NotificationSender {
        private final int total;
        private final NotificationCallback callback;
        private int completed = 0;
        private int failed = 0;

        NotificationSender(int total, NotificationCallback callback) {
            this.total = total;
            this.callback = callback;
        }

        synchronized void onSuccess() {
            completed++;
            checkCompletion();
        }

        synchronized void onError() {
            failed++;
            checkCompletion();
        }

        private void checkCompletion() {
            if (completed + failed == total) {
                callback.onComplete();
            }
        }
    }

    private interface NotificationCallback {
        void onComplete();
    }

    private interface NotificationCheckCallback {
        void onChecked(List<String> winnersToNotify);
        void onError(@NonNull Exception e);
    }
}

