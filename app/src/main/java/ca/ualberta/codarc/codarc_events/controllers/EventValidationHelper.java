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

    /**
     * Checks if current time is within registration window.
     *
     * @param event the event to check
     * @return true if within registration window, false otherwise
     */
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

    /**
     * Checks if event has capacity based on accepted participants.
     *
     * @param event the event to check
     * @param acceptedCount the number of accepted participants
     * @return true if event has capacity available, false otherwise
     */
    public static boolean hasCapacity(Event event, int acceptedCount) {
        if (event == null) {
            return false;
        }

        Integer maxCapacity = event.getMaxCapacity();
        if (maxCapacity == null || maxCapacity <= 0) {
            return true;
        }

        return acceptedCount < maxCapacity;
    }

    /**
     * Checks if waitlist has capacity available.
     *
     * @param event the event to check
     * @param waitlistCount the current number of entrants on the waitlist
     * @return true if waitlist has capacity available, false otherwise
     */
    public static boolean hasWaitlistCapacity(Event event, int waitlistCount) {
        if (event == null) {
            return false;
        }

        Integer maxCapacity = event.getMaxCapacity();
        if (maxCapacity == null || maxCapacity <= 0) {
            return true;
        }

        return waitlistCount < maxCapacity;
    }

    /**
     * Checks if registration deadline has passed.
     * Assumes event has registration close time (enforced in CreateEventController).
     *
     * @param event the event to check
     * @return true if registration deadline has passed, false otherwise
     */
    public static boolean hasRegistrationDeadlinePassed(Event event) {
        if (event == null) {
            return false;
        }

        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            long now = System.currentTimeMillis();
            String regClose = event.getRegistrationClose();

            if (regClose == null || regClose.isEmpty()) {
                Log.w(TAG, "Event missing registration close time: " + event.getId());
                return false;
            }

            long closeTime = isoFormat.parse(regClose).getTime();
            return now > closeTime;
        } catch (java.text.ParseException e) {
            Log.e(TAG, "Error parsing registration close time", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error checking registration deadline", e);
            return false;
        }
    }
}
