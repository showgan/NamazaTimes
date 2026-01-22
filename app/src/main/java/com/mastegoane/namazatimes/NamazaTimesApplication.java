package com.mastegoane.namazatimes;

import android.app.Application;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Application class for NamazaTimes app.
 * Initializes Firebase Crashlytics for crash reporting.
 */
public class NamazaTimesApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase Crashlytics
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        
        // Enable crash collection (you can disable this for debug builds if needed)
        crashlytics.setCrashlyticsCollectionEnabled(true);
    }
}
