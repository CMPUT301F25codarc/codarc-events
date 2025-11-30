package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import ca.ualberta.codarc.codarc_events.R;
import ca.ualberta.codarc.codarc_events.controllers.EntrantMapController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Displays a map showing where entrants joined the waitlist from.
 * Organizer-only view.
 */
public class EntrantMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    
    private static final String TAG = "EntrantMapActivity";
    
    private GoogleMap map;
    private EntrantMapController controller;
    private String eventId;
    private String deviceId;
    private TextView emptyState;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrant_map);
        
        eventId = getIntent().getStringExtra("eventId");
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(this, "Event ID required", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        deviceId = Identity.getOrCreateDeviceId(this);
        emptyState = findViewById(R.id.tv_empty_state);
        
        if (emptyState != null) {
            emptyState.setVisibility(View.GONE);
        }
        
        verifyOrganizerAccess();
        
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment not found!");
        }
        
        controller = new EntrantMapController(new EventDB(), new EntrantDB());
    }
    
    private void verifyOrganizerAccess() {
        EventDB eventDB = new EventDB();
        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event == null || event.getOrganizerId() == null || !deviceId.equals(event.getOrganizerId())) {
                    runOnUiThread(() -> {
                        Toast.makeText(EntrantMapActivity.this, 
                            getString(R.string.map_organizer_only), Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }
            
            @Override
            public void onError(@NonNull Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(EntrantMapActivity.this, 
                        getString(R.string.map_load_error), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        Log.d(TAG, "Map is ready, loading markers for eventId: " + eventId);
        
        if (map != null) {
            map.getUiSettings().setZoomControlsEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(false);
        }
        
        loadMapMarkers();
    }
    
    private void loadMapMarkers() {
        Log.d(TAG, "Loading map markers...");
        controller.loadMapData(eventId, new EntrantMapController.MapDataCallback() {
            @Override
            public void onSuccess(List<EntrantMapController.MapMarkerData> markers) {
                Log.d(TAG, "Map data loaded, marker count: " + (markers != null ? markers.size() : 0));
                if (markers == null || markers.isEmpty()) {
                    Log.d(TAG, "No markers found, showing empty state");
                    showEmptyState();
                    return;
                }
                
                Log.d(TAG, "Adding " + markers.size() + " markers to map");
                addMarkersToMap(markers);
                fitMapToMarkers(markers);
                hideEmptyState();
            }
            
            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to load map data", e);
                Toast.makeText(EntrantMapActivity.this, 
                    getString(R.string.map_load_error), Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }
    
    private void addMarkersToMap(List<EntrantMapController.MapMarkerData> markers) {
        if (map == null) {
            Log.w(TAG, "Map is null, cannot add markers");
            return;
        }
        
        for (EntrantMapController.MapMarkerData marker : markers) {
            LatLng position = new LatLng(marker.getLatitude(), marker.getLongitude());
            String title = marker.getEntrantName();
            String snippet = formatJoinTime(marker.getJoinedAt());
            
            com.google.android.gms.maps.model.Marker mapMarker = map.addMarker(new MarkerOptions()
                    .position(position)
                    .title(title)
                    .snippet(snippet));
            
            if (mapMarker != null) {
                Log.d(TAG, "Added marker at " + position.latitude + ", " + position.longitude + " for " + title);
            } else {
                Log.w(TAG, "Failed to add marker for " + title);
            }
        }
    }
    
    private void fitMapToMarkers(List<EntrantMapController.MapMarkerData> markers) {
        if (map == null || markers.isEmpty()) {
            return;
        }
        
        if (markers.size() == 1) {
            EntrantMapController.MapMarkerData marker = markers.get(0);
            LatLng position = new LatLng(marker.getLatitude(), marker.getLongitude());
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15.0f));
            Log.d(TAG, "Single marker: centering on " + position.latitude + ", " + position.longitude);
        } else {
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (EntrantMapController.MapMarkerData marker : markers) {
                boundsBuilder.include(new LatLng(marker.getLatitude(), marker.getLongitude()));
            }
            
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 100;
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
            Log.d(TAG, "Multiple markers: fitting bounds with padding " + padding);
        }
    }
    
    private String formatJoinTime(long timestamp) {
        if (timestamp <= 0) {
            return "Unknown time";
        }
        return DateFormat.getDateTimeInstance().format(new Date(timestamp));
    }
    
    private void showEmptyState() {
        Log.d(TAG, "Showing empty state");
        runOnUiThread(() -> {
            if (emptyState != null) {
                emptyState.setVisibility(View.VISIBLE);
            }
            if (map != null) {
                View mapView = ((SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map_fragment)).getView();
                if (mapView != null) {
                    mapView.setVisibility(View.GONE);
                }
            }
        });
    }
    
    private void hideEmptyState() {
        Log.d(TAG, "Hiding empty state");
        runOnUiThread(() -> {
            if (emptyState != null) {
                emptyState.setVisibility(View.GONE);
            }
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map_fragment);
            if (mapFragment != null) {
                View mapView = mapFragment.getView();
                if (mapView != null) {
                    mapView.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Map view set to VISIBLE");
                } else {
                    Log.w(TAG, "Map fragment view is null");
                }
            } else {
                Log.w(TAG, "Map fragment is null");
            }
        });
    }
}

