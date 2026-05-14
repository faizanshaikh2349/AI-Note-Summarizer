package com.example.ainotessummarizer;

/**
 * Model class representing a registered user.
 *
 * Updated to include lastLogin timestamp alongside existing fields.
 * Constructor signature updated; existing callers should update accordingly.
 */
public class User {

    public final int id;
    public final String email;
    public final String createdAt;
    public final long lastLogin;

    public User(int id, String email, String createdAt, long lastLogin) {
        this.id = id;
        this.email = email;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
    }
}