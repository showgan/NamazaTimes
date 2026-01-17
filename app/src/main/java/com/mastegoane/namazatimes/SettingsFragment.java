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
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
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
