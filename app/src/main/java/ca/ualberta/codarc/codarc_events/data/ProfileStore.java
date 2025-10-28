package ca.ualberta.codarc.codarc_events.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class ProfileStore {

    private static final String PREF_NAME = "entrant_profile";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_JOINED_EVENTS = "joined_events";

    private final SharedPreferences sp;

    public ProfileStore(Context ctx) {
        sp = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ✅ Check if user has filled all required fields
    public boolean isProfileComplete() {
        String name = sp.getString(KEY_NAME, "");
        String email = sp.getString(KEY_EMAIL, "");
        String phone = sp.getString(KEY_PHONE, "");
        return !name.isEmpty() && !email.isEmpty() && !phone.isEmpty();
    }

    // ✅ Save profile values
    public void saveProfile(String name, String email, String phone) {
        sp.edit()
                .putString(KEY_NAME, name)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PHONE, phone)
                .apply();
    }

    // ✅ Load Profile Data
    public String getName() {
        return sp.getString(KEY_NAME, "");
    }

    public String getEmail() {
        return sp.getString(KEY_EMAIL, "");
    }

    public String getPhone() {
        return sp.getString(KEY_PHONE, "");
    }

    // ✅ Get joined events list
    public Set<String> getJoinedEvents() {
        return new HashSet<>(sp.getStringSet(KEY_JOINED_EVENTS, new HashSet<>()));
    }

    // ✅ Store joined events list
    public void setJoinedEvents(Set<String> joined) {
        sp.edit().putStringSet(KEY_JOINED_EVENTS, joined).apply();
    }
    public void clearProfile() {
        sp.edit().clear().apply();
    }

}
