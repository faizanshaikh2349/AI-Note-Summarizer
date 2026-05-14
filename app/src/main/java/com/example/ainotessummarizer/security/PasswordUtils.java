package com.example.ainotessummarizer.security;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Security utility class for password hashing and strength validation.
 *
 * Architecture Note:
 * Passwords are hashed with a random 16-byte salt using SHA-256.
 * The stored format is "base64(salt):base64(hash)" so the salt can be
 * extracted on verification without being stored separately.
 *
 * Security Improvement:
 * Previous version used unsalted SHA-256, which is vulnerable to
 * rainbow-table attacks. Adding a unique random salt per password
 * prevents pre-computed attacks even if the database is compromised.
 */
public final class PasswordUtils {

    private static final int SALT_LENGTH_BYTES = 16;

    // Private constructor – utility class should not be instantiated
    private PasswordUtils() {
    }

    /**
     * Hashes a password with a newly generated random salt.
     *
     * @param password The plain-text password to hash.
     * @return A string in the format "base64(salt):base64(hash)"
     */
    public static String hashWithSalt(String password) {
        byte[] salt = generateSalt();
        byte[] hash = sha256(salt, password);
        String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP);
        return saltB64 + ":" + hashB64;
    }

    /**
     * Verifies a plain-text password against a previously hashed value.
     *
     * @param password     The plain-text password to check.
     * @param storedHashed The stored "salt:hash" string from the database.
     * @return true if the password matches, false otherwise.
     */
    public static boolean verify(String password, String storedHashed) {
        if (storedHashed == null || !storedHashed.contains(":"))
            return false;
        try {
            String[] parts = storedHashed.split(":", 2);
            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(parts[1], Base64.NO_WRAP);
            byte[] actualHash = sha256(salt, password);
            return MessageDigest.isEqual(actualHash, expectedHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Password strength levels used for the real-time strength indicator.
     */
    public enum PasswordStrength {
        VERY_WEAK, // < 8 chars
        WEAK, // 8 chars, missing most criteria
        FAIR, // meets 2-3 criteria
        STRONG, // meets all criteria
        VERY_STRONG // meets all + length >= 12
    }

    /**
     * Evaluates the strength of a password.
     *
     * Criteria:
     * - At least 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - At least one special character
     *
     * @param password The password to evaluate.
     * @return A {@link PasswordStrength} enum value.
     */
    public static PasswordStrength getStrength(String password) {
        if (password == null || password.length() < 8)
            return PasswordStrength.VERY_WEAK;

        int score = 0;
        if (password.length() >= 12)
            score++;
        if (password.matches(".*[A-Z].*"))
            score++;
        if (password.matches(".*[a-z].*"))
            score++;
        if (password.matches(".*\\d.*"))
            score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))
            score++;

        switch (score) {
            case 0:
            case 1:
                return PasswordStrength.WEAK;
            case 2:
                return PasswordStrength.FAIR;
            case 3:
            case 4:
                return PasswordStrength.STRONG;
            default:
                return PasswordStrength.VERY_STRONG;
        }
    }

    /**
     * Validates a password against the minimum security requirements.
     * These rules are enforced only at registration (not at login).
     *
     * Requirements: 8+ chars, 1 uppercase, 1 lowercase, 1 digit.
     *
     * @param password The password to validate.
     * @return null if valid, or an error message string if invalid.
     */
    public static String validateForSignup(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter";
        }
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one number";
        }
        return null; // valid
    }

    // --- Private helpers ---

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] sha256(byte[] salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
