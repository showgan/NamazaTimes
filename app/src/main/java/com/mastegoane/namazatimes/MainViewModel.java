package com.mastegoane.namazatimes;

import android.app.Application;
import android.content.res.Resources;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;

public class MainViewModel extends AndroidViewModel {
    public MainViewModel(@NonNull Application application) {
        super(application);
        mApplication = application;
        mResources = mApplication.getApplicationContext().getResources();
        mPrayerTimes = new PrayerTimes();
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

    public Calendar getCalendar() {
        return mPrayerTimes.getCalendar();
    }

    public int getCurrentPrayerIndex() {
        return mPrayerTimes.getCurrentPrayerIndex();
    }

    private final Application mApplication;
    private final Resources mResources;
    private final PrayerTimes mPrayerTimes;

    private static final String TAG = MainViewModel.class.getSimpleName();
}
