package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import android.util.Log;

import java.util.List;

import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.OrganizerDB;
import ca.ualberta.codarc.codarc_events.data.PosterStorage;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.User;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Handles banning of organizers by administrators.
 * Sets banned flag to true (does NOT delete organizer document).
 * Cascades deletion to events, entrant history, and notifications.
 */
public class RemoveOrganizerController {

    private static final String TAG = "RemoveOrganizerController";

    public static class RemoveOrganizerResult {
        private final boolean success;
        private final String errorMessage;

        private RemoveOrganizerResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static RemoveOrganizerResult success() {
            return new RemoveOrganizerResult(true, null);
        }

        public static RemoveOrganizerResult failure(String errorMessage) {
            return new RemoveOrganizerResult(false, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private final OrganizerDB organizerDB;
    private final EventDB eventDB;
    private final EntrantDB entrantDB;
    private final DeleteEventController deleteEventController;
    private final PosterStorage posterStorage;
    private final UserDB userDB;

    public RemoveOrganizerController() {
        this.organizerDB = new OrganizerDB();
        this.eventDB = new EventDB();
        this.entrantDB = new EntrantDB();
        this.deleteEventController = new DeleteEventController();
        this.posterStorage = new PosterStorage();
        this.userDB = new UserDB();
    }

    public RemoveOrganizerController(OrganizerDB organizerDB, EventDB eventDB, EntrantDB entrantDB,
                                    PosterStorage posterStorage, UserDB userDB) {
        this.organizerDB = organizerDB;
        this.eventDB = eventDB;
        this.entrantDB = entrantDB;
        this.deleteEventController = new DeleteEventController();
        this.posterStorage = posterStorage;
        this.userDB = userDB;
    }

    public RemoveOrganizerController(OrganizerDB organizerDB, EventDB eventDB, EntrantDB entrantDB,
                                    DeleteEventController deleteEventController, PosterStorage posterStorage, UserDB userDB) {
        this.organizerDB = organizerDB;
        this.eventDB = eventDB;
        this.entrantDB = entrantDB;
        this.deleteEventController = deleteEventController;
        this.posterStorage = posterStorage;
        this.userDB = userDB;
    }

    /**
     * Bans an organizer and deletes all their events.
     *
     * @param organizerId the organizer device ID to ban
     * @param adminDeviceId the device ID of the admin performing the ban
     * @param callback callback for completion
     */
    public void banOrganizer(String organizerId, String adminDeviceId, Callback callback) {
        try {
            ValidationHelper.requireNonEmpty(organizerId, "organizerId");
            ValidationHelper.requireNonEmpty(adminDeviceId, "adminDeviceId");
        } catch (IllegalArgumentException e) {
            callback.onResult(RemoveOrganizerResult.failure(e.getMessage()));
            return;
        }

        validateAdminStatus(adminDeviceId, new ValidationCallback() {
            @Override
            public void onSuccess() {
                proceedWithBan(organizerId, adminDeviceId, callback);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onResult(RemoveOrganizerResult.failure(errorMessage));
            }
        });
    }

    private void proceedWithBan(String organizerId, String adminDeviceId, Callback callback) {
        getOrganizerEvents(organizerId, new EventsCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (events == null || events.isEmpty()) {
                    setBannedStatus(organizerId, callback);
                    return;
                }
                deleteAllEvents(events, organizerId, adminDeviceId, callback);
            }

            @Override
            public void onError(String errorMessage) {
                callback.onResult(RemoveOrganizerResult.failure(errorMessage));
            }
        });
    }

    private void deleteAllEvents(List<Event> events, String organizerId, String adminDeviceId, Callback callback) {
        final int totalEvents = events.size();
        final EventDeletionAggregator aggregator = new EventDeletionAggregator(totalEvents, organizerId, adminDeviceId, this, callback);

        for (Event event : events) {
            deleteEventAndCleanup(event.getId(), event, adminDeviceId, aggregator);
        }
    }

    private void deleteEventAndCleanup(String eventId, Event event, String adminDeviceId, EventDeletionAggregator aggregator) {
        deletePosterAndProceed(eventId, event, adminDeviceId, aggregator);
    }

    private void deletePosterAndProceed(String eventId, Event event, String adminDeviceId, EventDeletionAggregator aggregator) {
        posterStorage.deletePoster(eventId, new PosterStorage.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Poster deleted successfully for event: " + eventId);
                deleteEventFromFirestore(eventId, event, adminDeviceId, aggregator);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.w(TAG, "Failed to delete poster for event: " + eventId, e);
                deleteEventFromFirestore(eventId, event, adminDeviceId, aggregator);
            }
        });
    }

    private void deleteEventFromFirestore(String eventId, Event event, String adminDeviceId, EventDeletionAggregator aggregator) {
        deleteEventController.deleteEvent(eventId, adminDeviceId, new DeleteEventController.Callback() {
            @Override
            public void onResult(DeleteEventController.DeleteEventResult result) {
                if (result.isSuccess()) {
                    Log.d(TAG, "Event deleted from Firestore: " + eventId);
                } else {
                    Log.w(TAG, "Failed to delete event from Firestore: " + eventId);
                }
                removeEventFromOrganizerSubcollection(event.getOrganizerId(), eventId, aggregator);
            }
        });
    }

    private void removeEventFromOrganizerSubcollection(String organizerId, String eventId, EventDeletionAggregator aggregator) {
        if (organizerId == null || organizerId.isEmpty()) {
            removeEventFromEntrantHistoryLazy(eventId, aggregator);
            return;
        }

        organizerDB.removeEventFromOrganizer(organizerId, eventId, new OrganizerDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Removed event from organizer's events subcollection: " + eventId);
                removeEventFromEntrantHistoryLazy(eventId, aggregator);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.w(TAG, "Failed to remove event from organizer's events subcollection: " + eventId, e);
                removeEventFromEntrantHistoryLazy(eventId, aggregator);
            }
        });
    }

    private void removeEventFromEntrantHistoryLazy(String eventId, EventDeletionAggregator aggregator) {
        entrantDB.removeEventFromAllEntrants(eventId, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                aggregator.onEventProcessed();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.w(TAG, "Failed to remove event from entrant history (lazy): " + eventId, e);
                aggregator.onEventProcessed();
            }
        });
    }

    void setBannedStatus(String organizerId, Callback callback) {
        organizerDB.setBannedStatus(organizerId, true, new OrganizerDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "Organizer banned successfully: " + organizerId);
                callback.onResult(RemoveOrganizerResult.success());
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to ban organizer: " + organizerId, e);
                callback.onResult(RemoveOrganizerResult.failure("Failed to ban organizer. Please try again."));
            }
        });
    }

    private void validateAdminStatus(String deviceId, ValidationCallback callback) {
        userDB.getUser(deviceId, new UserDB.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.isAdmin()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Admin access required");
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to check admin status", e);
                callback.onError("Failed to verify admin status");
            }
        });
    }

    private void getOrganizerEvents(String organizerId, EventsCallback callback) {
        organizerDB.getOrganizerEvents(organizerId, new OrganizerDB.Callback<List<String>>() {
            @Override
            public void onSuccess(List<String> eventIds) {
                if (eventIds == null || eventIds.isEmpty()) {
                    callback.onSuccess(new java.util.ArrayList<>());
                    return;
                }

                fetchEventsByIds(eventIds, callback);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to get organizer events", e);
                callback.onError("Failed to retrieve organizer events");
            }
        });
    }

    private void fetchEventsByIds(List<String> eventIds, EventsCallback callback) {
        final int total = eventIds.size();
        final List<Event> events = new java.util.ArrayList<>();
        final EventFetchAggregator aggregator = new EventFetchAggregator(total, events, callback);

        for (String eventId : eventIds) {
            eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
                @Override
                public void onSuccess(Event event) {
                    if (event != null) {
                        synchronized (events) {
                            events.add(event);
                        }
                    }
                    aggregator.onEventFetched();
                }

                @Override
                public void onError(@NonNull Exception e) {
                    Log.w(TAG, "Failed to fetch event: " + eventId, e);
                    aggregator.onEventFetched();
                }
            });
        }
    }

    private interface ValidationCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private interface EventsCallback {
        void onSuccess(List<Event> events);
        void onError(String errorMessage);
    }

    private static class EventDeletionAggregator {
        private final int total;
        private final String organizerId;
        private final String adminDeviceId;
        private final RemoveOrganizerController controller;
        private final Callback callback;
        private int completed;

        EventDeletionAggregator(int total, String organizerId, String adminDeviceId, RemoveOrganizerController controller, Callback callback) {
            this.total = total;
            this.organizerId = organizerId;
            this.adminDeviceId = adminDeviceId;
            this.controller = controller;
            this.callback = callback;
            this.completed = 0;
        }

        synchronized void onEventProcessed() {
            completed++;
            if (completed == total) {
                controller.setBannedStatus(organizerId, callback);
            }
        }
    }

    private static class EventFetchAggregator {
        private final int total;
        private final List<Event> events;
        private final EventsCallback callback;
        private int completed;

        EventFetchAggregator(int total, List<Event> events, EventsCallback callback) {
            this.total = total;
            this.events = events;
            this.callback = callback;
            this.completed = 0;
        }

        synchronized void onEventFetched() {
            completed++;
            if (completed == total) {
                callback.onSuccess(events);
            }
        }
    }

    public interface Callback {
        void onResult(RemoveOrganizerResult result);
    }
}
