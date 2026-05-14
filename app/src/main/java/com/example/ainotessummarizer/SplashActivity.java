package com.example.ainotessummarizer;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.ainotessummarizer.session.SessionManager;

/**
 * SplashActivity – the sole launcher activity of the app.
 *
 * Architecture Note:
 * SplashActivity replaces MainActivity as the entry point.
 * It checks SessionManager (single source of truth for session state)
 * and routes the user to the correct destination.
 *
 * Animation Note:
 * The splash screen features a staggered entrance sequence:
 * 1. Logo scales in from 0 with overshoot (0ms → 600ms)
 * 2. Lottie animation fades in (200ms delay)
 * 3. App name slides up + fades in (400ms delay)
 * 4. Tagline slides up + fades in (600ms delay)
 * 5. "Powered by" text fades in (800ms delay)
 *
 * Navigation Rules:
 * - Valid session → HomeActivity (no back stack)
 * - No/expired session → LoginActivity (no back stack)
 *
 * FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK ensure that pressing
 * "Back" from HomeActivity or LoginActivity exits the app entirely,
 * without navigating back to the splash screen.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 2500L;

    private ImageView heroImage;
    private LottieAnimationView lottieView;
    private TextView appNameText;
    private TextView taglineText;
    private TextView poweredByText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Bind views
        heroImage = findViewById(R.id.splashHeroImage);
        lottieView = findViewById(R.id.splashLottie);
        appNameText = findViewById(R.id.appNameText);
        taglineText = findViewById(R.id.taglineText);
        poweredByText = findViewById(R.id.poweredByText);

        // Start Lottie animation
        lottieView.playAnimation();

        // Play the staggered entrance animations
        playEntranceAnimations();

        // After the splash delay, check session and navigate
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateBySessionState, SPLASH_DURATION_MS);
    }

    /**
     * Plays a polished staggered entrance animation sequence.
     * Each element appears with a delay to create a cascade effect.
     */
    private void playEntranceAnimations() {
        // 1. Hero logo — scale from 0 + fade in with overshoot bounce
        heroImage.setScaleX(0f);
        heroImage.setScaleY(0f);
        heroImage.setAlpha(0f);

        AnimatorSet logoAnim = new AnimatorSet();
        logoAnim.playTogether(
                ObjectAnimator.ofFloat(heroImage, "scaleX", 0f, 1f),
                ObjectAnimator.ofFloat(heroImage, "scaleY", 0f, 1f),
                ObjectAnimator.ofFloat(heroImage, "alpha", 0f, 1f)
        );
        logoAnim.setDuration(600);
        logoAnim.setInterpolator(new OvershootInterpolator(1.2f));
        logoAnim.start();

        // 2. Lottie — fade in with slight delay
        lottieView.setAlpha(0f);
        lottieView.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(200)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 3. App name — slide up from 40dp + fade in
        appNameText.setAlpha(0f);
        appNameText.setTranslationY(dpToPx(40));
        appNameText.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(500)
                .setStartDelay(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 4. Tagline — slide up from 30dp + fade in
        taglineText.setAlpha(0f);
        taglineText.setTranslationY(dpToPx(30));
        taglineText.animate()
                .alpha(1f)
                .translationY(0)
                .setDuration(500)
                .setStartDelay(600)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        // 5. "Powered by" — gentle fade in
        poweredByText.setAlpha(0f);
        poweredByText.animate()
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(800)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Converts dp to pixels for translation animations.
     */
    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    /**
     * Checks session validity and routes the user to the appropriate screen.
     * The back stack is cleared so the user cannot navigate back to the splash.
     * A smooth slide transition is applied for the exit.
     */
    private void navigateBySessionState() {
        SessionManager sessionManager = new SessionManager(this);

        Intent intent;
        if (sessionManager.isSessionValid()) {
            // Existing valid session: go directly to home
            intent = new Intent(this, HomeActivity.class);
        } else {
            // No session or expired: require login
            intent = new Intent(this, LoginActivity.class);
        }

        // Clear back stack — pressing Back from next screen exits the app
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}
