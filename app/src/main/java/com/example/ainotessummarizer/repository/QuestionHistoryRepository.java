package com.example.ainotessummarizer.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.ainotessummarizer.DatabaseHelper;
import com.example.ainotessummarizer.model.QuestionModel;

import java.util.ArrayList;
import java.util.List;

public class QuestionHistoryRepository {

    private static final String TAG = "QuestionHistoryRepo";

    private final DatabaseHelper dbHelper;

    public QuestionHistoryRepository(Context context) {
        dbHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public boolean saveQuestionSet(String userEmail, QuestionModel questionModel) {
        if (userEmail == null || userEmail.trim().isEmpty() || questionModel == null) {
            return false;
        }

        UserInfo userInfo = getUserInfo(userEmail);
        if (userInfo == null) {
            Log.w(TAG, "Could not save history because user was not found: " + userEmail);
            return false;
        }

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_HISTORY_ID, questionModel.getId());
        values.put(DatabaseHelper.COL_HISTORY_USER_ID, userInfo.id);
        values.put(DatabaseHelper.COL_HISTORY_USER_EMAIL, userInfo.email);
        values.put(DatabaseHelper.COL_HISTORY_USER_NAME, userInfo.name);
        values.put(DatabaseHelper.COL_HISTORY_TOPIC, questionModel.getTitle());
        values.put(DatabaseHelper.COL_HISTORY_TIMESTAMP, questionModel.getTimestamp());
        values.put(DatabaseHelper.COL_HISTORY_QUESTION_COUNT, questionModel.getQuestionCount());
        values.put(DatabaseHelper.COL_HISTORY_QUESTION_TYPE, questionModel.getQuestionType());
        values.put(DatabaseHelper.COL_HISTORY_QUESTIONS, questionModel.getQuestions());

        try {
            return dbHelper.getWritableDatabase().insert(
                    DatabaseHelper.TABLE_QUESTION_HISTORY,
                    null,
                    values
            ) != -1;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save question history", e);
            return false;
        }
    }

    public List<QuestionModel> getQuestionHistory(String userEmail) {
        List<QuestionModel> items = new ArrayList<>();
        UserInfo userInfo = getUserInfo(userEmail);

        if (userInfo == null) {
            return items;
        }

        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().query(
                    DatabaseHelper.TABLE_QUESTION_HISTORY,
                    new String[] {
                            DatabaseHelper.COL_HISTORY_ID,
                            DatabaseHelper.COL_HISTORY_TOPIC,
                            DatabaseHelper.COL_HISTORY_TIMESTAMP,
                            DatabaseHelper.COL_HISTORY_QUESTION_COUNT,
                            DatabaseHelper.COL_HISTORY_QUESTION_TYPE,
                            DatabaseHelper.COL_HISTORY_QUESTIONS
                    },
                    DatabaseHelper.COL_HISTORY_USER_ID + "=?",
                    new String[] { String.valueOf(userInfo.id) },
                    null,
                    null,
                    DatabaseHelper.COL_HISTORY_TIMESTAMP + " DESC"
            );

            while (cursor.moveToNext()) {
                items.add(new QuestionModel(
                        cursor.getString(0),
                        cursor.getString(1),
                        cursor.getLong(2),
                        cursor.getInt(3),
                        cursor.getString(4),
                        cursor.getString(5)
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load question history", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return items;
    }

    public void clearQuestionHistory(String userEmail) {
        UserInfo userInfo = getUserInfo(userEmail);
        if (userInfo == null) {
            return;
        }

        dbHelper.getWritableDatabase().delete(
                DatabaseHelper.TABLE_QUESTION_HISTORY,
                DatabaseHelper.COL_HISTORY_USER_ID + "=?",
                new String[] { String.valueOf(userInfo.id) }
        );
    }

    public void close() {
        dbHelper.close();
    }

    private UserInfo getUserInfo(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return null;
        }

        Cursor cursor = null;
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    DatabaseHelper.TABLE_USERS,
                    new String[] {
                            DatabaseHelper.COL_ID,
                            DatabaseHelper.COL_EMAIL
                    },
                    DatabaseHelper.COL_EMAIL + "=? AND " + DatabaseHelper.COL_IS_DELETED + "=0",
                    new String[] { userEmail },
                    null,
                    null,
                    null,
                    "1"
            );

            if (!cursor.moveToFirst()) {
                return null;
            }

            String email = cursor.getString(1);
            return new UserInfo(cursor.getInt(0), email, extractUserName(email));
        } catch (Exception e) {
            Log.e(TAG, "Failed to resolve user info", e);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String extractUserName(String email) {
        if (email == null || !email.contains("@")) {
            return "User";
        }

        return email.substring(0, email.indexOf('@'));
    }

    private static class UserInfo {
        final int id;
        final String email;
        final String name;

        UserInfo(int id, String email, String name) {
            this.id = id;
            this.email = email;
            this.name = name;
        }
    }
}
