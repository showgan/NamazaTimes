package com.mastegoane.namazatimes;

import android.app.Application;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

public class MainViewModel extends AndroidViewModel {
    public MainViewModel(@NonNull Application application) {
        super(application);
        mApplication = application;
        mResources = mApplication.getApplicationContext().getResources();
        // TODO related to adding notifications
//        mAlarmMgr = (AlarmManager)mApplication.getSystemService(Context.ALARM_SERVICE);
        mPrayerTimes = new PrayerTimes();
        mUpdateViews = new MutableLiveData<>(false);
    }

    public void readPrayerTimes() {
        InputStream prayerTimesAqsa = mResources.openRawResource(R.raw.prayer_times_aqsa);
        mPrayerTimes.readPrayerTimes(prayerTimesAqsa);
    }

    public PrayerTimes getPrayerTimes() {
        return mPrayerTimes;
    }

    public PrayerTimes.DailyTimes getTodaysTimes() {
        return mPrayerTimes.getTodaysTimes();
    }

    public PrayerTimes.DailyTimes getTimesOf(Date date) {
        return mPrayerTimes.getTimesOf(date);
    }

    public LiveData<Calendar> getCalendar() {
        return mPrayerTimes.getCalendar();
    }

    public int getCurrentPrayerIndex() {
        return mPrayerTimes.getCurrentPrayerIndex();
    }

    public void updateViews() {
        mUpdateViews.setValue(!mUpdateViews.getValue());
    }

    public LiveData<Boolean> getUpdateViews() {
        return mUpdateViews;
    }

    // TODO related to adding notifications
//    public void setMainDailyAlarm() {
//        Intent intent = new Intent(mApplication, AlarmReceiver.class);
//        mAlarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
//
//        // Set the alarm to start at approximately 2:00 a.m.
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(System.currentTimeMillis());
//        calendar.set(Calendar.HOUR_OF_DAY, 2);
//        // With setInexactRepeating(), you have to use one of the AlarmManager interval
//        // constants - in this case, AlarmManager.INTERVAL_DAY.
//        mAlarmMgr.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(),
//        AlarmManager.INTERVAL_DAY, alarmIntent);
//    }

    // TODO related to adding notifications
//    public void setDailyPrayerAlarms() {
//    }

    // TODO related to adding notifications
//    private AlarmManager mAlarmMgr;
//    private PendingIntent mAlarmIntent;

    private MutableLiveData<Boolean> mUpdateViews;

    private final Application mApplication;
    private final Resources mResources;
    private final PrayerTimes mPrayerTimes;

    private static final String TAG = MainViewModel.class.getSimpleName();
}
