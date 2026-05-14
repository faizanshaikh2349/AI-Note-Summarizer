package com.example.ainotessummarizer.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.ainotessummarizer.repository.AuthRepository;
import com.example.ainotessummarizer.security.PasswordUtils;

/**
 * ViewModel for SignupActivity.
 *
 * Architecture Note (MVVM):
 * All registration logic lives here, not in the Activity.
 * The Activity observes SignupState LiveData and updates the UI accordingly.
 *
 * Responsibilities:
 * - Validate email format
 * - Validate strong password rules (registration only)
 * - Interact with AuthRepository to register users
 * - Evaluate real-time password strength for the strength indicator
 */
public class SignupViewModel extends AndroidViewModel {

    public enum SignupState {
        IDLE,
        LOADING,
        SUCCESS,
        EMAIL_EMPTY,
        EMAIL_INVALID,
        EMAIL_EXISTS,
        PASSWORD_EMPTY,
        PASSWORD_WEAK, // Specific error message from PasswordUtils
        PASSWORDS_MISMATCH,
        TERMS_NOT_AGREED,
        ERROR
    }

    private final AuthRepository authRepository;

    // Observed by SignupActivity
    public final MutableLiveData<SignupState> signupState = new MutableLiveData<>(SignupState.IDLE);
    // Specific error detail (e.g., which password rule failed)
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    // Real-time password strength for the strength bar
    public final MutableLiveData<PasswordUtils.PasswordStrength> passwordStrength = new MutableLiveData<>(
            PasswordUtils.PasswordStrength.VERY_WEAK);

    public SignupViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
    }

    /**
     * Called every time the password field changes (real-time strength indicator).
     *
     * @param password Current text in the password field.
     */
    public void onPasswordChanged(String password) {
        passwordStrength.setValue(PasswordUtils.getStrength(password));
    }

    /**
     * Attempts to register a new user.
     *
     * Validation order (showing first failure):
     * 1. Email empty
     * 2. Email format invalid
     * 3. Password empty
     * 4. Password not strong enough (8+, upper, lower, digit)
     * 5. Passwords don't match
     * 6. Terms not accepted
     * 7. Email already registered
     *
     * @param email           The entered email address.
     * @param password        The entered password.
     * @param confirmPassword The confirm password entry.
     * @param termsAccepted   Whether the user checked the terms checkbox.
     */
    public void register(String email, String password, String confirmPassword, boolean termsAccepted) {
        signupState.setValue(SignupState.LOADING);

        // Email validation
        if (email.isEmpty()) {
            signupState.setValue(SignupState.EMAIL_EMPTY);
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signupState.setValue(SignupState.EMAIL_INVALID);
            return;
        }

        // Password validation
        if (password.isEmpty()) {
            signupState.setValue(SignupState.PASSWORD_EMPTY);
            return;
        }
        String pwError = PasswordUtils.validateForSignup(password);
        if (pwError != null) {
            errorMessage.setValue(pwError);
            signupState.setValue(SignupState.PASSWORD_WEAK);
            return;
        }

        // Confirmation match
        if (!password.equals(confirmPassword)) {
            signupState.setValue(SignupState.PASSWORDS_MISMATCH);
            return;
        }

        // Terms agreement
        if (!termsAccepted) {
            signupState.setValue(SignupState.TERMS_NOT_AGREED);
            return;
        }

        // Uniqueness check
        if (authRepository.isEmailExists(email)) {
            signupState.setValue(SignupState.EMAIL_EXISTS);
            return;
        }

        // Register — password is hashed inside AuthRepository
        boolean success = authRepository.register(email, password);
        signupState.setValue(success ? SignupState.SUCCESS : SignupState.ERROR);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        authRepository.close();
    }
}
