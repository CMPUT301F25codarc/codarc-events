package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.HistoryItem;
import ca.ualberta.codarc.codarc_events.utils.DateHelper;

public class HistoryController {

    private final EventDB eventDB;

    public HistoryController(EventDB eventDB) {
        this.eventDB = eventDB;
    }

    public interface StatusCallback {
        void onStatus(String status);
    }

    public void loadStatus(String eventId, String deviceId, StatusCallback callback) {
        eventDB.getWinners(eventId, new EventDB.Callback<List<HashMap<String, Object>>>() {
            @Override
            public void onSuccess(List<HashMap<String, Object>> winners) {
                eventDB.getEnrolled(eventId, new EventDB.Callback<List<HashMap<String, Object>>>() {
                    @Override
                    public void onSuccess(List<HashMap<String, Object>> enrolled) {
                        eventDB.getCancelled(eventId, new EventDB.Callback<List<HashMap<String, Object>>>() {
                            @Override
                            public void onSuccess(List<HashMap<String, Object>> cancelled) {
                                eventDB.getWaitlist(eventId, new EventDB.Callback<List<HashMap<String, Object>>>() {
                                    @Override
                                    public void onSuccess(List<HashMap<String, Object>> waitlist) {
                                        String status = determineStatus(winners, enrolled, cancelled, waitlist, deviceId);
                                        callback.onStatus(status);
                                    }

                                    @Override
                                    public void onError(@NonNull Exception e) {
                                        callback.onStatus("unknown");
                                    }
                                });
                            }

                            @Override
                            public void onError(@NonNull Exception e) {
                                callback.onStatus("unknown");
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        callback.onStatus("unknown");
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onStatus("unknown");
            }
        });
    }

    private String determineStatus(
            List<Map<String, Object>> winners,
            List<Map<String, Object>> enrolled,
            List<Map<String, Object>> cancelled,
            List<Map<String, Object>> waitlist,
            String deviceId
    ) {
        if (containsDevice(winners, deviceId)) return "winner";
        if (containsDevice(enrolled, deviceId)) return "enrolled";
        if (containsDevice(cancelled, deviceId)) return "cancelled";
        if (containsDevice(waitlist, deviceId)) return "waitlisted";
        return "none";
    }

    private boolean containsDevice(List<Map<String, Object>> list, String deviceId) {
        if (list == null || deviceId == null) return false;
        for (Map<String, Object> entry : list) {
            Object id = entry.get("deviceId");
            if (deviceId.equals(id)) return true;
        }
        return false;
    }
}