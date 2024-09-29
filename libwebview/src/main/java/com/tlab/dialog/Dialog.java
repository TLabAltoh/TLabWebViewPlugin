package com.tlab.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public class Dialog extends RelativeLayout implements DialogInterface {

    protected final Context m_context;
    protected final LinearLayout m_options;

    protected static final int DEFAULT_VIEW_SIZE = 1024;

    public Dialog(Context context) {
        super(context);

        m_context = context;

        m_options = new LinearLayout(context);
        m_options.setOrientation(LinearLayout.HORIZONTAL);
        m_options.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        m_options.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    protected GradientDrawable getBackGround(int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.WHITE);
        shape.setCornerRadius(radius);
        return shape;
    }

    protected TextView getOptionLeftMargin() {
        String leftMargin = "    ";
        TextView margin = new TextView(m_context);
        margin.setText(leftMargin);
        margin.setTextSize(15);
        margin.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        return margin;
    }

    protected Button getOptionButton(String option, OnSelectOptionListener listener) {
        Button button = new Button(m_context);

        button.setPadding(10, 10, 10, 10);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);

        button.setTextSize(15);
        button.setText(option);
        button.setTextColor(Color.GREEN);
        button.setBackground(getBackGround(16));

        button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.OnSelectOption(option);
            }
        });
        return button;
    }

    public void setOptions(String option, final DialogInterface.OnSelectOptionListener listener) {
        m_options.addView(getOptionButton(option, listener));
        m_options.addView(getOptionLeftMargin());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    public void setScale(int parentSize) {
        setScaleX((float) parentSize / DEFAULT_VIEW_SIZE);
        setScaleY((float) parentSize / DEFAULT_VIEW_SIZE);
        setTranslationX((float) (parentSize - DEFAULT_VIEW_SIZE) / 2);
        setTranslationY((float) (parentSize - DEFAULT_VIEW_SIZE) / 2);
    }

    public static <T extends View> ArrayList<T> getViewsByType(ViewGroup root, Class<T> tClass) {
        final ArrayList<T> result = new ArrayList<>();
        int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = root.getChildAt(i);
            if (child instanceof ViewGroup) {
                result.addAll(getViewsByType((ViewGroup) child, tClass));
            }

            if (tClass.isInstance(child)) {
                result.add(tClass.cast(child));
            }
        }
        return result;
    }
}
