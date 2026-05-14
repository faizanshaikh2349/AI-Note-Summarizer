package com.example.ainotessummarizer.Fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ainotessummarizer.LoginActivity;
import com.example.ainotessummarizer.R;
import com.example.ainotessummarizer.session.SessionManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ProfileFragment — shows user info, usage stats, preferences, and account
 * actions.
 */
public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SessionManager session = new SessionManager(requireContext());
        String email = session.getUserEmail();
        if (email == null)
            email = "Unknown";

        // ── Header ──
        TextView tvDisplayName = view.findViewById(R.id.tvProfileDisplayName);
        if (tvDisplayName != null && email.contains("@")) {
            String name = email.substring(0, email.indexOf('@'));
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            tvDisplayName.setText(name);
        }

        TextView tvEmail = view.findViewById(R.id.profileEmailText);
        if (tvEmail != null)
            tvEmail.setText(email);

        TextView tvAccountEmail = view.findViewById(R.id.tvAccountEmail);
        if (tvAccountEmail != null)
            tvAccountEmail.setText(email);

        // ── Session login time ──
        TextView tvLoginTime = view.findViewById(R.id.tvLoginTime);
        if (tvLoginTime != null) {
            long loginMs = session.getLoginTime();
            if (loginMs > 0) {
                String formatted = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(new Date(loginMs));
                tvLoginTime.setText(formatted);
            }
        }

        // ── Member since ──
        TextView tvMemberSince = view.findViewById(R.id.tvMemberSince);
        if (tvMemberSince != null) {
            long loginMs = session.getLoginTime();
            if (loginMs > 0) {
                String since = new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                        .format(new Date(loginMs));
                tvMemberSince.setText("Since " + since);
            }
        }

        // ── Firebase stats ──
        if (email.contains("@")) {
            String safeEmail = email.replace(".", ",");
            FirebaseDatabase db = FirebaseDatabase.getInstance();

            db.getReference("chats").child(safeEmail)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            long count = snap.getChildrenCount();
                            updateStat(view, R.id.tvProfileChats, count);
                            updateStat(view, R.id.tvStatChats, count);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {
                        }
                    });
        }

        // ── Change Password row (show info dialog) ──
        View rowChangePwd = view.findViewById(R.id.rowChangePassword);
        if (rowChangePwd != null) {
            rowChangePwd.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                    .setTitle("Change Password")
                    .setMessage(
                            "To change your password, log out and use the 'Forgot Password' option on the login screen.")
                    .setPositiveButton("OK", null)
                    .show());
        }

        // ── Notifications toggle (persisted in SharedPreferences) ──
        SwitchMaterial switchNotif = view.findViewById(R.id.switchNotifications);
        if (switchNotif != null) {
            android.content.SharedPreferences prefs = requireContext()
                    .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE);
            switchNotif.setChecked(prefs.getBoolean("notifications_enabled", true));
            switchNotif.setOnCheckedChangeListener(
                    (btn, checked) -> prefs.edit().putBoolean("notifications_enabled", checked).apply());
        }

        // ── Clear chat history ──
        View rowClear = view.findViewById(R.id.rowClearHistory);
        if (rowClear != null && email.contains("@")) {
            String safeEmail = email.replace(".", ",");
            rowClear.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                    .setTitle("Clear Chat History")
                    .setMessage("This will permanently delete all your AI chat history. This cannot be undone.")
                    .setPositiveButton("Clear", (d, w) -> {
                        FirebaseDatabase.getInstance().getReference("chats")
                                .child(safeEmail).removeValue();
                        Snackbar.make(view, "Chat history cleared", Snackbar.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show());
        }

        // ── Logout ──
        View rowLogout = view.findViewById(R.id.rowLogout);
        if (rowLogout != null) {
            rowLogout.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Log Out", (d, w) -> {
                        session.clearSession();
                        startActivity(new Intent(requireActivity(), LoginActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        requireActivity().finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show());
        }
    }

    private void updateStat(View root, int viewId, long value) {
        if (!isAdded())
            return;
        TextView tv = root.findViewById(viewId);
        if (tv != null)
            tv.setText(String.valueOf(value));
    }
}