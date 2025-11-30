package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Handles Firestore operations for events.
 * 
 * Note: Complex batch operations for marking winners and managing replacement pools
 * (markWinners, markReplacement, promoteReplacementToWinner) were implemented with assistance 
 * from Claude Sonnet 4.5 (Anthropic). The sophisticated batch write operations with multiple
 * subcollection updates and state transitions were developed with LLM assistance.
 */
public class EventDB {

    private static final int BATCH_SIZE = 500;

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;

    public EventDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Adds or updates an event in Firestore.
     */
    public void addEvent(Event event, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonNull(event, "event");
            ValidationHelper.requireNonNull(event.getId(), "event.id");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("name", event.getName());
        eventData.put("description", event.getDescription());
        eventData.put("location", event.getLocation());
        eventData.put("open", event.isOpen());
        eventData.put("organizerId", event.getOrganizerId());
        eventData.put("qrCode", event.getQrCode());
        eventData.put("maxCapacity", event.getMaxCapacity());
        eventData.put("eventDateTime", event.getEventDateTime());
        eventData.put("registrationOpen", event.getRegistrationOpen());
        eventData.put("registrationClose", event.getRegistrationClose());
        eventData.put("posterUrl", event.getPosterUrl());
        
        if (event.getTags() != null && !event.getTags().isEmpty()) {
            eventData.put("tags", event.getTags());
        } else {
            eventData.put("tags", new ArrayList<String>());
        }

        db.collection("events").document(event.getId())
                .get()
                .addOnSuccessListener(existingDoc -> {
                    final List<String> oldTags;
                    if (existingDoc != null && existingDoc.exists()) {
                        Object tagsObj = existingDoc.get("tags");
                        if (tagsObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<String> tags = (List<String>) tagsObj;
                            oldTags = tags;
                        } else {
                            oldTags = null;
                        }
                    } else {
                        oldTags = null;
                    }

                    final List<String> eventTags = event.getTags();

                    db.collection("events").document(event.getId())
                            .set(eventData)
                            .addOnSuccessListener(aVoid -> {
                                TagDB tagDB = new TagDB();
                                if (oldTags == null) {
                                    tagDB.addTags(eventTags, new TagDB.Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void value) {
                                            cb.onSuccess(null);
                                        }

                                        @Override
                                        public void onError(@NonNull Exception e) {
                                            android.util.Log.w("EventDB", "Failed to update tags collection", e);
                                            cb.onSuccess(null);
                                        }
                                    });
                                } else {
                                    tagDB.updateTags(oldTags, eventTags, new TagDB.Callback<Void>() {
                                        @Override
                                        public void onSuccess(Void value) {
                                            cb.onSuccess(null);
                                        }

                                        @Override
                                        public void onError(@NonNull Exception e) {
                                            android.util.Log.w("EventDB", "Failed to update tags collection", e);
                                            cb.onSuccess(null);
                                        }
                                    });
                                }
                            })
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Gets all events with real-time updates.
     */
    public void getAllEvents(Callback<List<Event>> cb) {
        db.collection("events").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                cb.onError(e);
                return;
            }
            if (snapshots == null) {
                cb.onSuccess(new ArrayList<>());
                return;
            }
            List<Event> events = new ArrayList<>();
            for (QueryDocumentSnapshot doc : snapshots) {
                Event event = parseEventFromDocument(doc);
                if (event != null) {
                    events.add(event);
                }
            }
            cb.onSuccess(events);
        });
    }

    /**
     * Fetches all events once.
     *
     * @param cb callback with the list of events
     */
    public void getAllEventsOnce(Callback<List<Event>> cb) {
        db.collection("events")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> events = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Event event = parseEventFromDocument(doc);
                            if (event != null) {
                                events.add(event);
                            }
                        }
                    }
                    cb.onSuccess(events);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Gets events for a specific organizer, ordered by event date (newest first).
     *
     * @param organizerId the organizer device ID
     * @param limit maximum number of events to return
     * @param cb callback with list of Events
     */
    public void getEventsByOrganizer(String organizerId, int limit, Callback<List<Event>> cb) {
        try {
            ValidationHelper.requireNonEmpty(organizerId, "organizerId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        db.collection("events")
                .whereEqualTo("organizerId", organizerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> events = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Event event = parseEventFromDocument(doc);
                            if (event != null) {
                                events.add(event);
                            }
                        }
                    }

                    events.sort((e1, e2) -> {
                        String date1 = e1.getEventDateTime();
                        String date2 = e2.getEventDateTime();
                        if (date1 == null && date2 == null) return 0;
                        if (date1 == null) return 1;
                        if (date2 == null) return -1;
                        return date2.compareTo(date1);
                    });

                    if (events.size() > limit) {
                        events = events.subList(0, limit);
                    }

                    cb.onSuccess(events);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Fetches a single event by its ID.
     */
    public void getEvent(String eventId, Callback<Event> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        Event event = parseEventFromDocument(snapshot);
                        if (event != null) {
                            cb.onSuccess(event);
                        } else {
                            cb.onError(new RuntimeException("Failed to parse event"));
                        }
                    } else {
                        cb.onError(new RuntimeException("Event not found"));
                    }
                })
                .addOnFailureListener(cb::onError);
    }

    public void isEntrantOnWaitlist(String eventId, String deviceId, Callback<Boolean> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        db.collection("events").document(eventId)
                .collection("waitingList").document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    cb.onSuccess(snapshot != null && snapshot.exists());
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Checks if an entrant is a winner for a specific event.
     *
     * @param eventId  the event ID
     * @param deviceId the device ID of the entrant
     * @param cb       callback that receives true if the entrant is a winner, false otherwise
     */
    public void isEntrantWinner(String eventId, String deviceId, Callback<Boolean> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        db.collection("events").document(eventId)
                .collection("winners").document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    cb.onSuccess(snapshot != null && snapshot.exists());
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Checks if an event exists in Firestore.
     *
     * @param eventId the event ID to check
     * @param cb      callback that receives true if the event exists, false otherwise
     */
    public void eventExists(String eventId, Callback<Boolean> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    cb.onSuccess(snapshot != null && snapshot.exists());
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Checks if an entrant is accepted for a specific event.
     *
     * @param eventId  the event ID
     * @param deviceId the device ID of the entrant
     * @param cb       callback that receives true if the entrant is accepted, false otherwise
     */
    public void isEntrantAccepted(String eventId, String deviceId, Callback<Boolean> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        db.collection("events").document(eventId)
                .collection("accepted").document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    cb.onSuccess(snapshot != null && snapshot.exists());
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Checks if an entrant is cancelled for a specific event.
     *
     * @param eventId  the event ID
     * @param deviceId the device ID of the entrant
     * @param cb       callback that receives true if the entrant is cancelled, false otherwise
     */
    public void isEntrantCancelled(String eventId, String deviceId, Callback<Boolean> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        db.collection("events").document(eventId)
                .collection("cancelled").document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    cb.onSuccess(snapshot != null && snapshot.exists());
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Checks if user can join the waitlist.
     *
     * @param eventId  the event ID
     * @param deviceId the device ID of the entrant
     * @param cb       callback that receives true if the entrant can join, false otherwise
     */
    public void canJoinWaitlist(String eventId, String deviceId, Callback<Boolean> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        db.collection("events").document(eventId)
                .collection("waitingList").document(deviceId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        cb.onSuccess(false);
                        return;
                    }
                    
                    db.collection("events").document(eventId)
                            .collection("winners").document(deviceId)
                            .get()
                            .addOnSuccessListener(winnerSnapshot -> {
                                if (winnerSnapshot != null && winnerSnapshot.exists()) {
                                    cb.onSuccess(false);
                                    return;
                                }
                                
                                db.collection("events").document(eventId)
                                        .collection("accepted").document(deviceId)
                                        .get()
                                        .addOnSuccessListener(acceptedSnapshot -> {
                                            if (acceptedSnapshot != null && acceptedSnapshot.exists()) {
                                                cb.onSuccess(false);
                                            } else {
                                                cb.onSuccess(true);
                                            }
                                        })
                                        .addOnFailureListener(cb::onError);
                            })
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getWaitlistCount(String eventId, Callback<Integer> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot != null ? querySnapshot.size() : 0;
                    cb.onSuccess(count);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Gets the count of accepted participants for an event.
     *
     * @param eventId the event ID
     * @param cb callback with the accepted count
     */
    public void getAcceptedCount(String eventId, Callback<Integer> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        db.collection("events").document(eventId)
                .collection("accepted")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot != null ? querySnapshot.size() : 0;
                    cb.onSuccess(count);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Adds an entrant to the waitlist.
     *
     * @param eventId  the event ID
     * @param deviceId the device ID of the entrant
     * @param cb       callback for completion
     */
    public void joinWaitlist(String eventId, String deviceId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("request_time", FieldValue.serverTimestamp());

        db.collection("events").document(eventId)
                .collection("waitingList").document(deviceId)
                .set(data)
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Removes an entrant from the waitlist.
     *
     * @param eventId  the event ID
     * @param deviceId the device ID of the entrant
     * @param cb       callback for completion
     */
    public void leaveWaitlist(String eventId, String deviceId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        db.collection("events").document(eventId)
                .collection("waitingList").document(deviceId)
                .delete()
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Removes an entrant from all event subcollections.
     *
     * @param eventId the event ID
     * @param deviceId the device ID of the entrant to remove
     * @param cb callback for completion
     */
    public void removeEntrantFromEvent(String eventId, String deviceId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        WriteBatch batch = db.batch();
        
        DocumentReference waitlistRef = db.collection("events")
            .document(eventId)
            .collection("waitingList")
            .document(deviceId);
        batch.delete(waitlistRef);
        
        DocumentReference winnersRef = db.collection("events")
            .document(eventId)
            .collection("winners")
            .document(deviceId);
        batch.delete(winnersRef);
        
        DocumentReference acceptedRef = db.collection("events")
            .document(eventId)
            .collection("accepted")
            .document(deviceId);
        batch.delete(acceptedRef);
        
        // Remove from cancelled
        DocumentReference cancelledRef = db.collection("events")
            .document(eventId)
            .collection("cancelled")
            .document(deviceId);
        batch.delete(cancelledRef);
        
        DocumentReference replacementRef = db.collection("events")
            .document(eventId)
            .collection("replacementPool")
            .document(deviceId);
        batch.delete(replacementRef);
        
        batch.commit()
            .addOnSuccessListener(unused -> {
                android.util.Log.d("EventDB", "Removed entrant from all event subcollections: " + deviceId);
                cb.onSuccess(null);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("EventDB", "Failed to remove entrant from event: " + eventId, e);
                cb.onError(e);
            });
    }

    public void getWaitlist(String eventId, Callback<List<Map<String, Object>>> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        db.collection("events").document(eventId)
                .collection("waitingList")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> entries = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> entry = new HashMap<>();
                            entry.put("deviceId", doc.getId());
                            entry.put("requestTime", doc.get("request_time"));
                            entries.add(entry);
                        }
                    }
                    cb.onSuccess(entries);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Marks entrants as winners and creates replacement pool.
     *
     * @param eventId        the event ID
     * @param winnerIds      list of winner device IDs
     * @param replacementIds list of replacement device IDs
     * @param cb             callback for completion
     */
    public void markWinners(String eventId, List<String> winnerIds, List<String> replacementIds, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonNull(winnerIds, "winnerIds");
            if (winnerIds.isEmpty()) {
                throw new IllegalArgumentException("winnerIds cannot be empty");
            }
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        WriteBatch batch = db.batch();
        long timestamp = System.currentTimeMillis();

        // Move winners from waitingList to winners
        for (String winnerId : winnerIds) {
            // Remove from waitingList
            DocumentReference waitlistRef = db.collection("events")
                    .document(eventId)
                    .collection("waitingList")
                    .document(winnerId);
            batch.delete(waitlistRef);

            DocumentReference winnersRef = db.collection("events")
                    .document(eventId)
                    .collection("winners")
                    .document(winnerId);
            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", winnerId);
            data.put("invitedAt", timestamp);
            batch.set(winnersRef, data);
        }

        if (replacementIds != null && !replacementIds.isEmpty()) {
            for (String replacementId : replacementIds) {
                DocumentReference waitlistRef = db.collection("events")
                        .document(eventId)
                        .collection("waitingList")
                        .document(replacementId);
                batch.delete(waitlistRef);

                DocumentReference poolRef = db.collection("events")
                        .document(eventId)
                        .collection("replacementPool")
                        .document(replacementId);
                Map<String, Object> data = new HashMap<>();
                data.put("deviceId", replacementId);
                data.put("addedToPoolAt", timestamp);
                batch.set(poolRef, data);
            }
        }

        batch.commit()
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }
    
    /**
     * Marks entrants as winners without replacement pool.
     *
     * @param eventId    the event ID
     * @param entrantIds list of winner device IDs
     * @param cb         callback for completion
     */
    public void markWinners(String eventId, List<String> entrantIds, Callback<Void> cb) {
        markWinners(eventId, entrantIds, new ArrayList<>(), cb);
    }

    /**
     * Promotes a replacement from pool to winners.
     *
     * @param eventId   the event ID
     * @param entrantId the device ID of the replacement (null to pick first)
     * @param cb        callback for completion
     */
    public void markReplacement(String eventId, String entrantId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        if (entrantId != null && !entrantId.isEmpty()) {
            promoteReplacementToWinner(eventId, entrantId, cb);
        } else {
            getReplacementPool(eventId, new Callback<List<Map<String, Object>>>() {
                @Override
                public void onSuccess(List<Map<String, Object>> pool) {
                    if (pool == null || pool.isEmpty()) {
                        cb.onError(new IllegalStateException("Replacement pool is empty"));
                        return;
                    }
                    String firstReplacementId = (String) pool.get(0).get("deviceId");
                    promoteReplacementToWinner(eventId, firstReplacementId, cb);
                }

                @Override
                public void onError(@NonNull Exception e) {
                    cb.onError(e);
                }
            });
        }
    }
    
    private void promoteReplacementToWinner(String eventId, String entrantId, Callback<Void> cb) {
        db.collection("events").document(eventId)
                .collection("replacementPool").document(entrantId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        cb.onError(new IllegalArgumentException("Entrant not in replacement pool"));
                        return;
                    }

                    WriteBatch batch = db.batch();

                    DocumentReference poolRef = db.collection("events")
                            .document(eventId)
                            .collection("replacementPool")
                            .document(entrantId);
                    batch.delete(poolRef);

                    // Add to winners
                    DocumentReference winnersRef = db.collection("events")
                            .document(eventId)
                            .collection("winners")
                            .document(entrantId);
                    Map<String, Object> data = new HashMap<>();
                    data.put("deviceId", entrantId);
                    data.put("invitedAt", System.currentTimeMillis());
                    data.put("isReplacement", true);
                    batch.set(winnersRef, data);

                    batch.commit()
                            .addOnSuccessListener(unused -> cb.onSuccess(null))
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Sets the enrolled status of a winner.
     *
     * @param eventId  the event ID
     * @param deviceId the device ID of the entrant
     * @param enrolled true for accepted, false for cancelled
     * @param cb       callback for completion
     */
    public void setEnrolledStatus(String eventId, String deviceId, Boolean enrolled, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(deviceId, "deviceId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        WriteBatch batch = db.batch();

        DocumentReference winnersRef = db.collection("events")
                .document(eventId)
                .collection("winners")
                .document(deviceId);
        batch.delete(winnersRef);

        String targetCollection = enrolled ? "accepted" : "cancelled";
        DocumentReference targetRef = db.collection("events")
                .document(eventId)
                .collection(targetCollection)
                .document(deviceId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", deviceId);
        data.put("respondedAt", System.currentTimeMillis());
        batch.set(targetRef, data);

        batch.commit()
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    public void getWinners(String eventId, Callback<List<Map<String, Object>>> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        db.collection("events").document(eventId)
                .collection("winners")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> winners = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", doc.getId());
                            data.put("invitedAt", doc.get("invitedAt"));
                            winners.add(data);
                        }
                    }
                    cb.onSuccess(winners);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getCancelled(String eventId, Callback<List<Map<String, Object>>> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        db.collection("events").document(eventId)
                .collection("cancelled")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> cancelled = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", doc.getId());
                            data.put("respondedAt", doc.get("respondedAt"));
                            cancelled.add(data);
                        }
                    }
                    cb.onSuccess(cancelled);
                })
                .addOnFailureListener(cb::onError);
    }

    public void getEnrolled(String eventId, Callback<List<Map<String, Object>>> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        db.collection("events").document(eventId)
                .collection("accepted")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> enrolled = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", doc.getId());
                            data.put("respondedAt", doc.get("respondedAt"));
                            enrolled.add(data);
                        }
                    }
                    cb.onSuccess(enrolled);
                })
                .addOnFailureListener(cb::onError);
    }
    
    public void getReplacementPool(String eventId, Callback<List<Map<String, Object>>> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        db.collection("events").document(eventId)
                .collection("replacementPool")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> pool = new ArrayList<>();
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("deviceId", doc.getId());
                            data.put("addedToPoolAt", doc.get("addedToPoolAt"));
                            pool.add(data);
                        }
                    }
                    cb.onSuccess(pool);
                })
                .addOnFailureListener(cb::onError);
    }
    
    public void getReplacementPoolCount(String eventId, Callback<Integer> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        db.collection("events").document(eventId)
                .collection("replacementPool")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot != null ? querySnapshot.size() : 0;
                    cb.onSuccess(count);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Promotes an entrant from the waitlist directly to winners.
     * Used when replacement pool is empty and we need to select from waitlist.
     *
     * @param eventId   the event ID
     * @param entrantId the device ID of the entrant to promote
     * @param cb        callback for completion
     */
    public void promoteFromWaitlist(String eventId, String entrantId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(entrantId, "entrantId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        db.collection("events").document(eventId)
                .collection("waitingList").document(entrantId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) {
                        cb.onError(new IllegalArgumentException("Entrant not in waitlist"));
                        return;
                    }

                    WriteBatch batch = db.batch();

                    DocumentReference waitlistRef = db.collection("events")
                            .document(eventId)
                            .collection("waitingList")
                            .document(entrantId);
                    batch.delete(waitlistRef);

                    // Add to winners
                    DocumentReference winnersRef = db.collection("events")
                            .document(eventId)
                            .collection("winners")
                            .document(entrantId);
                    Map<String, Object> data = new HashMap<>();
                    data.put("deviceId", entrantId);
                    data.put("invitedAt", System.currentTimeMillis());
                    data.put("isReplacement", true);
                    batch.set(winnersRef, data);

                    batch.commit()
                            .addOnSuccessListener(unused -> cb.onSuccess(null))
                            .addOnFailureListener(cb::onError);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Logs a decline and its replacement.
     *
     * @param eventId            the event ID
     * @param declinedEntrantId  the device ID of the entrant who declined
     * @param replacementEntrantId the device ID of the replacement (null if none available)
     * @param source            source of replacement ("replacementPool", "waitlist", or null)
     * @param replacementNotified whether replacement was successfully notified
     * @param cb                callback for completion
     */
    public void logDeclineReplacement(String eventId,
                                      String declinedEntrantId,
                                      String replacementEntrantId,
                                      String source,
                                      boolean replacementNotified,
                                      Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonEmpty(declinedEntrantId, "declinedEntrantId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }

        long declinedAt = System.currentTimeMillis();
        Map<String, Object> logData = new HashMap<>();
        logData.put("declinedEntrantId", declinedEntrantId);
        logData.put("eventId", eventId);
        logData.put("declinedAt", declinedAt);
        logData.put("replacedAt", declinedAt);
        logData.put("replacementNotified", replacementNotified);

        if (replacementEntrantId != null && !replacementEntrantId.isEmpty()) {
            logData.put("replacementEntrantId", replacementEntrantId);
        }
        if (source != null && !source.isEmpty()) {
            logData.put("source", source);
        }

        db.collection("events").document(eventId)
                .collection("declineLogs")
                .add(logData)
                .addOnSuccessListener(unused -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    private Event parseEventFromDocument(DocumentSnapshot doc) {
        try {
            Event event = new Event();
            event.setId(doc.getId());
            event.setName(doc.getString("name"));
            event.setDescription(doc.getString("description"));
            event.setLocation(doc.getString("location"));
            event.setOpen(doc.getBoolean("open") != null && doc.getBoolean("open"));
            event.setOrganizerId(doc.getString("organizerId"));
            event.setQrCode(doc.getString("qrCode"));
            event.setMaxCapacity(doc.get("maxCapacity", Integer.class));

            event.setEventDateTime(convertTimestampToString(doc.get("eventDateTime")));
            event.setRegistrationOpen(convertTimestampToString(doc.get("registrationOpen")));
            event.setRegistrationClose(convertTimestampToString(doc.get("registrationClose")));

            Object tagsObj = doc.get("tags");
            if (tagsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> tags = (List<String>) tagsObj;
                event.setTags(tags);
            } else {
                event.setTags(null);
            }

            // Parse poster URL
            event.setPosterUrl(doc.getString("posterUrl"));

            return event;
        } catch (Exception e) {
            android.util.Log.e("EventDB", "Failed to parse event from document", e);
            return null;
        }
    }

    private String convertTimestampToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) value;
            Date date = timestamp.toDate();
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return isoFormat.format(date);
        }
        if (value instanceof Date) {
            Date date = (Date) value;
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            return isoFormat.format(date);
        }
        return value.toString();
    }

    /**
     * Deletes an event and all its subcollections.
     *
     * @param eventId the event ID to delete
     * @param cb callback for completion
     */
    public void deleteEvent(String eventId, Callback<Void> cb) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            cb.onError(e);
            return;
        }
        
        DocumentReference eventRef = db.collection("events").document(eventId);
        
        String[] subcollections = {
            "waitingList",
            "winners", 
            "accepted",
            "cancelled",
            "replacementPool",
            "declineLogs"
        };
        
        deleteSubcollections(eventRef, subcollections, 0, new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                deleteEventDocument(eventRef, eventId, cb);
            }
            
            @Override
            public void onError(@NonNull Exception e) {
                android.util.Log.w("EventDB", "Some subcollections failed to delete for event: " + eventId, e);
                deleteEventDocument(eventRef, eventId, cb);
            }
        });
    }

    /**
     * Deletes the event document itself.
     */
    private void deleteEventDocument(DocumentReference eventRef, String eventId, Callback<Void> cb) {
        eventRef.delete()
            .addOnSuccessListener(aVoid -> {
                android.util.Log.d("EventDB", "Event deleted: " + eventId);
                cb.onSuccess(null);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("EventDB", "Failed to delete event: " + eventId, e);
                cb.onError(e);
            });
    }

    /**
     * Recursively deletes all documents in subcollections.
     *
     * @param eventRef the event document reference
     * @param subcollectionNames array of subcollection names to delete
     * @param index current index in the array
     * @param cb callback for completion
     */
    private void deleteSubcollections(DocumentReference eventRef, String[] subcollectionNames, 
                                      int index, Callback<Void> cb) {
        if (index >= subcollectionNames.length) {
            cb.onSuccess(null);
            return;
        }
        
        String subcollectionName = subcollectionNames[index];
        eventRef.collection(subcollectionName)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                if (querySnapshot == null || querySnapshot.isEmpty()) {
                    deleteSubcollections(eventRef, subcollectionNames, index + 1, cb);
                    return;
                }
                
                List<QueryDocumentSnapshot> docs = new ArrayList<>();
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    docs.add(doc);
                }
                
                deleteDocumentsInBatches(eventRef, subcollectionName, docs, 0, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void value) {
                        deleteSubcollections(eventRef, subcollectionNames, index + 1, cb);
                    }
                    
                    @Override
                    public void onError(@NonNull Exception e) {
                        android.util.Log.w("EventDB", "Failed to delete subcollection: " + subcollectionName, e);
                        deleteSubcollections(eventRef, subcollectionNames, index + 1, cb);
                    }
                });
            })
            .addOnFailureListener(e -> {
                android.util.Log.w("EventDB", "Failed to query subcollection: " + subcollectionName, e);
                deleteSubcollections(eventRef, subcollectionNames, index + 1, cb);
            });
    }

    /**
     * Deletes documents in batches.
     *
     * @param eventRef the event document reference
     * @param subcollectionName name of the subcollection
     * @param docs list of documents to delete
     * @param batchIndex current batch index
     * @param cb callback for completion
     */
    private void deleteDocumentsInBatches(DocumentReference eventRef, String subcollectionName,
                                         List<QueryDocumentSnapshot> docs, int batchIndex,
                                         Callback<Void> cb) {
        if (docs.isEmpty() || batchIndex * BATCH_SIZE >= docs.size()) {
            cb.onSuccess(null);
            return;
        }
        
        int startIndex = batchIndex * BATCH_SIZE;
        int endIndex = Math.min(startIndex + BATCH_SIZE, docs.size());
        WriteBatch batch = db.batch();
        
        for (int i = startIndex; i < endIndex; i++) {
            batch.delete(docs.get(i).getReference());
        }
        
        batch.commit()
            .addOnSuccessListener(aVoid -> deleteDocumentsInBatches(eventRef, subcollectionName, docs, batchIndex + 1, cb))
            .addOnFailureListener(e -> {
                android.util.Log.w("EventDB", "Failed to delete batch " + (batchIndex + 1) + " of subcollection: " + subcollectionName, e);
                deleteDocumentsInBatches(eventRef, subcollectionName, docs, batchIndex + 1, cb);
            });
    }
}