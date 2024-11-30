package com.tlab.widget;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthWidget {
    public static class Init extends BaseWidget.Init {
        public static final String KEY_USERNAME = "username";
        public static final String KEY_PASSWORD = "password";
        public static final String KEY_INPUT_TYPE = "inputType";
        public static final String KEY_ONLY_PASSWORD = "onlyPassword";

        private String mUsername;
        private String mPassword;
        private int mInputType;
        private boolean mOnlyPassword;

        public Init() {

        }

        public void setUsername(String username) {
            mUsername = username;
        }

        public void setPassword(String password) {
            mPassword = password;
        }

        public void setFlag(boolean onlyPassword) {
            mOnlyPassword = onlyPassword;
        }

        public void setInputType(int inputType) {
            mInputType = inputType;
        }

        @Override
        public JSONObject toJSON() {
            try {
                JSONObject jo = super.toJSON();
                jo.put(KEY_USERNAME, mUsername != null ? mUsername : "");
                jo.put(KEY_PASSWORD, mPassword != null ? mPassword : "");
                jo.put(KEY_INPUT_TYPE, mInputType);
                jo.put(KEY_ONLY_PASSWORD, mOnlyPassword);
                return jo;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
