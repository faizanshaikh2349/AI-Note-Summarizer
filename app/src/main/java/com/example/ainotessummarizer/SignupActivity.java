package com.example.ainotessummarizer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ainotessummarizer.security.PasswordUtils;
import com.example.ainotessummarizer.viewmodel.SignupViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * SignupActivity handles the new user registration flow.
 *
 * Architecture Note (MVVM):
 * All validation and registration logic lives in SignupViewModel.
 * This Activity only observes state changes and updates the UI.
 *
 * Key Improvements:
 * - Strong password validation (8+ chars, upper, lower, digit)
 * - Real-time password strength bar (5 levels)
 * - Errors via TextInputLayout.setError() (inline, accessible)
 * - Snackbar for non-field errors (Terms, generic errors)
 * - SpannableString clickable "Already have an account? Login"
 */
public class SignupActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout, passwordInputLayout, confirmPasswordInputLayout;
    private TextInputEditText emailEditText, passwordEditText, confirmPasswordEditText;
    private LinearProgressIndicator strengthBar;
    private TextView strengthLabel;
    private MaterialCheckBox termsCheckbox;
    private MaterialButton registerButton;
    private CircularProgressIndicator progressBar;
    private SignupViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        bindViews();
        setupLoginRedirect();
        setupViewModel();
        setupPasswordStrengthWatcher();

        registerButton.setOnClickListener(v -> {
            clearAllErrors();
            viewModel.register(
                    getText(emailEditText),
                    getText(passwordEditText),
                    getText(confirmPasswordEditText),
                    termsCheckbox.isChecked());
        });
    }

    private void bindViews() {
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        strengthBar = findViewById(R.id.passwordStrengthBar);
        strengthLabel = findViewById(R.id.passwordStrengthLabel);
        termsCheckbox = findViewById(R.id.termsCheckbox);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.signupProgress);
    }

    /**
     * Real-time password strength watcher.
     * Updates the LinearProgressIndicator and label as the user types.
     */
    private void setupPasswordStrengthWatcher() {
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onPasswordChanged(s.toString());
            }
        });
    }

    private void setupLoginRedirect() {
        TextView redirectText = findViewById(R.id.loginRedirectText);
        String full = getString(R.string.signup_login_redirect);
        String link = getString(R.string.signup_login_link);
        SpannableString span = new SpannableString(full);
        int start = full.indexOf(link);
        int end = start + link.length();

        span.setSpan(new ForegroundColorSpan(
                ContextCompat.getColor(this, R.color.primary)),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        span.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            }
        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        redirectText.setText(span);
        redirectText.setMovementMethod(LinkMovementMethod.getInstance());
        redirectText.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(SignupViewModel.class);

        // Observe registration state
        viewModel.signupState.observe(this, state -> {
            setLoadingState(false);
            switch (state) {
                case SUCCESS:
                    showSnackbar(getString(R.string.signup_success));
                    startActivity(new Intent(this, LoginActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                    break;
                case EMAIL_EMPTY:
                    emailInputLayout.setError(getString(R.string.error_email_empty));
                    break;
                case EMAIL_INVALID:
                    emailInputLayout.setError(getString(R.string.error_email_invalid));
                    break;
                case EMAIL_EXISTS:
                    emailInputLayout.setError(getString(R.string.error_email_exists));
                    break;
                case PASSWORD_EMPTY:
                    passwordInputLayout.setError(getString(R.string.error_password_empty));
                    break;
                case PASSWORD_WEAK:
                    String msg = viewModel.errorMessage.getValue();
                    passwordInputLayout.setError(msg != null ? msg : getString(R.string.error_password_weak));
                    break;
                case PASSWORDS_MISMATCH:
                    confirmPasswordInputLayout.setError(getString(R.string.error_passwords_mismatch));
                    break;
                case TERMS_NOT_AGREED:
                    showSnackbar(getString(R.string.error_terms_not_agreed));
                    break;
                case LOADING:
                    setLoadingState(true);
                    break;
                case ERROR:
                    showSnackbar(getString(R.string.error_registration_failed));
                    break;
                default:
                    break;
            }
        });

        // Observe real-time password strength
        viewModel.passwordStrength.observe(this, this::updateStrengthBar);
    }

    /** Updates the strength bar and label based on detected password strength. */
    private void updateStrengthBar(PasswordUtils.PasswordStrength strength) {
        int progress, color;
        String label;

        switch (strength) {
            case VERY_WEAK:
                progress = 20;
                color = R.color.strength_very_weak;
                label = getString(R.string.strength_very_weak);
                break;
            case WEAK:
                progress = 40;
                color = R.color.strength_weak;
                label = getString(R.string.strength_weak);
                break;
            case FAIR:
                progress = 60;
                color = R.color.strength_fair;
                label = getString(R.string.strength_fair);
                break;
            case STRONG:
                progress = 80;
                color = R.color.strength_strong;
                label = getString(R.string.strength_strong);
                break;
            case VERY_STRONG:
            default:
                progress = 100;
                color = R.color.strength_very_strong;
                label = getString(R.string.strength_very_strong);
                break;
        }

        strengthBar.setProgress(progress, true);
        strengthBar.setIndicatorColor(ContextCompat.getColor(this, color));
        strengthLabel.setText(label);
        strengthLabel.setTextColor(ContextCompat.getColor(this, color));
    }

    private void setLoadingState(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!loading);
        emailEditText.setEnabled(!loading);
        passwordEditText.setEnabled(!loading);
        confirmPasswordEditText.setEnabled(!loading);
    }

    private void clearAllErrors() {
        emailInputLayout.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }
}
