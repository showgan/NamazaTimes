package com.mastegoane.namazatimes;

import android.content.SharedPreferences;
import android.content.Context;
import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.Preference;
import androidx.preference.ListPreference;
import android.content.Intent;
import androidx.appcompat.app.AppCompatDelegate;
import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import androidx.core.app.NotificationManagerCompat;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        // initialize switch state
        SwitchPreferenceCompat themeSwitch = findPreference("pref_key_dark_theme");
        if (themeSwitch != null) {
                int mode = getContext().getSharedPreferences("namazatimes", Context.MODE_PRIVATE)
                    .getInt("pref_night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            themeSwitch.setChecked(mode == AppCompatDelegate.MODE_NIGHT_YES);
        }
        // initialize widget theme preference
        ListPreference widgetPref = findPreference("pref_key_widget_theme");
        if (widgetPref != null) {
            int val = getContext().getSharedPreferences("namazatimes", Context.MODE_PRIVATE)
                    .getInt("pref_widget_theme", 0);
            widgetPref.setValue(String.valueOf(val));
        }

        // Notification settings preference
        Preference notifPref = findPreference("pref_key_notifications");
        if (notifPref != null) {
            notifPref.setOnPreferenceClickListener(preference -> {
                Context ctx = getContext();
                if (ctx == null) return true;

                // On Android 13+, request POST_NOTIFICATIONS if not granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
                        return true;
                    }
                }

                // Otherwise open the app's notification settings
                try {
                    Intent intent = new Intent();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, ctx.getPackageName());
                    } else {
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + ctx.getPackageName()));
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ctx, "Could not open notification settings", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateNotificationPreferenceSummary();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            Context ctx = getContext();
            if (ctx == null) return;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(ctx, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied: explain and offer system settings
                new androidx.appcompat.app.AlertDialog.Builder(ctx)
                        .setTitle("Notifications blocked")
                        .setMessage("To enable notifications, allow notifications for this app in system settings.")
                        .setPositiveButton("Open Settings", (d, w) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse("package:" + ctx.getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } catch (Exception e) { e.printStackTrace(); }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
            updateNotificationPreferenceSummary();
        }
    }

    private void updateNotificationPreferenceSummary() {
        Preference notifPref = findPreference("pref_key_notifications");
        Context ctx = getContext();
        if (notifPref == null || ctx == null) return;
        boolean enabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled();
        if (enabled) {
            notifPref.setSummary("Notifications are allowed. Tap to open notification settings.");
        } else {
            // On Android 13+, check if POST_NOTIFICATIONS permission is granted; if not, prompt
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notifPref.setSummary("Notifications permission not granted — tap to allow notifications.");
                    return;
                }
            }
            notifPref.setSummary("Notifications are blocked — tap to open system notification settings.");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("pref_key_dark_theme".equals(key)) {
            boolean enabled = sharedPreferences.getBoolean(key, false);
            int mode = enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            SharedPreferences.Editor editor = getContext().getSharedPreferences("namazatimes", Context.MODE_PRIVATE).edit();
            editor.putInt("pref_night_mode", mode).commit();
            AppCompatDelegate.setDefaultNightMode(mode);
            // notify widget to update when app theme changes
            try {
                Intent intent = new Intent(getContext(), NextPrayerWidgetProvider.class);
                intent.setAction(NextPrayerWidgetProvider.ACTION_UPDATE_WIDGET);
                getContext().sendBroadcast(intent);
            } catch (Exception ignored) {}
        } else if ("pref_key_language".equals(key)) {
            String val = sharedPreferences.getString(key, "1");
            int selected = 1;
            try {
                selected = Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
            SharedPreferences.Editor editor = getContext().getSharedPreferences("namazatimes", Context.MODE_PRIVATE).edit();
            editor.putInt("sp_current_fragment", selected).commit();
            // No direct access to MainActivity here; the MainActivity will check prefs on resume.
        }
        if ("pref_key_widget_theme".equals(key)) {
            String str = sharedPreferences.getString(key, "0");
            int v = 0;
            try { v = Integer.parseInt(str); } catch (Exception ignored) {}
            getContext().getSharedPreferences("namazatimes", Context.MODE_PRIVATE).edit().putInt("pref_widget_theme", v).commit();
            // notify widget to update
            Intent intent = new Intent(getContext(), NextPrayerWidgetProvider.class);
            intent.setAction(NextPrayerWidgetProvider.ACTION_UPDATE_WIDGET);
            getContext().sendBroadcast(intent);
        }
    }
}
