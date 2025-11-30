package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;

/**
 * Handles lottery draw - selects winners and replacement pool.
 * Automatically sends notifications to winners when draw is completed.
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
                        // Automatically send notifications to winners
                        sendWinnerNotifications(eventId, winners, new NotificationCallback() {
                            @Override
                            public void onComplete() {
                                // Notifications sent (or failed silently - don't block draw success)
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
     * Sends winner notifications to all winners, checking for duplicates first.
     * This prevents sending multiple notifications to the same user.
     */
    private void sendWinnerNotifications(String eventId, List<String> winnerIds, NotificationCallback cb) {
        if (winnerIds == null || winnerIds.isEmpty()) {
            cb.onComplete();
            return;
        }

        // Check which winners already have notifications for this event
        checkExistingNotifications(eventId, winnerIds, new NotificationCheckCallback() {
            @Override
            public void onChecked(List<String> winnersToNotify) {
                if (winnersToNotify.isEmpty()) {
                    // All winners already notified
                    cb.onComplete();
                    return;
                }

                // Send notifications only to winners who haven't been notified yet
                String message = "Congratulations! You won. Proceed to signup.";
                final int total = winnersToNotify.size();
                final int[] completed = {0};
                final int[] failed = {0};

                for (String winnerId : winnersToNotify) {
                    entrantDB.addNotification(winnerId, eventId, message, "winner", new EntrantDB.Callback<Void>() {
                        @Override
                        public void onSuccess(Void value) {
                            completed[0]++;
                            checkNotificationCompletion(completed, failed, total, cb);
                        }

                        @Override
                        public void onError(@NonNull Exception e) {
                            failed[0]++;
                            // Log but don't fail the entire operation
                            android.util.Log.e("DrawController", "Failed to send notification to " + winnerId, e);
                            checkNotificationCompletion(completed, failed, total, cb);
                        }
                    });
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                // If check fails, try sending to all (better to send duplicate than miss one)
                android.util.Log.w("DrawController", "Failed to check existing notifications, sending to all", e);
                sendNotificationsToAll(eventId, winnerIds, cb);
            }
        });
    }

    /**
     * Checks which winners already have notifications for this event.
     */
    private void checkExistingNotifications(String eventId, List<String> winnerIds, NotificationCheckCallback cb) {
        // For simplicity, we'll check by querying each winner's notifications
        // In a production system, you might want to add a flag to the winners document
        final List<String> winnersToNotify = new ArrayList<>();
        final int[] checked = {0};
        final int total = winnerIds.size();

        if (total == 0) {
            cb.onChecked(winnersToNotify);
            return;
        }

        for (String winnerId : winnerIds) {
            entrantDB.getNotifications(winnerId, new EntrantDB.Callback<List<Map<String, Object>>>() {
                @Override
                public void onSuccess(List<Map<String, Object>> notifications) {
                    boolean hasNotification = false;
                    if (notifications != null) {
                        for (Map<String, Object> notification : notifications) {
                            String notifEventId = (String) notification.get("eventId");
                            String category = (String) notification.get("category");
                            if (eventId.equals(notifEventId) && "winner".equals(category)) {
                                hasNotification = true;
                                break;
                            }
                        }
                    }
                    if (!hasNotification) {
                        synchronized (winnersToNotify) {
                            winnersToNotify.add(winnerId);
                        }
                    }
                    checked[0]++;
                    if (checked[0] == total) {
                        cb.onChecked(winnersToNotify);
                    }
                }

                @Override
                public void onError(@NonNull Exception e) {
                    // If we can't check, include this winner to be safe
                    synchronized (winnersToNotify) {
                        winnersToNotify.add(winnerId);
                    }
                    checked[0]++;
                    if (checked[0] == total) {
                        cb.onChecked(winnersToNotify);
                    }
                }
            });
        }
    }

    /**
     * Fallback: sends notifications to all winners without checking.
     */
    private void sendNotificationsToAll(String eventId, List<String> winnerIds, NotificationCallback cb) {
        if (winnerIds == null || winnerIds.isEmpty()) {
            cb.onComplete();
            return;
        }

        String message = "Congratulations! You won. Proceed to signup.";
        final int total = winnerIds.size();
        final int[] completed = {0};
        final int[] failed = {0};

        for (String winnerId : winnerIds) {
            entrantDB.addNotification(winnerId, eventId, message, "winner", new EntrantDB.Callback<Void>() {
                @Override
                public void onSuccess(Void value) {
                    completed[0]++;
                    checkNotificationCompletion(completed, failed, total, cb);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    failed[0]++;
                    android.util.Log.e("DrawController", "Failed to send notification to " + winnerId, e);
                    checkNotificationCompletion(completed, failed, total, cb);
                }
            });
        }
    }

    private void checkNotificationCompletion(int[] completed, int[] failed, int total, NotificationCallback cb) {
        if (completed[0] + failed[0] == total) {
            cb.onComplete();
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

