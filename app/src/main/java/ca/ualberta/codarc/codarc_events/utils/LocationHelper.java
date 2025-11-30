package ca.ualberta.codarc.codarc_events.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Helper for requesting location permission and getting current location.
 */
public class LocationHelper {
    
    private static final String TAG = "LocationHelper";
    
    /**
     * Checks if location permission is granted.
     *
     * @param context the context
     * @return true if permission is granted
     */
    public static boolean hasLocationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
    
    /**
     * Gets the current location.
     * First tries to get last known location, then requests a fresh update if needed.
     *
     * @param context the context
     * @param callback callback with location (may be null if unavailable)
     */
    @SuppressWarnings("MissingPermission")
    public static void getCurrentLocation(Context context, LocationCallback callback) {
        if (!hasLocationPermission(context)) {
            callback.onLocation(null);
            return;
        }
        
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(context);
        
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                callback.onLocation(location);
            } else {
                Log.d(TAG, "Last location is null, requesting fresh location");
                requestFreshLocation(locationClient, callback);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get last location", e);
            requestFreshLocation(locationClient, callback);
        });
    }

    @SuppressWarnings("MissingPermission")
    private static void requestFreshLocation(FusedLocationProviderClient locationClient, LocationCallback callback) {
        CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(5000)
                .build();
        
        locationClient.getCurrentLocation(currentLocationRequest, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callback.onLocation(location);
                    } else {
                        Log.d(TAG, "Fresh location is null");
                        callback.onLocation(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get fresh location", e);
                    callback.onLocation(null);
                });
    }
    
    public interface LocationCallback {
        void onLocation(Location location);
    }
}
