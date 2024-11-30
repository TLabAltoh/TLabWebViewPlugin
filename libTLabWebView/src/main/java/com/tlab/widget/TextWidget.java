package com.tlab.widget;

import android.text.InputType;

import org.json.JSONException;
import org.json.JSONObject;

public class TextWidget {
    public static class Init extends BaseWidget.Init {
        public static final String KEY_TEXT = "text";
        public static final String KEY_HINT = "hint";
        public static final String KEY_INPUT_TYPE = "inputType";

        private String mText;
        private String mHint;
        private int mInputType = InputType.TYPE_NULL;

        public Init(String title, String message) {
            super(title, message);
        }

        public void setText(String text) {
            mText = text;
        }

        public void setHint(String hint) {
            mHint = hint;
        }

        public void setInputType(int inputType) {
            mInputType = inputType;
        }

        @Override
        public JSONObject toJSON() {
            try {
                JSONObject jo = new JSONObject();
                jo.put(KEY_TEXT, mText != null ? mText : "");
                jo.put(KEY_HINT, mHint != null ? mHint : "");
                jo.put(KEY_INPUT_TYPE, mInputType);
                return jo;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
