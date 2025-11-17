package ca.ualberta.codarc.codarc_events.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateHelper {

    public static String formatEventDate(String rawTimeDate) {
        if (rawTimeDate == null || rawTimeDate.isEmpty()) return "";

        try {
            SimpleDateFormat input =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

            SimpleDateFormat output =
                    new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());

            Date date = input.parse(rawTimeDate);
            return output.format(date);

        } catch (Exception e) {
            return rawTimeDate; // fallback if it doesn't work
        }
    }
}


