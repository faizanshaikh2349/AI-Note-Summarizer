package com.example.ainotessummarizer;

import android.app.Activity;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * BottomNavHelper — Material 3 bottom navigation controller.
 *
 * Features:
 *  - Pill-shaped active indicator (set via style)
 *  - Smooth overshoot scale animation on each tab tap
 *  - Color transition handled by bottom_nav_colors selector
 *  - Clean tab-ID callback for HomeActivity
 *  - Programmatic tab selection for back-press restore
 */
public class BottomNavHelper {

    private final Activity activity;
    private final NavSelectionListener listener;
    private BottomNavigationView bottomNav;

    public interface NavSelectionListener {
        void onTabSelected(int tabId);
    }

    public BottomNavHelper(Activity activity, NavSelectionListener listener) {
        this.activity  = activity;
        this.listener  = listener;
        initViews();
    }

    private void initViews() {
        bottomNav = activity.findViewById(R.id.bottomNavigation);
        if (bottomNav == null) return;

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if      (id == R.id.nav_home)      listener.onTabSelected(1);
            else if (id == R.id.nav_flashcard) listener.onTabSelected(2);
            else if (id == R.id.nav_ai_chat)   listener.onTabSelected(3);
            else if (id == R.id.nav_history)   listener.onTabSelected(4);
            else if (id == R.id.nav_profile)   listener.onTabSelected(5);

            animateSelectedItem(item);
            return true;
        });

        // Default: Home tab selected
        bottomNav.setSelectedItemId(R.id.nav_home);
    }

    /**
     * Bouncy overshoot scale animation on the tapped item icon.
     * Scale goes 1.0 → 1.25 (with overshoot) → 1.0 smoothly.
     */
    private void animateSelectedItem(MenuItem item) {
        View itemView = bottomNav.findViewById(item.getItemId());
        if (itemView == null) return;

        // Scale up with overshoot
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(itemView, View.SCALE_X, 1f, 1.22f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(itemView, View.SCALE_Y, 1f, 1.22f);
        scaleUpX.setDuration(180);
        scaleUpY.setDuration(180);
        scaleUpX.setInterpolator(new OvershootInterpolator(2.5f));
        scaleUpY.setInterpolator(new OvershootInterpolator(2.5f));

        // Scale back to normal
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(itemView, View.SCALE_X, 1.22f, 1f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(itemView, View.SCALE_Y, 1.22f, 1f);
        scaleDownX.setDuration(160);
        scaleDownY.setDuration(160);

        AnimatorSet set = new AnimatorSet();
        set.play(scaleUpX).with(scaleUpY);
        set.play(scaleDownX).with(scaleDownY).after(scaleUpX);
        set.start();
    }

    /**
     * Programmatically selects a tab (used by HomeActivity on back-press to restore Home).
     *
     * tabId mapping:
     *   1 = Home | 2 = Flashcard | 3 = AI Chat | 4 = History | 5 = Profile
     */
    public void selectTab(int tabId) {
        if (bottomNav == null) return;
        switch (tabId) {
            case 1: bottomNav.setSelectedItemId(R.id.nav_home);      break;
            case 2: bottomNav.setSelectedItemId(R.id.nav_flashcard); break;
            case 3: bottomNav.setSelectedItemId(R.id.nav_ai_chat);   break;
            case 4: bottomNav.setSelectedItemId(R.id.nav_history);   break;
            case 5: bottomNav.setSelectedItemId(R.id.nav_profile);   break;
        }
    }

    /** Returns the underlying BottomNavigationView for direct access if needed. */
    public BottomNavigationView getBottomNavigationView() {
        return bottomNav;
    }
}