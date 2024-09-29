package com.tlab.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Calendar;

@SuppressLint("ViewConstructor")
public class DataTimePickerDialog extends Dialog implements DialogInterface {
    private int m_year = 0;
    private int m_month = 0;
    private int m_day = 0;

    private int m_hour = 0;
    private int m_minutes = 0;

    private boolean m_date = false;
    private boolean m_time = false;

    private final int DEFAULT_NUMBER_PICKER_SIZE_X = 150;
    private final int DEFAULT_NUMBER_PICKER_SIZE_Y = 300;

    @SuppressLint("DefaultLocale")
    public String getValue() {
        String value = "";
        if (m_date) {
            value += String.format("%04d", m_year) + "-" + String.format("%02d", m_month) + "-" + String.format("%02d", m_day);
        }
        if (m_time) {
            value += (!value.isEmpty() ? "T" : "") + String.format("%02d", m_hour) + ":" + String.format("%02d", m_minutes);
        }
        return value;
    }

    public DataTimePickerDialog(Context context, boolean date, boolean time) {
        super(context);

        m_date = date;
        m_time = time;

        setLayoutParams(new LayoutParams(DEFAULT_VIEW_SIZE, DEFAULT_VIEW_SIZE));
        setGravity(Gravity.CENTER);

        LinearLayout vertical = new LinearLayout(context);
        vertical.setBackground(getBackGround(16));
        vertical.setOrientation(LinearLayout.VERTICAL);
        vertical.setGravity(Gravity.CENTER);
        vertical.setLayoutParams(new LayoutParams((int) (DEFAULT_VIEW_SIZE * 0.75f), LayoutParams.WRAP_CONTENT));

        Calendar c = Calendar.getInstance();

        if (date) {
            LinearLayout datePicker = new LinearLayout(context);
            //datePicker.setBackgroundColor(Color.GREEN);
            datePicker.setOrientation(LinearLayout.HORIZONTAL);
            datePicker.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            m_year = c.get(Calendar.YEAR);
            NumberPicker yearPicker = getNumberPicker(context, 0, 3000, m_year, (picker, oldVal, newVal) -> m_year = newVal);
            datePicker.addView(yearPicker);

            m_month = c.get(Calendar.MONTH) + 1;
            NumberPicker monthPicker = getNumberPicker(context, 1, 12, m_month, (picker, oldVal, newVal) -> m_month = newVal);
            datePicker.addView(monthPicker);

            m_day = c.get(Calendar.DATE);
            NumberPicker dayPicker = getNumberPicker(context, 1, 31, m_day, (picker, oldVal, newVal) -> m_day = newVal);
            datePicker.addView(dayPicker);

            vertical.addView(datePicker);
        }

        if (time) {
            LinearLayout timePicker = new LinearLayout(context);
            //timePicker.setBackgroundColor(Color.RED);
            timePicker.setOrientation(LinearLayout.HORIZONTAL);
            timePicker.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            m_hour = c.get(Calendar.HOUR);
            NumberPicker hourPicker = getNumberPicker(context, 0, 23, m_hour, (picker, oldVal, newVal) -> m_hour = newVal);
            timePicker.addView(hourPicker);

            m_minutes = c.get(Calendar.MINUTE);
            NumberPicker minutesPicker = getNumberPicker(context, 0, 59, m_minutes, (picker, oldVal, newVal) -> m_minutes = newVal);
            timePicker.addView(minutesPicker);

            vertical.addView(timePicker);
        }

        vertical.addView(m_options);

        TextView dummy = new TextView(m_context);
        dummy.setText(" ");
        dummy.setTextSize(7);
        dummy.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        vertical.addView(dummy);

        addView(vertical);
    }

    private NumberPicker getNumberPicker(Context context, int min, int max, int init, NumberPicker.OnValueChangeListener listener) {
        NumberPicker numberPicker = new NumberPicker(context);
        numberPicker.setMinValue(min);
        numberPicker.setMaxValue(max);
        numberPicker.setValue(init);
        numberPicker.setOnValueChangedListener(listener);
        numberPicker.setLayoutParams(new LayoutParams(150, 300));
        return numberPicker;
    }
}
