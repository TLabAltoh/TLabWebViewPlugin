package com.tlab.dialog;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class Dialog extends RelativeLayout implements DialogInterface {

    protected final Context mContext;

    protected static final int DEFAULT_VIEW_SIZE = 1024;

    protected int mViewSize;

    public Dialog(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
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
