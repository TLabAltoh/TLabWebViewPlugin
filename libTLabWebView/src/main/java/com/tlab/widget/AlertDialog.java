package com.tlab.widget;

import com.tlab.util.Common;

import org.json.JSONException;
import org.json.JSONObject;

public class AlertDialog {
    public interface Callback {
        void onResult(int action, JSONObject jo);

        void dismiss();
    }

    public static class Overlay {
        public static class Init extends Common.JSONSerialisable {
            public static final String KEY_TYPE = "type";
            public static final String KEY_INIT = "init";

            private int mType = BaseWidget.Type.None.ordinal();
            private String mInit = "";

            public void set(BaseWidget.Init init) {
                if (init instanceof AuthWidget.Init) mType = BaseWidget.Type.Auth.ordinal();
                if (init instanceof ColorWidget.Init) mType = BaseWidget.Type.Color.ordinal();
                if (init instanceof DateTimeWidget.Init) mType = BaseWidget.Type.DateTime.ordinal();
                if (init instanceof SelectWidget.Init) mType = BaseWidget.Type.Select.ordinal();
                if (init instanceof TextWidget.Init) mType = BaseWidget.Type.Text.ordinal();

                mInit = init.marshall();
            }

            @Override
            public JSONObject toJSON() {
                try {
                    JSONObject jo = super.toJSON();
                    jo.put(KEY_TYPE, mType);
                    jo.put(KEY_INIT, mInit != null ? mInit : "");
                    return jo;
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static class Init extends BaseWidget.Init {
        public static final String KEY_POSITIVE_LABEL = "positiveLabel";
        public static final String KEY_NEUTRAL_LABEL = "neutralLabel";
        public static final String KEY_NEGATIVE_LABEL = "negativeLabel";
        public static final String KEY_POSITIVE = "positive";
        public static final String KEY_NEUTRAL = "neutral";
        public static final String KEY_NEGATIVE = "negative";
        public static final String KEY_REASON = "reason";
        public static final String KEY_OVERLAY = "overlay";

        public static class Result {
            public static final int POSITIVE = 1;
            public static final int NEUTRAL = 0;
            public static final int NEGATIVE = -1;
        }

        public static class Reason {
            public static final int PROMPT = 0;
            public static final int ERROR = 1;
        }

        protected DialogInterface.OnClickListener mPositiveListener;
        protected boolean mPositive;
        protected String mPositiveLabel = "";

        protected DialogInterface.OnClickListener mNeutralListener;
        protected boolean mNeutral;
        protected String mNeutralLabel = "";

        protected DialogInterface.OnClickListener mNegativeListener;
        protected boolean mNegative;
        protected String mNegativeLabel = "";

        protected int mReason = Reason.PROMPT;

        protected DialogInterface.OnDismissListener mOnDismissListener;

        protected Overlay.Init mOverlay = new Overlay.Init();

        public Init(int reason, String title, String message) {
            super(title, message);
            setReason(reason);
        }

        public Init(int reason, String title) {
            super(title);
            setReason(reason);
        }

        public Init(int reason) {
            setReason(reason);
        }

        public void setReason(int reason) {
            mReason = reason;
        }

        public void setPositive(String label, DialogInterface.OnClickListener listener) {
            mPositive = true;
            mPositiveLabel = label;
            mPositiveListener = listener;
        }

        public void positive(JSONObject jo) {
            if (mPositiveListener != null) mPositiveListener.onClick(jo);
        }

        public void setNegative(String label, DialogInterface.OnClickListener listener) {
            mNegative = true;
            mNegativeLabel = label;
            mNegativeListener = listener;
        }

        public void negative(JSONObject jo) {
            if (mNegativeListener != null) mNegativeListener.onClick(jo);
        }

        public void setNeutral(String label, DialogInterface.OnClickListener listener) {
            mNeutral = true;
            mNeutralLabel = label;
            mNeutralListener = listener;
        }

        public void neutral(JSONObject jo) {
            if (mNeutralListener != null) mNeutralListener.onClick(jo);
        }

        public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
            mOnDismissListener = listener;
        }

        public void dismiss() {
            if (mOnDismissListener != null) mOnDismissListener.onDismiss();
        }

        public void setOverlay(BaseWidget.Init init) {
            mOverlay.set(init);
        }

        public Callback getOnResultListener() {
            return new Callback() {
                @Override
                public void onResult(int action, JSONObject result) {
                    switch (action) {
                        case Result.POSITIVE:
                            positive(result);
                            break;
                        case Result.NEUTRAL:
                            neutral(result);
                            break;
                        case Result.NEGATIVE:
                            negative(result);
                            break;
                        default:
                            if (mOnDismissListener != null) mOnDismissListener.onDismiss();
                            break;
                    }
                }

                @Override
                public void dismiss() {
                    AlertDialog.Init.this.dismiss();
                }
            };
        }

        @Override
        public JSONObject toJSON() {
            JSONObject jo = super.toJSON();
            try {
                jo.put(KEY_POSITIVE, mPositive);
                jo.put(KEY_POSITIVE_LABEL, mPositiveLabel != null ? mPositiveLabel : "");

                jo.put(KEY_NEUTRAL, mNeutral);
                jo.put(KEY_NEUTRAL_LABEL, mNeutralLabel != null ? mNeutralLabel : "");

                jo.put(KEY_NEGATIVE, mNegative);
                jo.put(KEY_NEGATIVE_LABEL, mNegativeLabel != null ? mNegativeLabel : "");

                jo.put(KEY_REASON, mReason);

                jo.put(KEY_OVERLAY, mOverlay != null ? mOverlay.toJSON() : new JSONObject());
                return jo;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
