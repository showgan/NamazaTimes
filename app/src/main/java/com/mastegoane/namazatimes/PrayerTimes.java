package com.mastegoane.namazatimes;

import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PrayerTimes {
    public PrayerTimes() {
        mTimeTable = new ArrayMap<>();
        mCalendar = Calendar.getInstance();
        mDateFormat = new SimpleDateFormat("dd,mm");
        mDaylightTime = mCalendar.getTimeZone().inDaylightTime(mCalendar.getTime());
    }

    public Calendar getCalendar() {
        return mCalendar;
    }

    public ArrayMap<String, DailyTimes> getTimeTable() {
        return mTimeTable;
    }

    private String toDaylightTime(String time) {
        final String[] hourAndMinutes = time.split(":");
        final String hourStr = hourAndMinutes[0];
        final String minuteStr = hourAndMinutes[1];
        //int hour = Integer.parseInt(time.substring(0, 2)) + 1;
        int hour = Integer.parseInt(hourStr) + 1;
        if (hour > 23) {
            hour = 0;
        }
//        time = hour + time.substring(2,5);
        time = hour + ":" + minuteStr;
        return time;
    }

    private DailyTimes toDaylightTime(DailyTimes dailyTimes) {
        dailyTimes.mFajr = toDaylightTime(dailyTimes.mFajr);
        dailyTimes.mShurooq = toDaylightTime(dailyTimes.mShurooq);
        dailyTimes.mDuhr = toDaylightTime(dailyTimes.mDuhr);
        dailyTimes.mAsr = toDaylightTime(dailyTimes.mAsr);
        dailyTimes.mMagrib = toDaylightTime(dailyTimes.mMagrib);
        dailyTimes.mIsha = toDaylightTime(dailyTimes.mIsha);

        return dailyTimes;
    }

    public DailyTimes getTodaysTimes() {
        final int day = mCalendar.get(Calendar.DAY_OF_MONTH);
        final int month = mCalendar.get(Calendar.MONTH) + 1;
        String key = day + "," + month;
        DailyTimes dailyTimes = mTimeTable.get(key);
        return dailyTimes;
    }
    public DailyTimes getTimesOf(Date date) {
        final String strDate = mDateFormat.format(date);
        return mTimeTable.get(strDate);
    }

    public void addTime(String dateStr, DailyTimes dailyTimes) {
        mTimeTable.put(dateStr, dailyTimes);
    }

    public void readPrayerTimes(InputStream inputStream) {
        List<String[]> tokenizedLines = new ArrayList<>();
        loadTable(inputStream, tokenizedLines, " ");
        try {
            //TODO replace with thread sync using Message
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // File Format:
        //# Isha  Magrib Asr   Duhr  Shurooq Fajr  Day      Month
        //# hh:mm hh:mm  hh:mm hh:mm hh:mm   hh:mm of month of year
        //# ===== =====  ===== ===== =====   ===== ======== =======
        //  06:13 04:52  02:27 11:42 06:36   05:12 1        1

        if (tokenizedLines.size() > 0) {
            for (String[] currentTokenizedLine : tokenizedLines) {
                if (currentTokenizedLine.length == 8) {
                    final String isha = currentTokenizedLine[0];
                    final String magrib = currentTokenizedLine[1];
                    final String asr = currentTokenizedLine[2];
                    final String duhr = currentTokenizedLine[3];
                    final String shurooq = currentTokenizedLine[4];
                    final String fajr = currentTokenizedLine[5];
                    final String day = currentTokenizedLine[6];
                    final String month = currentTokenizedLine[7];
                    DailyTimes dailyTimes = new DailyTimes(fajr, shurooq, duhr, asr, magrib, isha);
                    if (mDaylightTime) {
                        dailyTimes = toDaylightTime(dailyTimes);
                    }

                    final String dateStr = day + "," + month;
                    addTime(dateStr, dailyTimes);
                } else {
                    // TODO Will the following work? can a String[] be printed like this?
                    Log.e("readPrayerTimes()", "Unrecognized line - 8 columns are expected: " + currentTokenizedLine);
                }
            }
        } else {
            Log.e("readPrayerScenario()", "0 lines were read!!");
        }
    }

    public void loadTable(InputStream inputStream, List<String[]> tokenizedLines, String delimiterEpression) {
        mInputStream = inputStream;
        mDelimiterEpression = delimiterEpression;
        mTokenizedLines = tokenizedLines;
        new Thread(new Runnable() {
            public void run() {
                try {
                    load();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void load() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(mInputStream));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = TextUtils.split(line, mDelimiterEpression);
                if (line.trim().startsWith("#") || tokens.length == 0) {
                    // skip comment lines and empty/space lines
                    continue;
                }
                mTokenizedLines.add(tokens);
            }
        } finally {
            reader.close();
        }
    }

    public int getCurrentPrayerIndex() {
        final String dateFormat = "HH:mm";
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
        final Date currentTime = mCalendar.getTime();
        final Calendar temporaryCalendar = Calendar.getInstance();
        temporaryCalendar.setTime(currentTime);
        final int currentTimeInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
        Log.d(TAG, "YYY1c currentTime: " + currentTime.toString());
        final DailyTimes dailyTimes = getTodaysTimes();
        try {
            Date timeToCheck = simpleDateFormat.parse(dailyTimes.mIsha);
            if (timeToCheck == null) {
                return 0;
            }
            Log.d(TAG, "YYY1d timeToCheck: " + timeToCheck.toString());
            temporaryCalendar.setTime(timeToCheck);
            int timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes < currentTimeInMinutes) {
                return 6;
            }
            timeToCheck = simpleDateFormat.parse(dailyTimes.mMagrib);
            if (timeToCheck == null) {
                return 0;
            }
            temporaryCalendar.setTime(timeToCheck);
            timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes < currentTimeInMinutes) {
                return 5;
            }
            timeToCheck = simpleDateFormat.parse(dailyTimes.mAsr);
            if (timeToCheck == null) {
                return 0;
            }
            temporaryCalendar.setTime(timeToCheck);
            timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes < currentTimeInMinutes) {
                return 4;
            }
            timeToCheck = simpleDateFormat.parse(dailyTimes.mDuhr);
            if (timeToCheck == null) {
                return 0;
            }
            temporaryCalendar.setTime(timeToCheck);
            timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes < currentTimeInMinutes) {
                return 3;
            }
            timeToCheck = simpleDateFormat.parse(dailyTimes.mShurooq);
            if (timeToCheck == null) {
                return 0;
            }
            temporaryCalendar.setTime(timeToCheck);
            timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes < currentTimeInMinutes) {
                return 2;
            }
            timeToCheck = simpleDateFormat.parse(dailyTimes.mFajr);
            if (timeToCheck == null) {
                return 0;
            }
            temporaryCalendar.setTime(timeToCheck);
            timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes < currentTimeInMinutes) {
                return 1;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static class DailyTimes {
        DailyTimes(String fajr, String shurooq, String duhr, String asr, String magrib, String isha) {
            mFajr = fajr;
            mShurooq = shurooq;
            mDuhr = duhr;
            mAsr = asr;
            mMagrib = magrib;
            mIsha = isha;

            // remove the "0" prefix from the "hour" if it has it
            mFajr = mFajr.replaceFirst("^0", "");
            mShurooq = mShurooq.replaceFirst("^0", "");
            mDuhr = mDuhr.replaceFirst("^0", "");
            mAsr = mAsr.replaceFirst("^0", "");
            mMagrib = mMagrib.replaceFirst("^0", "");
            mIsha = mIsha.replaceFirst("^0", "");
        }
        public String mFajr;
        public String mShurooq;
        public String mDuhr;
        public String mAsr;
        public String mMagrib;
        public String mIsha;
    }

    private InputStream mInputStream;
    private String mDelimiterEpression;
    private List<String[]> mTokenizedLines;


    private ArrayMap<String, DailyTimes> mTimeTable;
    private Calendar mCalendar;
    private final SimpleDateFormat mDateFormat;
    private boolean mDaylightTime;

    private static final String TAG = PrayerTimes.class.getSimpleName();
}
