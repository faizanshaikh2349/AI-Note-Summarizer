package com.example.ainotessummarizer.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.ainotessummarizer.DatabaseHelper;
import com.example.ainotessummarizer.User;
import com.example.ainotessummarizer.security.PasswordUtils;

/**
 * AuthRepository abstracts all authentication-related database operations.
 *
 * Architecture Note (MVVM):
 * This is the "Repository" layer — the single source of truth for auth data.
 * ViewModels call this class; Activities/Fragments must NOT call DatabaseHelper
 * directly for auth operations. This makes the code testable and separates
 * concerns properly.
 *
 * Security Note:
 * All queries use parameterized statements (no SQL injection risk).
 * Passwords are never stored or returned in plain text.
 * Cursors are always closed in finally blocks to prevent memory leaks.
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";

    private final DatabaseHelper dbHelper;

    public AuthRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    // ==================== REGISTRATION ====================

    /**
     * Registers a new user in the database.
     * Password is hashed with a unique salt before storage.
     *
     * @param email    User's email address.
     * @param password Plain-text password (will be hashed immediately).
     * @return true if registration succeeded, false if email already exists or DB
     *         error.
     */
    public boolean register(String email, String password) {
        if (isEmailExists(email)) {
            Log.w(TAG, "Registration failed: email already exists");
            return false;
        }

        String hashedPassword = PasswordUtils.hashWithSalt(password);
        long now = System.currentTimeMillis();

        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_EMAIL, email);
        cv.put(DatabaseHelper.COL_PASSWORD, hashedPassword);
        cv.put(DatabaseHelper.COL_CREATED_AT, now);
        cv.put(DatabaseHelper.COL_UPDATED_AT, now);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            return db.insert(DatabaseHelper.TABLE_USERS, null, cv) != -1;
        } catch (Exception e) {
            Log.e(TAG, "Error registering user", e);
            return false;
        }
    }

    /**
     * Checks if an email is already registered in the database.
     *
     * @param email The email to check.
     * @return true if email exists and account is not deleted.
     */
    public boolean isEmailExists(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_USERS,
                    new String[] { "1" },
                    DatabaseHelper.COL_EMAIL + "=? AND " + DatabaseHelper.COL_IS_DELETED + "=0",
                    new String[] { email },
                    null, null, null, "1");
            return cursor.moveToFirst();
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    // ==================== LOGIN / AUTH ====================

    /**
     * Verifies login credentials.
     * Retrieves stored salted hash and verifies the plain-text password against it.
     *
     * @param email    User's email address.
     * @param password Plain-text password to verify.
     * @return true if credentials match, false otherwise.
     */
    public boolean verifyCredentials(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_USERS,
                    new String[] { DatabaseHelper.COL_PASSWORD },
                    DatabaseHelper.COL_EMAIL + "=? AND " + DatabaseHelper.COL_IS_DELETED + "=0",
                    new String[] { email },
                    null, null, null);

            if (!cursor.moveToFirst())
                return false;

            String storedHash = cursor.getString(0);
            return PasswordUtils.verify(password, storedHash);

        } catch (Exception e) {
            Log.e(TAG, "Error verifying credentials", e);
            return false;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    /**
     * Updates the last_login timestamp for a user upon successful login.
     *
     * @param email The user's email.
     */
    public void updateLastLogin(String email) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_LAST_LOGIN, System.currentTimeMillis());
        cv.put(DatabaseHelper.COL_UPDATED_AT, System.currentTimeMillis());
        db.update(DatabaseHelper.TABLE_USERS, cv,
                DatabaseHelper.COL_EMAIL + "=?", new String[] { email });
    }

    // ==================== ACCOUNT LOCK ====================

    /**
     * Returns the number of consecutive failed login attempts for a user.
     *
     * @param email The user's email.
     * @return Number of failed attempts, or 0 if user not found.
     */
    public int getFailedAttempts(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_USERS,
                    new String[] { DatabaseHelper.COL_FAILED_ATTEMPTS },
                    DatabaseHelper.COL_EMAIL + "=?",
                    new String[] { email },
                    null, null, null);
            if (cursor.moveToFirst())
                return cursor.getInt(0);
            return 0;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    /**
     * Increments the failed login attempt counter for a user by 1.
     *
     * @param email The user's email.
     */
    public void incrementFailedAttempts(String email) {
        int current = getFailedAttempts(email);
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_FAILED_ATTEMPTS, current + 1);
        cv.put(DatabaseHelper.COL_UPDATED_AT, System.currentTimeMillis());
        dbHelper.getWritableDatabase().update(
                DatabaseHelper.TABLE_USERS, cv,
                DatabaseHelper.COL_EMAIL + "=?", new String[] { email });
    }

    /**
     * Resets failed attempts counter and clears lock time after successful login.
     *
     * @param email The user's email.
     */
    public void resetFailedAttempts(String email) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_FAILED_ATTEMPTS, 0);
        cv.put(DatabaseHelper.COL_LOCK_TIME, 0);
        cv.put(DatabaseHelper.COL_UPDATED_AT, System.currentTimeMillis());
        dbHelper.getWritableDatabase().update(
                DatabaseHelper.TABLE_USERS, cv,
                DatabaseHelper.COL_EMAIL + "=?", new String[] { email });
    }

    /**
     * Stores the current timestamp as the lock start time for a user.
     * Called when max failed attempts are reached.
     *
     * @param email The user's email.
     */
    public void setLockTime(String email) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_LOCK_TIME, System.currentTimeMillis());
        cv.put(DatabaseHelper.COL_UPDATED_AT, System.currentTimeMillis());
        dbHelper.getWritableDatabase().update(
                DatabaseHelper.TABLE_USERS, cv,
                DatabaseHelper.COL_EMAIL + "=?", new String[] { email });
    }

    /**
     * Calculates the remaining lock duration for a user.
     * Returns 0 if the lock has expired or no lock is active.
     *
     * @param email          The user's email.
     * @param lockDurationMs The total lock duration in milliseconds.
     * @return Remaining milliseconds of lock, or 0 if unlocked.
     */
    public long getRemainingLockTime(String email, long lockDurationMs) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_USERS,
                    new String[] { DatabaseHelper.COL_LOCK_TIME },
                    DatabaseHelper.COL_EMAIL + "=?",
                    new String[] { email },
                    null, null, null);
            if (cursor.moveToFirst()) {
                long lockTime = cursor.getLong(0);
                if (lockTime > 0) {
                    long elapsed = System.currentTimeMillis() - lockTime;
                    return Math.max(0, lockDurationMs - elapsed);
                }
            }
            return 0;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    // ==================== USER DATA ====================

    /**
     * Retrieves a User object for a given email.
     *
     * @param email The user's email.
     * @return A populated {@link User}, or null if not found.
     */
    public User getUserByEmail(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    DatabaseHelper.TABLE_USERS,
                    new String[] {
                            DatabaseHelper.COL_ID,
                            DatabaseHelper.COL_EMAIL,
                            DatabaseHelper.COL_CREATED_AT,
                            DatabaseHelper.COL_LAST_LOGIN
                    },
                    DatabaseHelper.COL_EMAIL + "=? AND " + DatabaseHelper.COL_IS_DELETED + "=0",
                    new String[] { email },
                    null, null, null);
            if (cursor.moveToFirst()) {
                return new User(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getLong(3));
            }
            return null;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    /**
     * Close the database helper when done to release resources.
     * Call this in onDestroy() of the ViewModel.
     */
    public void close() {
        dbHelper.close();
    }
}
