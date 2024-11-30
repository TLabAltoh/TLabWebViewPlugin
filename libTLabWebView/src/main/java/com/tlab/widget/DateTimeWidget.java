package com.tlab.widget;

import org.json.JSONException;
import org.json.JSONObject;

public class DateTimeWidget {
    public static class Init extends BaseWidget.Init {
        public static final String KEY_TYPE = "type";
        public static final String KEY_DATE = "date";
        public static final String KEY_TIME = "time";
        public static final String KEY_MIN_DATE = "minDate";
        public static final String KEY_MAX_DATE = "maxDate";
        public static final String KEY_YEAR = "year";
        public static final String KEY_MONTH = "month";
        public static final String KEY_DAY_OF_MONTH = "dayOfMonth";
        public static final String KEY_HOUR = "hour";
        public static final String KEY_Minutes = "minutes";

        private int mType;

        private boolean mDate;
        private boolean mTime;

        private long mMinDate = -1;
        private long mMaxDate = -1;

        private int mYear;
        private int mMonth;
        private int mDayOfMonth;

        private int mHour;
        private int mMinutes;

        public void setType(int type) {
            mType = type;
        }

        public void setDate(int year, int month, int dayOfMonth) {
            setYear(year);
            setMonth(month);
            setDayOfMonth(dayOfMonth);
            mDate = true;
        }

        public void setYear(int year) {
            mYear = year;
            mDate = true;
        }

        public void setMonth(int month) {
            mMonth = month;
            mDate = true;
        }

        public void setDayOfMonth(int dayOfMonth) {
            mDayOfMonth = dayOfMonth;
            mDate = true;
        }

        public void setMinDate(long date) {
            mMinDate = date;
        }

        public void setMaxDate(long date) {
            mMaxDate = date;
        }

        public void setTime(int hour, int minute) {
            setHour(hour);
            setMinute(minute);
            mTime = true;
        }

        public void setHour(int hour) {
            mHour = hour;
            mTime = true;
        }

        public void setMinute(int minute) {
            mMinutes = minute;
            mTime = true;
        }

        @Override
        public JSONObject toJSON() {
            try {
                JSONObject jo = super.toJSON();
                jo.put(KEY_TYPE, mType);
                jo.put(KEY_DATE, mDate);
                jo.put(KEY_TIME, mTime);
                jo.put(KEY_MIN_DATE, mMinDate); // TODO: long --> int ?
                jo.put(KEY_MAX_DATE, mMaxDate);
                jo.put(KEY_YEAR, mYear);
                jo.put(KEY_MONTH, mMonth);
                jo.put(KEY_DAY_OF_MONTH, mDayOfMonth);
                jo.put(KEY_HOUR, mHour);
                jo.put(KEY_Minutes, mMinutes);
                return jo;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
