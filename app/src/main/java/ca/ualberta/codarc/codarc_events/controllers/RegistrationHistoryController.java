package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.RegistrationHistoryEntry;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Handles registration history retrieval and processing.
 * Fetches event history for entrants and determines selection status.
 * 
 * Note: The complex decision tree logic for determining selection status (checkAccepted → checkCancelled 
 * → checkInvited → checkWaitlisted) was implemented with assistance from Claude Sonnet 4.5 (Anthropic).
 * The sophisticated chained async state determination logic was developed with LLM assistance.
 */
public class RegistrationHistoryController {

    private static final String TAG = "RegistrationHistoryController";

    public static class HistoryResult {
        private final boolean success;
        private final List<RegistrationHistoryEntry> entries;
        private final String errorMessage;

        private HistoryResult(boolean success, List<RegistrationHistoryEntry> entries, String errorMessage) {
            this.success = success;
            this.entries = entries;
            this.errorMessage = errorMessage;
        }

        public static HistoryResult success(List<RegistrationHistoryEntry> entries) {
            return new HistoryResult(true, entries, null);
        }

        public static HistoryResult failure(String errorMessage) {
            return new HistoryResult(false, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public List<RegistrationHistoryEntry> getEntries() {
            return entries;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public interface Callback {
        void onResult(HistoryResult result);
    }

    private final EntrantDB entrantDB;
    private final EventDB eventDB;

    public RegistrationHistoryController(EntrantDB entrantDB, EventDB eventDB) {
        this.entrantDB = entrantDB;
        this.eventDB = eventDB;
    }

    /**
     * Loads the registration history for an entrant.
     *
     * @param deviceId the device ID of the entrant
     * @param callback callback that receives the history result
     */
    public void loadRegistrationHistory(String deviceId, Callback callback) {
        try {
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            callback.onResult(HistoryResult.failure("Device ID is required"));
            return;
        }

        entrantDB.getRegistrationHistory(deviceId, new EntrantDB.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> eventIds) {
                if (eventIds == null || eventIds.isEmpty()) {
                    callback.onResult(HistoryResult.success(new ArrayList<>()));
                    return;
                }

                processEventIds(eventIds, deviceId, callback);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to load registration history", e);
                callback.onResult(HistoryResult.failure("Failed to load history. Please try again."));
            }
        });
    }

    /**
     * Processes event IDs and builds history entries.
     *
     * @param eventIds the list of event IDs to process
     * @param deviceId the device ID of the entrant
     * @param callback callback that receives the final result
     */
    private void processEventIds(List<String> eventIds, String deviceId, Callback callback) {
        List<RegistrationHistoryEntry> entries = Collections.synchronizedList(new ArrayList<>());
        EventProcessor processor = new EventProcessor(entries, eventIds.size(), callback);

        if (eventIds.isEmpty()) {
            callback.onResult(HistoryResult.success(new ArrayList<>()));
            return;
        }

        for (String eventId : eventIds) {
            processSingleEvent(eventId, deviceId, entries, processor);
        }
    }

    /**
     * Processes a single event and adds it to the history entries.
     */
    private void processSingleEvent(String eventId, String deviceId,
                                    List<RegistrationHistoryEntry> entries,
                                    EventProcessor processor) {
        eventDB.eventExists(eventId, new EventDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean exists) {
                if (!exists) {
                    Log.d(TAG, "Event " + eventId + " no longer exists, filtering from history");
                    cleanupDeletedEvent(deviceId, eventId);
                    processor.onEventProcessed();
                    return;
                }
                fetchEventAndDetermineStatus(eventId, deviceId, entries, processor);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to check if event exists: " + eventId, e);
                processor.onEventProcessed();
            }
        });
    }

    /**
     * Fetches event details and determines selection status.
     */
    private void fetchEventAndDetermineStatus(String eventId, String deviceId,
                                              List<RegistrationHistoryEntry> entries,
                                              EventProcessor processor) {
        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null) {
                    processor.onEventProcessed();
                    return;
                }
                determineStatusAndAddEntry(event, deviceId, entries, processor);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to fetch event " + eventId, e);
                processor.onEventProcessed();
            }
        });
    }

    /**
     * Determines selection status and adds entry to history.
     */
    private void determineStatusAndAddEntry(Event event, String deviceId,
                                           List<RegistrationHistoryEntry> entries,
                                           EventProcessor processor) {
        determineSelectionStatus(event, deviceId, new SelectionStatusCallback() {
            @Override
            public void onStatus(String status) {
                RegistrationHistoryEntry entry = new RegistrationHistoryEntry(
                        event.getId(),
                        event.getName(),
                        event.getEventDateTime(),
                        status
                );
                entries.add(entry);
                processor.onEventProcessed();
            }
        });
    }

    /**
     * Helper class to track event processing completion.
     */
    private static class EventProcessor {
        private final List<RegistrationHistoryEntry> entries;
        private final int total;
        private final Callback callback;
        private int completed = 0;

        EventProcessor(List<RegistrationHistoryEntry> entries, int total, Callback callback) {
            this.entries = entries;
            this.total = total;
            this.callback = callback;
        }

        synchronized void onEventProcessed() {
            completed++;
            if (completed == total) {
                sortAndReturn(entries, callback);
            }
        }
    }

    /**
     * Sorts entries by event date and returns the result.
     *
     * @param entries the list of entries to sort
     * @param callback callback to receive the sorted result
     */
    private static void sortAndReturn(List<RegistrationHistoryEntry> entries, Callback callback) {

        Collections.sort(entries, new Comparator<RegistrationHistoryEntry>() {
            @Override
            public int compare(RegistrationHistoryEntry e1, RegistrationHistoryEntry e2) {
                try {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                    Date date1 = format.parse(e1.getEventDate());
                    Date date2 = format.parse(e2.getEventDate());
                    if (date1 == null || date2 == null) {
                        return 0;
                    }
                    return date2.compareTo(date1);
                } catch (Exception e) {
                    Log.e(TAG, "Error sorting entries by date", e);
                    return 0;
                }
            }
        });
        callback.onResult(HistoryResult.success(entries));
    }

    /**
     * Determines the selection status for an entrant in an event.
     *
     * @param event   the event to check
     * @param deviceId the device ID of the entrant
     * @param callback callback that receives the status string
     */
    private void determineSelectionStatus(Event event, String deviceId, SelectionStatusCallback callback) {
        checkAccepted(event, deviceId, callback);
    }

    private void checkAccepted(Event event, String deviceId, SelectionStatusCallback callback) {
        eventDB.isEntrantAccepted(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isAccepted) {
                if (isAccepted != null && isAccepted) {
                    callback.onStatus("Accepted");
                } else {
                    checkCancelled(event, deviceId, callback);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to check accepted status", e);
                checkCancelled(event, deviceId, callback);
            }
        });
    }

    private void checkCancelled(Event event, String deviceId, SelectionStatusCallback callback) {
        eventDB.isEntrantCancelled(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isCancelled) {
                if (isCancelled != null && isCancelled) {
                    callback.onStatus("Cancelled");
                } else {
                    checkInvited(event, deviceId, callback);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to check cancelled status", e);
                checkInvited(event, deviceId, callback);
            }
        });
    }

    private void checkInvited(Event event, String deviceId, SelectionStatusCallback callback) {
        eventDB.isEntrantWinner(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isWinner) {
                if (isWinner != null && isWinner) {
                    callback.onStatus("Invited");
                } else {
                    checkWaitlisted(event, deviceId, callback);
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to check winner status", e);
                checkWaitlisted(event, deviceId, callback);
            }
        });
    }

    private void checkWaitlisted(Event event, String deviceId, SelectionStatusCallback callback) {
        eventDB.isEntrantOnWaitlist(event.getId(), deviceId, new EventDB.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean isOnWaitlist) {
                if (isOnWaitlist != null && isOnWaitlist) {
                    callback.onStatus("Waitlisted");
                } else {
                    boolean isPast = isEventPast(event);
                    callback.onStatus(isPast ? "Not Selected" : "Waitlisted");
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to check waitlist status", e);
                callback.onStatus("Waitlisted");
            }
        });
    }

    /**
     * Checks if an event's date is in the past.
     *
     * @param event the event to check
     * @return true if the event date has passed, false otherwise
     */
    private boolean isEventPast(Event event) {
        if (event == null || event.getEventDateTime() == null || event.getEventDateTime().isEmpty()) {
            return false;
        }

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date eventDate = format.parse(event.getEventDateTime());
            return eventDate != null && eventDate.before(new Date());
        } catch (Exception e) {
            Log.e(TAG, "Error parsing event date", e);
            return false;
        }
    }

    /**
     * Removes a deleted event from history.
     *
     * @param deviceId the device ID of the entrant
     * @param eventId  the event ID to remove
     */
    private void cleanupDeletedEvent(String deviceId, String eventId) {
        entrantDB.removeEventFromEntrant(deviceId, eventId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Cleaned up deleted event from history: " + eventId);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.w(TAG, "Failed to cleanup deleted event: " + eventId, e);
            }
        });
    }

    private interface SelectionStatusCallback {
        void onStatus(String status);
    }
}

