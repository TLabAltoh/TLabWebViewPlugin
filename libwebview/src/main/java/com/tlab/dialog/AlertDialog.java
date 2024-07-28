package com.tlab.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressLint("ViewConstructor")
public class AlertDialog extends Dialog implements DialogInterface {

    private final HashMap<String, OnSelectOptionListener> mOptionAndCallbackMap = new HashMap<>();

    private final TextView mTitle;
    private final TextView mBody;
    private final TextView mDummy;

    private final LinearLayout mOptions;

    public AlertDialog(Context context, int viewSize) {
        super(context);

        mViewSize = viewSize;

        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setGravity(Gravity.CENTER);

        LinearLayout layout = new LinearLayout(context);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        mTitle = new TextView(context);
        mTitle.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mTitle.setGravity(Gravity.LEFT | Gravity.TOP);
        //mTitle.setBackgroundColor(Color.RED);
        layout.addView(mTitle);

        mBody = new TextView(context);
        mBody.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mBody.setGravity(Gravity.CENTER);
        //mBody.setBackgroundColor(Color.GREEN);
        layout.addView(mBody);

        // I know this is bad practice. But I need to dynamically rescale
        // dialog component when parent size changes. TextView is
        // convenient to use as a scalable margin.
        mDummy = new TextView(context);
        mDummy.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mDummy.setText("  ");
        layout.addView(mDummy);

        mOptions = new LinearLayout(context);
        mOptions.setOrientation(LinearLayout.HORIZONTAL);
        mOptions.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        //mOptions.setBackgroundColor(Color.BLUE);
        mOptions.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        layout.addView(mOptions);

        addView(layout);
    }

    public void setMessage(String title, String body) {
        mTitle.setText(title);
        mBody.setText(body);
    }

    public void setOptions(String option, final DialogInterface.OnSelectOptionListener listener) {
        TextView dummy = new TextView(mContext);
        dummy.setText("     ");
        dummy.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        mOptions.addView(dummy);

        Button button = new Button(mContext);
        button.setText(option);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setTextColor(Color.GREEN);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(0, 0, 0, 0);

        button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.OnSelectOption(option);
            }
        });

        mOptions.addView(button);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        mViewSize = Math.min(getWidth(), getHeight());

        mTitle.setTextSize(32 * (float) mViewSize / DEFAULT_VIEW_SIZE);
        mBody.setTextSize(24 * (float) mViewSize / DEFAULT_VIEW_SIZE);
        mDummy.setTextSize(5 * (float) mViewSize / DEFAULT_VIEW_SIZE);

        ArrayList<TextView> texts = getViewsByType(mOptions, TextView.class);
        texts.forEach((text -> {
            text.setTextSize(15 * (float) mViewSize / DEFAULT_VIEW_SIZE);
        }));
    }
}
