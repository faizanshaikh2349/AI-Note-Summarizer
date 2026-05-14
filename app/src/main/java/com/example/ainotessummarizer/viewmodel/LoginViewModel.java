package com.example.ainotessummarizer.viewmodel;

import android.app.Application;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.ainotessummarizer.repository.AuthRepository;
import com.example.ainotessummarizer.session.SessionManager;

/**
 * ViewModel for LoginActivity.
 *
 * Architecture Note (MVVM):
 * This ViewModel survives configuration changes (rotation), so the countdown
 * timer is preserved even if the Activity is recreated. The Activity observes
 * LiveData and reacts to state changes — it never performs business logic
 * itself.
 *
 * Responsibilities:
 * - Validate inputs (lightweight checks only)
 * - Interact with AuthRepository for credential verification
 * - Manage lock countdown timer (rotation-safe)
 * - Post login state to LoginActivity via LiveData
 */
public class LoginViewModel extends AndroidViewModel {

    public enum LoginState {
        IDLE,
        LOADING,
        SUCCESS,
        WRONG_CREDENTIALS,
        ACCOUNT_LOCKED,
        FIELDS_EMPTY,
        ERROR
    }

    // Max login attempts before account is locked
    public static final int MAX_ATTEMPTS = 5;
    // Lock duration: 60 seconds
    public static final long LOCK_DURATION_MS = 60_000L;

    private final AuthRepository authRepository;
    private final SessionManager sessionManager;

    // Observed by LoginActivity
    public final MutableLiveData<LoginState> loginState = new MutableLiveData<>(LoginState.IDLE);
    // Remaining lock seconds — drives the button text countdown
    public final MutableLiveData<Long> lockSecondsRemaining = new MutableLiveData<>(0L);

    private CountDownTimer countDownTimer;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
        sessionManager = new SessionManager(application);
    }

    /**
     * Attempts to log in the user.
     *
     * Flow:
     * 1. Basic empty-field check
     * 2. Check if account is currently locked (persisted in DB)
     * 3. Verify credentials via AuthRepository
     * 4. On success: save session, update last login, reset attempts
     * 5. On failure: increment attempts, lock if threshold reached
     *
     * No password pattern validation here — login only checks credentials.
     *
     * @param email    The entered email address.
     * @param password The entered password (plain text).
     */
    public void attemptLogin(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            loginState.setValue(LoginState.FIELDS_EMPTY);
            return;
        }

        loginState.setValue(LoginState.LOADING);

        // Check if account is still locked from a previous session
        long remaining = authRepository.getRemainingLockTime(email, LOCK_DURATION_MS);
        if (remaining > 0) {
            loginState.setValue(LoginState.ACCOUNT_LOCKED);
            startCountdown(remaining);
            return;
        }

        // Verify credentials against the database
        boolean success = authRepository.verifyCredentials(email, password);

        if (success) {
            // Successful login — clean up and save session
            authRepository.resetFailedAttempts(email);
            authRepository.updateLastLogin(email);
            sessionManager.saveSession(email);
            loginState.setValue(LoginState.SUCCESS);
        } else {
            // Failed login — increment attempts and check if lock threshold reached
            authRepository.incrementFailedAttempts(email);
            int attempts = authRepository.getFailedAttempts(email);

            if (attempts >= MAX_ATTEMPTS) {
                authRepository.setLockTime(email);
                startCountdown(LOCK_DURATION_MS);
                loginState.setValue(LoginState.ACCOUNT_LOCKED);
            } else {
                loginState.setValue(LoginState.WRONG_CREDENTIALS);
            }
        }
    }

    /**
     * Returns how many login attempts the user has left.
     *
     * @param email The user's email.
     * @return Number of remaining attempts before lockout.
     */
    public int getRemainingAttempts(String email) {
        return MAX_ATTEMPTS - authRepository.getFailedAttempts(email);
    }

    /**
     * Starts or resumes a countdown timer for the account lock.
     * Survives rotation because this ViewModel is NOT destroyed on config change.
     *
     * @param millis Total milliseconds to count down.
     */
    public void startCountdown(long millis) {
        if (countDownTimer != null)
            countDownTimer.cancel();

        countDownTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                lockSecondsRemaining.postValue(millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                lockSecondsRemaining.postValue(0L);
                loginState.postValue(LoginState.IDLE);
            }
        }.start();
    }

    /** Checks any pre-existing lock for a given email on Activity start. */
    public void checkExistingLock(String email) {
        if (email == null || email.isEmpty())
            return;
        long remaining = authRepository.getRemainingLockTime(email, LOCK_DURATION_MS);
        if (remaining > 0) {
            loginState.setValue(LoginState.ACCOUNT_LOCKED);
            startCountdown(remaining);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null)
            countDownTimer.cancel();
        authRepository.close();
    }
}
