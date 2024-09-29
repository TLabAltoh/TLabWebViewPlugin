package com.tlab.dialog;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class ColorPicker extends Dialog implements DialogInterface {

    private final int DEFAULT_SLIDER_SIZE_X = 500;
    private final int DEFAULT_SLIDER_SIZE_Y = 100;

    private final GradientDrawable m_colorPreview;
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

        LinearLayout vertical = new LinearLayout(context);
        vertical.setOrientation(LinearLayout.VERTICAL);
        vertical.setGravity(Gravity.CENTER);
        vertical.setLayoutParams(new LayoutParams((int) (DEFAULT_VIEW_SIZE * 0.75f), LayoutParams.WRAP_CONTENT));

        m_r = 126;
        m_g = 126;
        m_b = 126;
        SeekBar r = getSeekBar(context, m_r, new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_r = progress;
                m_colorPreview.setColor(Color.rgb(m_r, m_g, m_b));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        vertical.addView(r);

        SeekBar g = getSeekBar(context, m_g, new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_g = progress;
                m_colorPreview.setColor(Color.rgb(m_r, m_g, m_b));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        vertical.addView(g);

        SeekBar b = getSeekBar(context, m_b, new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                m_b = progress;
                m_colorPreview.setColor(Color.rgb(m_r, m_g, m_b));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        m_colorPreview = getBackGround(16);
        m_colorPreview.setColor(Color.rgb(m_r, m_g, m_b));
        vertical.setBackground(m_colorPreview);
        vertical.addView(b);

        vertical.addView(m_options);

        TextView dummy = new TextView(m_context);
        dummy.setText(" ");
        dummy.setTextSize(7);
        dummy.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        vertical.addView(dummy);

        addView(vertical);
    }

    private SeekBar getSeekBar(Context context, int init, SeekBar.OnSeekBarChangeListener listener) {
        SeekBar seekbar = new SeekBar(context);
        seekbar.setMin(0);
        seekbar.setMax(255);
        seekbar.setProgress(init);
        seekbar.setOnSeekBarChangeListener(listener);
        seekbar.setLayoutParams(new LayoutParams(500, 100));
        return seekbar;
    }
}
