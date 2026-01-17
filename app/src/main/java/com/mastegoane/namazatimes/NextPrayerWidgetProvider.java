package com.mastegoane.namazatimes;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.RingtoneManager;
import android.net.Uri;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NextPrayerWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_UPDATE_WIDGET = "com.mastegoane.namazatimes.ACTION_UPDATE_WIDGET";
    private static final int PERIODIC_UPDATE_MILLIS = 15 * 60 * 1000; // 15 minutes
    private static final String CHANNEL_ID = "prayer_times_channel";
    private static final String CHANNEL_NAME = "Prayer Times";
    private static final int NOTIF_ID = 1001;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        // schedule periodic updates and exact update at next prayer
        schedulePeriodicUpdate(context);
        scheduleExactUpdateAtNextPrayer(context);
    }

    @Override
    public void onEnabled(Context context) {
        schedulePeriodicUpdate(context);
        scheduleExactUpdateAtNextPrayer(context);
    }

    @Override
    public void onDisabled(Context context) {
        cancelScheduledUpdates(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_UPDATE_WIDGET.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, NextPrayerWidgetProvider.class));
            onUpdate(context, appWidgetManager, ids);
            // show notification for the prayer time; replaces previous notification (same ID)
            showPrayerNotification(context);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_next_prayer);

        // Determine widget theme mode (0=follow system,1=follow app,2=light,3=dark)
        SharedPreferences sp = context.getSharedPreferences("namazatimes", Context.MODE_PRIVATE);
        int widgetPref = sp.getInt("pref_widget_theme", 0);
        boolean isDark = false;
        if (widgetPref == 0) {
            int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            isDark = (uiMode == Configuration.UI_MODE_NIGHT_YES);
        } else if (widgetPref == 1) {
            int appMode = sp.getInt("pref_night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            if (appMode == AppCompatDelegate.MODE_NIGHT_YES) isDark = true;
            else if (appMode == AppCompatDelegate.MODE_NIGHT_NO) isDark = false;
            else {
                int uiMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                isDark = (uiMode == Configuration.UI_MODE_NIGHT_YES);
            }
        } else if (widgetPref == 2) {
            isDark = false;
        } else if (widgetPref == 3) {
            isDark = true;
        }

        // Apply colors explicitly so launcher renders correctly regardless of its configuration
        int bgColor = ContextCompat.getColor(context, isDark ? R.color.widget_bg_dark : R.color.widget_bg_light);
        int textColor = ContextCompat.getColor(context, isDark ? R.color.widget_text_dark : R.color.widget_text_light);
        try {
            views.setInt(R.id.widget_root, "setBackgroundColor", bgColor);
            views.setTextColor(R.id.widget_prayer_name, textColor);
            views.setTextColor(R.id.widget_prayer_time, textColor);
        } catch (Exception ignored) {}

        try {
            PrayerTimes pt = new PrayerTimes();
            InputStream is = context.getResources().openRawResource(R.raw.prayer_times_aqsa);
            pt.readPrayerTimes(is);
            PrayerTimes.NextPrayer np = pt.getNextPrayer();
            if (np != null) {
                String name = np.getName();
                String time = np.getTime();
                views.setTextViewText(R.id.widget_prayer_name, toAdigaPrayerName(name));
                views.setTextViewText(R.id.widget_prayer_time, time);
            } else {
                views.setTextViewText(R.id.widget_prayer_name, "--");
                views.setTextViewText(R.id.widget_prayer_time, "--:--");
            }
        } catch (Exception e) {
            views.setTextViewText(R.id.widget_prayer_name, "--");
            views.setTextViewText(R.id.widget_prayer_time, "--:--");
        }

        // click opens app
        Intent launch = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_prayer_name, pendingIntent);
        views.setOnClickPendingIntent(R.id.widget_prayer_time, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    private static String toAdigaPrayerName(String name) {
        if (name == null) return "--";
        switch (name.toLowerCase()) {
            case "fajr":
                return "Sebeh";
            case "shurooq":
                return "Tığe Kıćeqığö";
            case "duhr":
                return "Şegağö";
            case "asr":
                return "Yeḱed";
            case "magrib":
                return "Aḣçam";
            case "isha":
                return "Yaś";
            default:
                return capitalize(name);
        }
    }

    private void schedulePeriodicUpdate(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NextPrayerWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long now = System.currentTimeMillis();
        if (am != null) {
            am.setInexactRepeating(AlarmManager.RTC, now + PERIODIC_UPDATE_MILLIS, PERIODIC_UPDATE_MILLIS, pi);
        }
    }

    private void scheduleExactUpdateAtNextPrayer(Context context) {
        try {
            PrayerTimes pt = new PrayerTimes();
            InputStream is = context.getResources().openRawResource(R.raw.prayer_times_aqsa);
            pt.readPrayerTimes(is);
            PrayerTimes.NextPrayer np = pt.getNextPrayer();
            if (np == null) return;
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            Date t = sdf.parse(np.getTime());
            if (t == null) return;
            Calendar target = Calendar.getInstance();
            Calendar now = Calendar.getInstance();
            target.setTime(t);
            target.set(Calendar.YEAR, now.get(Calendar.YEAR));
            target.set(Calendar.MONTH, now.get(Calendar.MONTH));
            target.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
            if (target.before(now) || target.equals(now)) {
                target.add(Calendar.DAY_OF_MONTH, 1);
            }

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, NextPrayerWidgetProvider.class);
            intent.setAction(ACTION_UPDATE_WIDGET);
            PendingIntent pi = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (am != null) {
                // Check if app is allowed to schedule exact alarms. If not, fall back to periodic inexact updates.
                boolean canExact = true;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    canExact = am.canScheduleExactAlarms();
                }
                if (!canExact) {
                    // fallback: schedule periodic inexact update instead of exact alarm
                    schedulePeriodicUpdate(context);
                    return;
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    am.setExact(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void cancelScheduledUpdates(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NextPrayerWidgetProvider.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        PendingIntent pi0 = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pi1 = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (am != null) {
            if (pi0 != null) am.cancel(pi0);
            if (pi1 != null) am.cancel(pi1);
        }
    }

    private void ensureNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("Notifications for prayer times");
                ch.enableLights(true);
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
            }
        }
    }

    private void showPrayerNotification(Context context) {
        try {
            PrayerTimes pt = new PrayerTimes();
            InputStream is = context.getResources().openRawResource(R.raw.prayer_times_aqsa);
            pt.readPrayerTimes(is);
            PrayerTimes.NextPrayer np = pt.getNextPrayer();
            if (np == null) return;
            String name = toAdigaPrayerName(np.getName());
            String time = np.getTime();

            ensureNotificationChannel(context);

            Intent launch = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_outline_settings_24)
                    .setContentTitle(name)
                    .setContentText("Prayer at " + time)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(soundUri)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
