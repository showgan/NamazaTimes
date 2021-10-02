package com.mastegoane.namazatimes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.mastegoane.namazatimes.databinding.ActivityMainBinding;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public void pickDateClicked(View view) {
        new DatePickerDialog(this, mDatePickerDateSetListener,
                mCalendar.get(Calendar.YEAR),
                mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    public void incrementDateClicked(View view) {
        if (view.getTag().toString().equals("increment")) {
            mCalendar.add(Calendar.DATE, 1);
        } else {
            mCalendar.add(Calendar.DATE, -1);
        }
        updateViews();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMainViewModel = new ViewModelProvider(this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(this.getApplication()))
                .get(MainViewModel.class);
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mSharedPreferences = this.getSharedPreferences("namazatimes", MODE_PRIVATE);

        mCalendar = mMainViewModel.getCalendar();

        final BottomNavigationView navigationView = findViewById(R.id.bottomnavigationView);
        navigationView.setSelectedItemId(R.id.navToMainActivity);
        navigationView.setOnNavigationItemSelectedListener(item -> {
//            Intent intent;
            switch (item.getItemId()) {
                case R.id.navToDailyPrayersActivity:
//                        intent = new Intent(getApplicationContext(), DailyPrayersActivity.class);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
                    return true;
                case R.id.navToYearlyPrayerTimesActivity:
//                        intent = new Intent(getApplicationContext(), YearlyPrayerTimesActivity.class);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
                    return true;
                case R.id.navToAmdazActivity:
//                        intent = new Intent(getApplicationContext(), DailyPrayersActivity.class);
//                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
                    return true;
                case R.id.navToMainActivity:
                    return true;
            }
            return false;
        });

        mDatePickerDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // TODO Auto-generated method stub
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, monthOfYear);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateViews();
            }
        };

        mMainViewModel.readPrayerTimes();
        updateViews();
    }

    private void updateViews() {
        final PrayerTimes.DailyTimes dailyTimes = mMainViewModel.getTodaysTimes();
        if (dailyTimes != null) {
            mBinding.textViewMainSabahTime.setText(dailyTimes.mFajr);
            mBinding.textViewMainShurooqTime.setText(dailyTimes.mShurooq);
            mBinding.textViewMainDuhrTime.setText(dailyTimes.mDuhr);
            mBinding.textViewMainAsrTime.setText(dailyTimes.mAsr);
            mBinding.textViewMainMagribTime.setText(dailyTimes.mMagrib);
            mBinding.textViewMainIshaTime.setText(dailyTimes.mIsha);

            final int prayerIndex = mMainViewModel.getCurrentPrayerIndex();
            switch (prayerIndex) {
                case 1:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(true);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
                case 2:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(true);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
                case 3:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(true);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
                case 4:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(true);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
                case 5:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(true);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
                case 6:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(true);
                    break;
                default:
                    mBinding.linearLayoutMainTimeFajr.setEnabled(false);
                    mBinding.linearLayoutMainTimeShurooq.setEnabled(false);
                    mBinding.linearLayoutMainTimeDuhr.setEnabled(false);
                    mBinding.linearLayoutMainTimeAsr.setEnabled(false);
                    mBinding.linearLayoutMainTimeMagrib.setEnabled(false);
                    mBinding.linearLayoutMainTimeIsha.setEnabled(false);
                    break;
            }
            updateTextColors(prayerIndex);

            /*
            <TextView
                android:textColor="@color/mainText"
            <TextView
                android:textColor="@color/mainText"
            <TextView
                android:textColor="@color/mainText"
            <TextView
                android:shadowColor="#50000000"
                android:shadowDx="5"
                android:shadowDy="5"
                android:shadowRadius="9"
                android:textColor="@color/mainText"

        <!-- Duhr -->
            <TextView
                android:textColor="@color/mainTextSelected"
            <TextView
                android:textColor="@color/mainTextSelected"
            <TextView
                android:textColor="@color/mainTextSelected"
            <TextView
                android:shadowColor="#50000000"
                android:shadowDx="5"
                android:shadowDy="5"
                android:shadowRadius="9"
                android:textColor="@color/mainTextSelected"
             */
        }
        String dateFormat = "d\nMMMM\nyyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
        mBinding.textViewMainDate.setText(simpleDateFormat.format(mCalendar.getTime()));
        final int dayOfWeek = mCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        mBinding.textViewMainDay.setText(mDayNamesAdiga[dayOfWeek] + "\n" + mDayNamesHebrew[dayOfWeek] + "\n" + mDayNamesArabic[dayOfWeek]);
    }

    private void updateTextColors(int prayerIndex) {
        final int colorNotSelected = ContextCompat.getColor(this, R.color.mainText);
        final int colorSelected = ContextCompat.getColor(this, R.color.mainTextSelected);
        mBinding.textViewMainSabahAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainSabahHebrew.setTextColor(colorNotSelected);
        mBinding.textViewMainSabahArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainSabahTime.setTextColor(colorNotSelected);

        mBinding.textViewMainShurooqAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainShurooqHebrew.setTextColor(colorNotSelected);
        mBinding.textViewMainShurooqArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainShurooqTime.setTextColor(colorNotSelected);

        mBinding.textViewMainDuhrAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainDuhrHebrew.setTextColor(colorNotSelected);
        mBinding.textViewMainDuhrArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainDuhrTime.setTextColor(colorNotSelected);

        mBinding.textViewMainAsrAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainAsrHebrew.setTextColor(colorNotSelected);
        mBinding.textViewMainAsrArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainAsrTime.setTextColor(colorNotSelected);

        mBinding.textViewMainMagribAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainMagribHebrew.setTextColor(colorNotSelected);
        mBinding.textViewMainMagribArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainMagribTime.setTextColor(colorNotSelected);

        mBinding.textViewMainIshaAdiga.setTextColor(colorNotSelected);
        mBinding.textViewMainIshaHebrew.setTextColor(colorNotSelected);
        mBinding.textViewMainIshaArabic.setTextColor(colorNotSelected);
        mBinding.textViewMainIshaTime.setTextColor(colorNotSelected);

        switch (prayerIndex) {
            case 1:
                mBinding.textViewMainSabahAdiga.setTextColor(colorSelected);
                mBinding.textViewMainSabahHebrew.setTextColor(colorSelected);
                mBinding.textViewMainSabahArabic.setTextColor(colorSelected);
                mBinding.textViewMainSabahTime.setTextColor(colorSelected);
                break;
            case 2:
                mBinding.textViewMainShurooqAdiga.setTextColor(colorSelected);
                mBinding.textViewMainShurooqHebrew.setTextColor(colorSelected);
                mBinding.textViewMainShurooqArabic.setTextColor(colorSelected);
                mBinding.textViewMainShurooqTime.setTextColor(colorSelected);
                break;
            case 3:
                mBinding.textViewMainDuhrAdiga.setTextColor(colorSelected);
                mBinding.textViewMainDuhrHebrew.setTextColor(colorSelected);
                mBinding.textViewMainDuhrArabic.setTextColor(colorSelected);
                mBinding.textViewMainDuhrTime.setTextColor(colorSelected);
                break;
            case 4:
                mBinding.textViewMainAsrAdiga.setTextColor(colorSelected);
                mBinding.textViewMainAsrHebrew.setTextColor(colorSelected);
                mBinding.textViewMainAsrArabic.setTextColor(colorSelected);
                mBinding.textViewMainAsrTime.setTextColor(colorSelected);
                break;
            case 5:
                mBinding.textViewMainMagribAdiga.setTextColor(colorSelected);
                mBinding.textViewMainMagribHebrew.setTextColor(colorSelected);
                mBinding.textViewMainMagribArabic.setTextColor(colorSelected);
                mBinding.textViewMainMagribTime.setTextColor(colorSelected);
                break;
            case 6:
                mBinding.textViewMainIshaAdiga.setTextColor(colorSelected);
                mBinding.textViewMainIshaHebrew.setTextColor(colorSelected);
                mBinding.textViewMainIshaArabic.setTextColor(colorSelected);
                mBinding.textViewMainIshaTime.setTextColor(colorSelected);
                break;
            default:
                break;
        }
    }

    private Calendar mCalendar;
    private DatePickerDialog.OnDateSetListener mDatePickerDateSetListener;
    private String[] mDayNamesAdiga = new String[] {
            "Thawmaf",
            "Blıpe",
            "Ğubcı",
            "Bereskéjiy",
            "Mefeḱu",
            "Bereskefu",
            "Mefezako"
    };
    private String[] mDayNamesEnglish = new String[] {
            "Sunday",
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday"
    };
    private String[] mDayNamesTurkish = new String[] {
            "Pazar",
            "Pazartesi",
            "Salı",
            "Çarşamba",
            "Perşembe",
            "Cuma",
            "Cumartesi"
    };
    private String[] mDayNamesArabic = new String[] {
            "الأحد",
            "الإثنين",
            "الثلاثاء",
            "الأربعاء",
            "الخميس",
            "الجمعة",
            "السبت"
    };
    private String[] mDayNamesHebrew = new String[] {
            "ראשון",
            "שני",
            "שלישי",
            "רביעי",
            "חמישי",
            "שישי",
            "שבת"
    };

    private ActivityMainBinding mBinding;
    private MainViewModel mMainViewModel;
    private SharedPreferences mSharedPreferences = null;

    private static final String TAG = MainActivity.class.getSimpleName();
}
