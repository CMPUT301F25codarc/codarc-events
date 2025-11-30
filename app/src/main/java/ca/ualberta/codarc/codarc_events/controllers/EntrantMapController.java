package ca.ualberta.codarc.codarc_events.controllers;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.firestore.GeoPoint;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles preparation of entrant location data for map display.
 */
public class EntrantMapController {
    
    private static final String TAG = "EntrantMapController";
    
    private final EventDB eventDB;
    private final EntrantDB entrantDB;
    
    public interface MapDataCallback {
        void onSuccess(List<MapMarkerData> markers);
        void onError(@NonNull Exception e);
    }
    
    public static class MapMarkerData {
        private final String deviceId;
        private final String entrantName;
        private final double latitude;
        private final double longitude;
        private final long joinedAt;
        
        public MapMarkerData(String deviceId, String entrantName, 
                           double latitude, double longitude, long joinedAt) {
            this.deviceId = deviceId;
            this.entrantName = entrantName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.joinedAt = joinedAt;
        }
        
        public String getDeviceId() { return deviceId; }
        public String getEntrantName() { return entrantName; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public long getJoinedAt() { return joinedAt; }
    }
    
    public EntrantMapController(EventDB eventDB, EntrantDB entrantDB) {
        this.eventDB = eventDB;
        this.entrantDB = entrantDB;
    }
    
    public void loadMapData(String eventId, MapDataCallback callback) {
        Log.d(TAG, "Loading map data for eventId: " + eventId);
        eventDB.getEntrantsWithLocations(eventId, new EventDB.Callback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> entries) {
                Log.d(TAG, "getEntrantsWithLocations returned " + (entries != null ? entries.size() : 0) + " entries");
                if (entries == null || entries.isEmpty()) {
                    Log.d(TAG, "No entries found, returning empty list");
                    callback.onSuccess(new ArrayList<>());
                    return;
                }
                Log.d(TAG, "Resolving entrant names for " + entries.size() + " entries");
                resolveEntrantNames(entries, callback);
            }
            
            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Error loading entrants with locations", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Resolves entrant names by fetching profiles in parallel and aggregating results.
     * 
     * Note: The async orchestration for coordinating multiple parallel profile fetches and
     * thread-safe aggregation using NameResolutionAggregator was implemented with assistance
     * from Claude Sonnet 4.5 (Anthropic). The coordination pattern ensures all profile lookups
     * complete before returning marker data.
     */
    private void resolveEntrantNames(List<Map<String, Object>> entries, MapDataCallback callback) {
        List<MapMarkerData> markers = Collections.synchronizedList(new ArrayList<>());
        NameResolutionAggregator aggregator = new NameResolutionAggregator(entries.size(), markers, callback);
        
        for (Map<String, Object> entry : entries) {
            String deviceId = (String) entry.get("deviceId");
            GeoPoint location = (GeoPoint) entry.get("joinLocation");
            Object timestampObj = entry.get("timestamp");
            
            if (deviceId == null || location == null) {
                aggregator.onNameResolved();
                continue;
            }
            
            long timestamp = parseTimestamp(timestampObj);
            
            entrantDB.getProfile(deviceId, new EntrantDB.Callback<Entrant>() {
                @Override
                public void onSuccess(Entrant entrant) {
                    String name = deviceId;
                    if (entrant != null && entrant.getName() != null && !entrant.getName().isEmpty()) {
                        name = entrant.getName();
                    }
                    
                    MapMarkerData marker = new MapMarkerData(deviceId, name, 
                            location.getLatitude(), location.getLongitude(), timestamp);
                    markers.add(marker);
                    aggregator.onNameResolved();
                }
                
                @Override
                public void onError(@NonNull Exception e) {
                    Log.w(TAG, "Failed to resolve name for " + deviceId, e);
                    MapMarkerData marker = new MapMarkerData(deviceId, deviceId, 
                            location.getLatitude(), location.getLongitude(), timestamp);
                    markers.add(marker);
                    aggregator.onNameResolved();
                }
            });
        }
    }
    
    private long parseTimestamp(Object timestampObj) {
        if (timestampObj == null) {
            return 0;
        }
        if (timestampObj instanceof Long) {
            return (Long) timestampObj;
        }
        if (timestampObj instanceof Number) {
            return ((Number) timestampObj).longValue();
        }
        if (timestampObj instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
        }
        if (timestampObj instanceof java.util.Date) {
            return ((java.util.Date) timestampObj).getTime();
        }
        return 0;
    }
    
    private static class NameResolutionAggregator {
        private final int total;
        private final List<MapMarkerData> markers;
        private final MapDataCallback callback;
        private int completed;
        
        NameResolutionAggregator(int total, List<MapMarkerData> markers, MapDataCallback callback) {
            this.total = total;
            this.markers = markers;
            this.callback = callback;
            this.completed = 0;
        }
        
        synchronized void onNameResolved() {
            completed++;
            if (completed >= total) {
                callback.onSuccess(markers);
            }
        }
    }
}

