package com.tlab.dialog;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class OptionSelector extends Dialog implements DialogInterface {

    private final LinearLayout m_options;

    private final Button[] m_buttons;

    private final boolean m_multiple;
    private final String[] m_texts;
    private final boolean[] m_flags;

    public boolean[] getValue() {
        return m_flags;
    }

    public OptionSelector(Context context, String[] texts, boolean[] flags, boolean multiple) {
        super(context);

        m_multiple = multiple;
        m_texts = texts;
        m_flags = flags;

        setLayoutParams(new LayoutParams(DEFAULT_VIEW_SIZE, DEFAULT_VIEW_SIZE));
        setGravity(Gravity.CENTER);

        LinearLayout vertical = new LinearLayout(context);
        vertical.setBackgroundColor(Color.WHITE);
        vertical.setOrientation(LinearLayout.VERTICAL);
        vertical.setGravity(Gravity.CENTER);
        vertical.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(context);
        scroll.setLayoutParams(new LayoutParams((int) (DEFAULT_VIEW_SIZE * 0.75f), LayoutParams.WRAP_CONTENT));

        LinearLayout items = new LinearLayout(context);
        items.setOrientation(LinearLayout.VERTICAL);
        items.setGravity(Gravity.CENTER);
        items.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        scroll.addView(items);

        vertical.addView(scroll);

        m_options = new LinearLayout(context);
        m_options.setOrientation(LinearLayout.HORIZONTAL);
        m_options.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        m_options.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        vertical.addView(m_options);

        m_buttons = new Button[m_texts.length];

        for (int i = 0; i < m_texts.length; i++) {
            Button button = new Button(m_context);
            button.setTextSize(15);
            button.setText(texts[i]);
            button.setTextColor(Color.GREEN);
            button.setBackgroundColor(flags[i] ? Color.CYAN : Color.TRANSPARENT);

            m_buttons[i] = button;

            button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            int finalI = i;
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    flags[finalI] = !flags[finalI];
                    button.setBackgroundColor(flags[finalI] ? Color.CYAN : Color.TRANSPARENT);
                    if (!m_multiple) {
                        for (int j = 0; j < m_texts.length; j++) {
                            if (j == finalI) {
                                continue;
                            }

                            flags[j] = false;
                            m_buttons[j].setBackgroundColor(Color.TRANSPARENT);
                        }
                    }
                }
            });

            items.addView(button);
        }

        TextView dummy = new TextView(m_context);
        dummy.setText("  ");
        dummy.setTextSize(7);
        dummy.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        vertical.addView(dummy);

        addView(vertical);
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
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.OnSelectOption(option);
            }
        });
        return button;
    }
}
