package ca.ualberta.codarc.codarc_events.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Small helpers for date/time formatting.
 * Handles Firestore Timestamp, Long, and String formats.
 */
public class DateTimeUtils {

    /**
     * Formats a timestamp for display.
     *
     * @param timeObj the timestamp object (Timestamp, Long, or String)
     * @return formatted string or "Time unknown"
     */
    public static String formatTimestamp(Object timeObj) {
        if (timeObj == null) {
            return "Time unknown";
        }

        long timeMillis = extractTimeMillis(timeObj);
        if (timeMillis == Long.MAX_VALUE) {
            return "Time unknown";
        }

        SimpleDateFormat format = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm a z", Locale.US);
        return format.format(new Date(timeMillis));
    }

    /**
     * Formats a timestamp for short display.
     *
     * @param timeObj the timestamp object
     * @return formatted string or "Time unknown"
     */
    public static String formatTimestampShort(Object timeObj) {
        if (timeObj == null) {
            return "Time unknown";
        }

        long timeMillis = extractTimeMillis(timeObj);
        if (timeMillis == Long.MAX_VALUE) {
            return "Time unknown";
        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        return format.format(new Date(timeMillis));
    }

    /**
     * Extracts milliseconds from a timestamp object.
     *
     * @param timeObj the timestamp object
     * @return milliseconds since epoch, or Long.MAX_VALUE if invalid
     */
    public static long extractTimeMillis(Object timeObj) {
        if (timeObj == null) {
            return Long.MAX_VALUE;
        }

        if (timeObj instanceof Long) {
            return (Long) timeObj;
        }

        if (timeObj instanceof Date) {
            return ((Date) timeObj).getTime();
        }

        if (timeObj instanceof String) {
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                return isoFormat.parse((String) timeObj).getTime();
            } catch (Exception e) {
                return Long.MAX_VALUE;
            }
        }

        // Handle Firestore Timestamp via reflection
        try {
            String className = timeObj.getClass().getName();
            if (className.contains("Timestamp")) {
                java.lang.reflect.Method toDateMethod = timeObj.getClass().getMethod("toDate");
                Date date = (Date) toDateMethod.invoke(timeObj);
                return date.getTime();
            }
        } catch (Exception e) {
            // Couldn't extract timestamp
        }

        return Long.MAX_VALUE;
    }

    /**
     * Compares two timestamp objects.
     *
     * @param timeA first timestamp
     * @param timeB second timestamp
     * @return negative if timeA < timeB, positive if timeA > timeB, 0 if equal
     */
    public static int compareTimestamps(Object timeA, Object timeB) {
        long millisA = extractTimeMillis(timeA);
        long millisB = extractTimeMillis(timeB);
        return Long.compare(millisA, millisB);
    }
}

