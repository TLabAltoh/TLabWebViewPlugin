package com.tlab.dialog;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ColorPicker extends Dialog implements DialogInterface {

    private final int DEFAULT_SLIDER_SIZE_X = 500;
    private final int DEFAULT_SLIDER_SIZE_Y = 100;

    private final LinearLayout m_options;
    private final LinearLayout m_vertical;
    private int m_r;
    private int m_g;
    private int m_b;

    public String getValue() {
        return String.format("#%02x%02x%02x", m_r, m_g, m_b);
    }

    public ColorPicker(Context context) {
        super(context);

        setLayoutParams(new LayoutParams(DEFAULT_VIEW_SIZE, DEFAULT_VIEW_SIZE));
        setGravity(Gravity.CENTER);

        m_vertical = new LinearLayout(context);
        m_vertical.setOrientation(LinearLayout.VERTICAL);
        m_vertical.setGravity(Gravity.CENTER);
        m_vertical.setLayoutParams(new LayoutParams((int) (DEFAULT_VIEW_SIZE * 0.75f), LayoutParams.WRAP_CONTENT));

        m_r = 126;
        m_g = 126;
        m_b = 126;
        SeekBar r = getSeekBar(context, 0, 255, m_r, DEFAULT_SLIDER_SIZE_X, DEFAULT_SLIDER_SIZE_Y, new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_r = progress;
                m_vertical.setBackgroundColor(Color.rgb(m_r, m_g, m_b));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        m_vertical.addView(r);

        SeekBar g = getSeekBar(context, 0, 255, m_g, DEFAULT_SLIDER_SIZE_X, DEFAULT_SLIDER_SIZE_Y, new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_g = progress;
                m_vertical.setBackgroundColor(Color.rgb(m_r, m_g, m_b));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        m_vertical.addView(g);

        SeekBar b = getSeekBar(context, 0, 255, m_b, DEFAULT_SLIDER_SIZE_X, DEFAULT_SLIDER_SIZE_Y, new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_b = progress;
                m_vertical.setBackgroundColor(Color.rgb(m_r, m_g, m_b));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        m_vertical.setBackgroundColor(Color.rgb(m_r, m_g, m_b));
        m_vertical.addView(b);

        m_options = new LinearLayout(context);
        m_options.setOrientation(LinearLayout.HORIZONTAL);
        m_options.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        m_options.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        m_vertical.addView(m_options);

        TextView dummy = new TextView(m_context);
        dummy.setText("  ");
        dummy.setTextSize(7);
        dummy.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        m_vertical.addView(dummy);

        addView(m_vertical);
    }

    private SeekBar getSeekBar(Context context, int min, int max, int init, int width, int height, SeekBar.OnSeekBarChangeListener listener) {
        SeekBar seekbar = new SeekBar(context);
        seekbar.setMin(min);
        seekbar.setMax(max);
        seekbar.setProgress(init);
        seekbar.setOnSeekBarChangeListener(listener);
        seekbar.setLayoutParams(new LayoutParams(width, height));
        return seekbar;
    }

    public void setOptions(String option, final DialogInterface.OnSelectOptionListener listener) {
        TextView dummy = new TextView(m_context);
        dummy.setText("  ");
        dummy.setTextSize(15);
        dummy.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        m_options.addView(dummy);

        Button button = getButton(option, listener);

        m_options.addView(button);

        dummy = new TextView(m_context);
        dummy.setText("  ");
        dummy.setTextSize(15);
        dummy.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        m_options.addView(dummy);
    }

    private Button getButton(String option, OnSelectOptionListener listener) {
        Button button = new Button(m_context);
        button.setPadding(10, 10, 10, 10);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setTextSize(15);
        button.setText(option);
        button.setTextColor(Color.GREEN);
        button.setBackgroundColor(Color.WHITE);

        button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(v -> listener.OnSelectOption(option));
        return button;
    }
}
