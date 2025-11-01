package ca.ualberta.codarc.codarc_events.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Generates and stores a stable device-scoped identifier.
 *
 * We keep this extremely simple for the project: a UUID persisted in
 * SharedPreferences. The value is used as the Firestore profile document id.
 */
public class Identity {

    private static final String PREFS_NAME = "codarc_identity_prefs";
    private static final String KEY_DEVICE_ID = "device_id";

    /**
     * Returns a cached device id, generating and persisting one on first run.
     */
    public static String getOrCreateDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String existing = prefs.getString(KEY_DEVICE_ID, null);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply();
        return generated;
    }
}