package com.example.ainotessummarizer;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * SQLiteOpenHelper subclass responsible for database creation and version
 * migration.
 *
 * Architecture Note:
 * This class only handles schema management (create/upgrade).
 * All business-logic queries are in AuthRepository, not here.
 * Activities should never interact with DatabaseHelper directly.
 *
 * Migration Strategy:
 * onUpgrade() uses ALTER TABLE to add new columns, preserving existing data.
 * This prevents data loss when users update the app.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "SecureUserDB.db";

    // Bump version when schema changes — triggers onUpgrade()
    private static final int DATABASE_VERSION = 8;

    // ---- Table & Column names ----
    public static final String TABLE_USERS = "users";
    public static final String COL_ID = "id";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password_hash";
    public static final String COL_IS_LOGGED_IN = "is_logged_in";
    public static final String COL_IS_DELETED = "is_deleted";
    public static final String COL_FAILED_ATTEMPTS = "failed_attempts";
    public static final String COL_LOCK_TIME = "lock_time";
    public static final String COL_LAST_ACTIVE = "last_active";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_UPDATED_AT = "updated_at"; // NEW in v7
    public static final String COL_LAST_LOGIN = "last_login"; // NEW in v7
    public static final String TABLE_QUESTION_HISTORY = "question_history";
    public static final String COL_HISTORY_ID = "history_id";
    public static final String COL_HISTORY_USER_ID = "user_id";
    public static final String COL_HISTORY_USER_EMAIL = "user_email";
    public static final String COL_HISTORY_USER_NAME = "user_name";
    public static final String COL_HISTORY_TOPIC = "topic";
    public static final String COL_HISTORY_TIMESTAMP = "history_timestamp";
    public static final String COL_HISTORY_QUESTION_COUNT = "question_count";
    public static final String COL_HISTORY_QUESTION_TYPE = "question_type";
    public static final String COL_HISTORY_QUESTIONS = "questions_text";

    private static final String SQL_CREATE_USERS = "CREATE TABLE " + TABLE_USERS + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_EMAIL + " TEXT UNIQUE NOT NULL, " +
            COL_PASSWORD + " TEXT NOT NULL, " +
            COL_IS_LOGGED_IN + " INTEGER DEFAULT 0, " +
            COL_IS_DELETED + " INTEGER DEFAULT 0, " +
            COL_FAILED_ATTEMPTS + " INTEGER DEFAULT 0, " +
            COL_LOCK_TIME + " INTEGER DEFAULT 0, " +
            COL_LAST_ACTIVE + " INTEGER DEFAULT 0, " +
            COL_LAST_LOGIN + " INTEGER DEFAULT 0, " +
            COL_CREATED_AT + " INTEGER DEFAULT 0, " +
            COL_UPDATED_AT + " INTEGER DEFAULT 0)";

    private static final String SQL_CREATE_QUESTION_HISTORY =
            "CREATE TABLE IF NOT EXISTS " + TABLE_QUESTION_HISTORY + " (" +
                    COL_HISTORY_ID + " TEXT PRIMARY KEY, " +
                    COL_HISTORY_USER_ID + " INTEGER NOT NULL, " +
                    COL_HISTORY_USER_EMAIL + " TEXT NOT NULL, " +
                    COL_HISTORY_USER_NAME + " TEXT, " +
                    COL_HISTORY_TOPIC + " TEXT NOT NULL, " +
                    COL_HISTORY_TIMESTAMP + " INTEGER NOT NULL, " +
                    COL_HISTORY_QUESTION_COUNT + " INTEGER NOT NULL, " +
                    COL_HISTORY_QUESTION_TYPE + " TEXT NOT NULL, " +
                    COL_HISTORY_QUESTIONS + " TEXT NOT NULL, " +
                    "FOREIGN KEY(" + COL_HISTORY_USER_ID + ") REFERENCES " +
                    TABLE_USERS + "(" + COL_ID + "))";

    private static final String SQL_CREATE_QUESTION_HISTORY_INDEX =
            "CREATE INDEX IF NOT EXISTS idx_question_history_user_id ON " +
                    TABLE_QUESTION_HISTORY + "(" + COL_HISTORY_USER_ID + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        createQuestionHistoryTable(db);
        Log.d(TAG, "Database created at version " + DATABASE_VERSION);
    }

    /**
     * Upgrades the database schema while preserving existing user data.
     *
     * Strategy:
     * - From v6 → v7: Add the two new columns with ALTER TABLE.
     * - Older versions: rebuild from scratch (acceptable for dev builds).
     *
     * Note: Passwords hashed with the old unsalted scheme will not verify
     * against the new salted PasswordUtils scheme. Affected users must
     * re-register (dev/test scenario only; production would need a migration flow).
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade from v" + oldVersion + " to v" + newVersion);
        if (oldVersion < 7) {
            safeAddColumn(db, TABLE_USERS, COL_LAST_LOGIN, "INTEGER DEFAULT 0");
            safeAddColumn(db, TABLE_USERS, COL_UPDATED_AT, "INTEGER DEFAULT 0");
        }

        if (oldVersion < 8) {
            createQuestionHistoryTable(db);
        }
    }

    /** Adds a column to a table without throwing if it already exists. */
    private void safeAddColumn(SQLiteDatabase db, String table, String column, String definition) {
        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (Exception e) {
            Log.w(TAG, "Column " + column + " may already exist: " + e.getMessage());
        }
    }

    private void createQuestionHistoryTable(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_QUESTION_HISTORY);
        db.execSQL(SQL_CREATE_QUESTION_HISTORY_INDEX);
    }
}
