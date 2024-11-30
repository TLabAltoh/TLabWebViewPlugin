package com.tlab.widget;

import org.json.JSONException;
import org.json.JSONObject;

public class ColorWidget {
    public static class Init extends BaseWidget.Init {
        public static final String KEY_COLOR = "color";

        private int mColor;

        public void setColor(int color) {
            mColor = color;
        }

        @Override
        public JSONObject toJSON() {
            try {
                JSONObject jo = super.toJSON();
                jo.put(KEY_COLOR, mColor);
                return jo;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
