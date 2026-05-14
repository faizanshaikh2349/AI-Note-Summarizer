package com.example.ainotessummarizer.session;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager handles all user session state using SharedPreferences.
 *
 * Architecture Note:
 * This is the single source of truth for session state. Activities should
 * NEVER read SharedPreferences directly for auth state — they must use this
 * class. This decouples session logic from UI and makes it testable.
 *
 * Security Note:
 * Session data is stored in a private SharedPreferences file (MODE_PRIVATE),
 * accessible only by this app. A session expiry mechanism prevents indefinite
 * sessions — users are logged out after SESSION_EXPIRY_MS of inactivity.
 */
public class SessionManager {

    // Preferences file name (app-private)
    private static final String PREFS_NAME = "ai_notes_session_prefs";

    // Preference keys
    private static final String KEY_EMAIL = "session_email";
    private static final String KEY_LOGIN_TIME = "session_login_time";
    private static final String KEY_LAST_ACTIVE = "session_last_active";

    // Session expiry: 7 days in milliseconds
    private static final long SESSION_EXPIRY_MS = 7L * 24 * 60 * 60 * 1000;

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        // Use application context to avoid memory leaks from Activity references
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Saves a new user session after successful login.
     * Stores the email and current timestamp as the session start time.
     *
     * @param email The authenticated user's email address.
     */
    public void saveSession(String email) {
        long now = System.currentTimeMillis();
        prefs.edit()
                .putString(KEY_EMAIL, email)
                .putLong(KEY_LOGIN_TIME, now)
                .putLong(KEY_LAST_ACTIVE, now)
                .apply();
    }

    /**
     * Checks whether a valid (non-expired) session exists.
     *
     * Session Expiry Logic:
     * A session is valid if:
     * 1. An email is stored (user has logged in before), AND
     * 2. The last activity was within SESSION_EXPIRY_MS
     *
     * @return true if session is valid, false if expired or absent.
     */
    public boolean isSessionValid() {
        String email = prefs.getString(KEY_EMAIL, null);
        if (email == null || email.isEmpty())
            return false;

        long lastActive = prefs.getLong(KEY_LAST_ACTIVE, 0L);
        long elapsed = System.currentTimeMillis() - lastActive;
        return elapsed < SESSION_EXPIRY_MS;
    }

    /**
     * Returns the email address of the currently logged-in user.
     *
     * @return User email string, or null if no session exists.
     */
    public String getUserEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    /**
     * Updates the last active timestamp for the session.
     * Call this periodically (e.g., on each significant user action)
     * to keep the session alive.
     */
    public void updateLastActive() {
        prefs.edit()
                .putLong(KEY_LAST_ACTIVE, System.currentTimeMillis())
                .apply();
    }

    /**
     * Clears all session data, effectively logging the user out.
     * After this call, isSessionValid() will return false.
     */
    public void clearSession() {
        prefs.edit().clear().apply();
    }

    /**
     * Returns the timestamp when the session was created (login time).
     *
     * @return Unix timestamp in milliseconds, or 0 if not set.
     */
    public long getLoginTime() {
        return prefs.getLong(KEY_LOGIN_TIME, 0L);
    }
}
