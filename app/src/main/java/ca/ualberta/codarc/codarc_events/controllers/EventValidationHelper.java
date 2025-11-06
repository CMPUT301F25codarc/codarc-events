package ca.ualberta.codarc.codarc_events.controllers;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Locale;

import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Validation helper for events.
 */
public class EventValidationHelper {

    private static final String TAG = "EventValidationHelper";

    // Check if current time is within registration window
    public static boolean isWithinRegistrationWindow(Event event) {
        if (event == null) {
            return false;
        }

        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            long now = System.currentTimeMillis();

            String regOpen = event.getRegistrationOpen();
            String regClose = event.getRegistrationClose();

            if (regOpen == null || regClose == null || regOpen.isEmpty() || regClose.isEmpty()) {
                return false;
            }

            long openTime = isoFormat.parse(regOpen).getTime();
            long closeTime = isoFormat.parse(regClose).getTime();

            return now >= openTime && now <= closeTime;
        } catch (java.text.ParseException e) {
            Log.e(TAG, "Error parsing registration window", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in registration window check", e);
            return false;
        }
    }

    // Check if event has capacity (returns true if no limit set)
    public static boolean hasCapacity(Event event, int currentWaitlistCount) {
        if (event == null) {
            return false;
        }

        Integer maxCapacity = event.getMaxCapacity();
        if (maxCapacity == null || maxCapacity <= 0) {
            // No capacity limit set
            return true;
        }

        return currentWaitlistCount < maxCapacity;
    }
}
