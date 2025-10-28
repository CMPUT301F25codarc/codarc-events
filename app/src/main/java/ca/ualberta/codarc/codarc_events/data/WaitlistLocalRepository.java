package ca.ualberta.codarc.codarc_events.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Local-only implementation of waitlist storage.
 * Later you will replace this with Firestore.
 */
public class WaitlistLocalRepository {

    private static final String PREF_NAME = "waitlist_prefs";
    private static final String KEY_WAITLIST = "waitlist_events";

    private final SharedPreferences prefs;

    public WaitlistLocalRepository(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ✅ Check if already in waitlist
    public boolean isInWaitlist(String eventId) {
        Set<String> set = prefs.getStringSet(KEY_WAITLIST, new HashSet<>());
        return set.contains(eventId);
    }

    // ✅ Add to waitlist
    public void joinWaitlist(String eventId) {
        Set<String> set = prefs.getStringSet(KEY_WAITLIST, new HashSet<>());
        set = new HashSet<>(set); // must copy before editing
        set.add(eventId);
        prefs.edit().putStringSet(KEY_WAITLIST, set).apply();
    }

    // ✅ Remove from waitlist
    public void leaveWaitlist(String eventId) {
        Set<String> set = prefs.getStringSet(KEY_WAITLIST, new HashSet<>());
        set = new HashSet<>(set);
        set.remove(eventId);
        prefs.edit().putStringSet(KEY_WAITLIST, set).apply();
    }
}
