package com.mastegoane.namazatimes;

import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
        mCalendarLD = new MutableLiveData<>(Calendar.getInstance());
        // Use day and month (no minutes). 'M' is month; use non-padded pattern to match keys like "1,1".
        mDateFormat = new SimpleDateFormat("d,M");
        mDaylightTime = mCalendarLD.getValue().getTimeZone().inDaylightTime(mCalendarLD.getValue().getTime());
    }

    public LiveData<Calendar> getCalendar() {
        return mCalendarLD;
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

//    private String toTwelveHourFormat(String time) {
//        final String[] hourAndMinutes = time.split(":");
//        final String hourStr = hourAndMinutes[0];
//        final String minuteStr = hourAndMinutes[1];
//        //int hour = Integer.parseInt(time.substring(0, 2)) + 1;
//        int hour = Integer.parseInt(hourStr);
//        if (hour > 12) {
//            hour = hour - 12;
//        }
//        time = hour + ":" + minuteStr;
//        return time;
//    }
//
//    private DailyTimes toTwelveHourFormat(DailyTimes dailyTimes) {
//        dailyTimes.mFajr = toTwelveHourFormat(dailyTimes.mFajr);
//        dailyTimes.mShurooq = toTwelveHourFormat(dailyTimes.mShurooq);
//        dailyTimes.mDuhr = toTwelveHourFormat(dailyTimes.mDuhr);
//        dailyTimes.mAsr = toTwelveHourFormat(dailyTimes.mAsr);
//        dailyTimes.mMagrib = toTwelveHourFormat(dailyTimes.mMagrib);
//        dailyTimes.mIsha = toTwelveHourFormat(dailyTimes.mIsha);
//        return dailyTimes;
//    }


    public DailyTimes getTodaysTimes() {
        final int day = mCalendarLD.getValue().get(Calendar.DAY_OF_MONTH);
        final int month = mCalendarLD.getValue().get(Calendar.MONTH) + 1;
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
//                    dailyTimes = toTwelveHourFormat(dailyTimes);

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

    public void loadTable(InputStream inputStream, List<String[]> tokenizedLines, String delimiterExpression) {
        mInputStream = inputStream;
        mDelimiterExpression = delimiterExpression;
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
                String[] tokens = TextUtils.split(line, mDelimiterExpression);
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
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        final Date currentTime = mCalendarLD.getValue().getTime();
        final Calendar temporaryCalendar = Calendar.getInstance();
        temporaryCalendar.setTime(currentTime);
        final int currentTimeInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
        final DailyTimes dailyTimes = getTodaysTimes();
        try {
            Date timeToCheck = simpleDateFormat.parse(dailyTimes.mIsha);
            if (timeToCheck == null) {
                return 0;
            }
            temporaryCalendar.setTime(timeToCheck);
            int timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes <= currentTimeInMinutes) {
                return 6;
            }
            timeToCheck = simpleDateFormat.parse(dailyTimes.mMagrib);
            if (timeToCheck == null) {
                return 0;
            }
            temporaryCalendar.setTime(timeToCheck);
            timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes <= currentTimeInMinutes) {
                return 5;
            }
            timeToCheck = simpleDateFormat.parse(dailyTimes.mAsr);
            if (timeToCheck == null) {
                return 0;
            }
            temporaryCalendar.setTime(timeToCheck);
            timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes <= currentTimeInMinutes) {
                return 4;
            }
            timeToCheck = simpleDateFormat.parse(dailyTimes.mDuhr);
            if (timeToCheck == null) {
                return 0;
            }
            temporaryCalendar.setTime(timeToCheck);
            timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes <= currentTimeInMinutes) {
                return 3;
            }
            timeToCheck = simpleDateFormat.parse(dailyTimes.mShurooq);
            if (timeToCheck == null) {
                return 0;
            }
            temporaryCalendar.setTime(timeToCheck);
            timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes <= currentTimeInMinutes) {
                return 2;
            }
            timeToCheck = simpleDateFormat.parse(dailyTimes.mFajr);
            if (timeToCheck == null) {
                return 0;
            }
            temporaryCalendar.setTime(timeToCheck);
            timeToCheckInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
            if (timeToCheckInMinutes <= currentTimeInMinutes) {
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

        public String getTimeOf(String prayerName, boolean is12HourFormat) {
            String prayerTime;
            switch (prayerName.toLowerCase()) {
                case "fajr":
                    prayerTime = mFajr;
                    break;
                case "shurooq":
                    prayerTime = mShurooq;
                    break;
                case "duhr":
                    prayerTime = mDuhr;
                    break;
                case "asr":
                    prayerTime = mAsr;
                    break;
                case "magrib":
                    prayerTime = mMagrib;
                    break;
                case "isha":
                    prayerTime = mIsha;
                    break;
                default:
                    Log.e(TAG, "BUG getTimeOf() got illegal prayer name: " + prayerName);
                    prayerTime = mFajr;
                    break;
            }
            if (is12HourFormat) {
                return toTwelveHourFormat(prayerTime);
            } else {
                return prayerTime;
            }
        }

        private String toTwelveHourFormat(String time) {
            final String[] hourAndMinutes = time.split(":");
            final String hourStr = hourAndMinutes[0];
            final String minuteStr = hourAndMinutes[1];
            //int hour = Integer.parseInt(time.substring(0, 2)) + 1;
            int hour = Integer.parseInt(hourStr);
            if (hour > 12) {
                hour = hour - 12;
            }
            time = hour + ":" + minuteStr;
            return time;
        }

//        private DailyTimes toTwelveHourFormat(DailyTimes dailyTimes) {
//            dailyTimes.mFajr = toTwelveHourFormat(dailyTimes.mFajr);
//            dailyTimes.mShurooq = toTwelveHourFormat(dailyTimes.mShurooq);
//            dailyTimes.mDuhr = toTwelveHourFormat(dailyTimes.mDuhr);
//            dailyTimes.mAsr = toTwelveHourFormat(dailyTimes.mAsr);
//            dailyTimes.mMagrib = toTwelveHourFormat(dailyTimes.mMagrib);
//            dailyTimes.mIsha = toTwelveHourFormat(dailyTimes.mIsha);
//            return dailyTimes;
//        }

        private String mFajr;
        private String mShurooq;
        private String mDuhr;
        private String mAsr;
        private String mMagrib;
        private String mIsha;
    }

    public static class NextPrayer {
        private final String name;
        private final String time;

        public NextPrayer(String name, String time) {
            this.name = name;
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public String getTime() {
            return time;
        }
    }

    /**
     * Returns the next prayer (name and time). If no later prayer today is found,
     * returns fajr time of the next day when available.
     */
    public NextPrayer getNextPrayer() {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        final Date currentTime = mCalendarLD.getValue().getTime();
        final Calendar temporaryCalendar = Calendar.getInstance();
        temporaryCalendar.setTime(currentTime);
        final int currentTimeInMinutes = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);

        final DailyTimes todays = getTodaysTimes();
        if (todays == null) {
            return null;
        }

        final String[] names = new String[]{"fajr", "shurooq", "duhr", "asr", "magrib", "isha"};
        try {
            for (String name : names) {
                String timeStr = todays.getTimeOf(name, false);
                if (timeStr == null) continue;
                Date t = simpleDateFormat.parse(timeStr);
                if (t == null) continue;
                temporaryCalendar.setTime(t);
                int tMin = temporaryCalendar.get(Calendar.HOUR_OF_DAY) * 60 + temporaryCalendar.get(Calendar.MINUTE);
                if (tMin > currentTimeInMinutes) {
                    return new NextPrayer(name, timeStr);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // No later prayer today - return fajr of next day if available
        final Calendar nextDayCal = (Calendar) mCalendarLD.getValue().clone();
        nextDayCal.add(Calendar.DAY_OF_MONTH, 1);
        final int day = nextDayCal.get(Calendar.DAY_OF_MONTH);
        final int month = nextDayCal.get(Calendar.MONTH) + 1;
        final String key = day + "," + month;
        final DailyTimes nextDayTimes = mTimeTable.get(key);
        if (nextDayTimes != null) {
            return new NextPrayer("fajr", nextDayTimes.getTimeOf("fajr", false));
        }

        // Fallback - return fajr of today
        return new NextPrayer("fajr", todays.getTimeOf("fajr", false));
    }

    private InputStream mInputStream;
    private String mDelimiterExpression;
    private List<String[]> mTokenizedLines;

    private ArrayMap<String, DailyTimes> mTimeTable;
    private final MutableLiveData<Calendar> mCalendarLD;
    private final SimpleDateFormat mDateFormat;
    private boolean mDaylightTime;

    private static final String TAG = PrayerTimes.class.getSimpleName();
}
