package com.example.ainotessummarizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ainotessummarizer.viewmodel.LoginViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * LoginActivity handles user authentication UI.
 *
 * Architecture Note (MVVM):
 * This Activity is a pure UI controller. All logic lives in LoginViewModel.
 * The Activity observes LiveData and reacts to state changes.
 *
 * Key Improvements:
 * - No direct database access (removed)
 * - No password pattern validation (login only verifies credentials)
 * - Uses Snackbar instead of Toast for better UX
 * - Countdown timer is rotation-safe (lives in ViewModel)
 * - Uses ContextCompat for all color access (no deprecated
 * getResources().getColor())
 * - Material3 TextInputLayout for better accessibility and error display
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout, passwordInputLayout;
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton loginButton;
    private CircularProgressIndicator progressBar;
    private TextView signupRedirectText;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        bindViews();
        setupSignupRedirect();
        setupViewModel();
        setupClickListeners();
    }

    private void bindViews() {
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.loginProgress);
        signupRedirectText = findViewById(R.id.signupRedirectText);
    }

    /**
     * Creates a SpannableString for "New User? Sign Up" where "Sign Up"
     * is styled as a clickable colored link. This avoids having two separate
     * TextViews and provides proper accessibility.
     */
    private void setupSignupRedirect() {
        String full = getString(R.string.login_signup_redirect);
        String link = getString(R.string.login_signup_link);
        SpannableString span = new SpannableString(full);
        int start = full.indexOf(link);
        int end = start + link.length();

        span.setSpan(new ForegroundColorSpan(
                ContextCompat.getColor(this, R.color.primary)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        span.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@androidx.annotation.NonNull View widget) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        signupRedirectText.setText(span);
        signupRedirectText.setMovementMethod(LinkMovementMethod.getInstance());
        signupRedirectText.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Observe login state changes
        viewModel.loginState.observe(this, state -> {
            setLoadingState(false);
            switch (state) {
                case SUCCESS:
                    startActivity(new Intent(this, HomeActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                    finish();
                    break;

                case WRONG_CREDENTIALS:
                    int left = viewModel.getRemainingAttempts(getEmail());
                    showSnackbar(getString(R.string.error_wrong_credentials_attempts, left));
                    passwordInputLayout.setError(getString(R.string.error_wrong_password));
                    break;

                case ACCOUNT_LOCKED:
                    // Button text updated by lockSecondsRemaining observer
                    loginButton.setEnabled(false);
                    break;

                case FIELDS_EMPTY:
                    showSnackbar(getString(R.string.error_fields_empty));
                    break;

                case LOADING:
                    setLoadingState(true);
                    emailInputLayout.setError(null);
                    passwordInputLayout.setError(null);
                    break;

                case IDLE:
                    loginButton.setEnabled(true);
                    loginButton.setText(R.string.login_button);
                    loginButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
                    break;

                case ERROR:
                    showSnackbar(getString(R.string.error_generic));
                    break;
            }
        });

        // Countdown drives button text during lock
        viewModel.lockSecondsRemaining.observe(this, seconds -> {
            if (seconds > 0) {
                loginButton.setEnabled(false);
                loginButton.setText(getString(R.string.login_locked_countdown, seconds));
                loginButton.setBackgroundTintList(
                        ContextCompat.getColorStateList(this, R.color.login_locked));
            }
        });
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> {
            passwordInputLayout.setError(null);
            viewModel.attemptLogin(getEmail(), getPassword());
        });

        // "Forgot Password?" placeholder dialog
        TextView forgotPassword = findViewById(R.id.forgotPasswordText);
        forgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    /** Shows a loading spinner and disables the login button during auth. */
    private void setLoadingState(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        emailEditText.setEnabled(!loading);
        passwordEditText.setEnabled(!loading);
    }

    /**
     * Placeholder "Forgot Password" flow.
     * In a full production app this would launch a reset email flow.
     */
    private void showForgotPasswordDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.forgot_password_title)
                .setMessage(R.string.forgot_password_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String getEmail() {
        return emailEditText.getText() != null ? emailEditText.getText().toString().trim() : "";
    }

    private String getPassword() {
        return passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}