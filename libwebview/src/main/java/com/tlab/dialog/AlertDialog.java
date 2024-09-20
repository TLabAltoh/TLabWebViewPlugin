package com.tlab.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;

@SuppressLint("ViewConstructor")
public class AlertDialog extends Dialog implements DialogInterface {

    private final HashMap<String, OnSelectOptionListener> m_optionAndCallbackMap = new HashMap<>();

    private final TextView m_title;
    private final TextView m_body;
    private final LinearLayout m_options;

    public AlertDialog(Context context) {
        super(context);

        setLayoutParams(new LayoutParams(DEFAULT_VIEW_SIZE, DEFAULT_VIEW_SIZE));
        setGravity(Gravity.CENTER);

        LinearLayout vertical = new LinearLayout(context);

        vertical.setOrientation(LinearLayout.VERTICAL);
        vertical.setBackgroundColor(Color.WHITE);
        vertical.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        m_title = new TextView(context);
        //m_title.setBackgroundColor(Color.GREEN);
        m_title.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        m_title.setGravity(Gravity.LEFT | Gravity.TOP);
        m_title.setTextSize(32);
        vertical.addView(m_title);

        m_body = new TextView(context);
        //m_body.setBackgroundColor(Color.RED);
        m_body.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        m_body.setGravity(Gravity.CENTER);
        m_body.setTextSize(24);
        vertical.addView(m_body);

        // I know this is bad practice. But I need to dynamically rescale
        // dialog component when parent size changes. TextView is
        // convenient to use as a scalable margin.
        TextView dummy = new TextView(context);
        dummy.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        dummy.setText("  ");
        dummy.setTextSize(15);
        vertical.addView(dummy);

        m_options = new LinearLayout(context);
        m_options.setOrientation(LinearLayout.HORIZONTAL);
        m_options.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        m_options.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        vertical.addView(m_options);

        addView(vertical);
    }

    public void setMessage(String title, String body) {
        m_title.setText(title);
        m_body.setText(body);
    }

    public void setOptions(String option, final DialogInterface.OnSelectOptionListener listener) {
        TextView dummy = new TextView(m_context);
        dummy.setText("     ");
        dummy.setTextSize(15);
        dummy.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        m_options.addView(dummy);

        Button button = new Button(m_context);
        button.setTextSize(15);
        button.setText(option);
        button.setTextColor(Color.GREEN);
        button.setBackgroundColor(Color.TRANSPARENT);

        button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(v -> listener.OnSelectOption(option));

        m_options.addView(button);
    }
}