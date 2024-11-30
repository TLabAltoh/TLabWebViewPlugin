package com.tlab.widget;

import android.app.Activity;

import com.tlab.util.Common;

import org.json.JSONException;
import org.json.JSONObject;

public class BaseWidget {
    public enum Type {
        None, Auth, Color, DateTime, Select, Text
    }

    public abstract static class Init extends Common.JSONSerialisable {
        public static final String KEY_TITLE = "title";
        public static final String KEY_MESSAGE = "message";

        private String mTitle = "";
        private String mMessage = "";

        public Init(String title, String message) {
            setTitle(title);
            setMessage(message);
        }

        public Init(String title) {
            setTitle(title);
        }

        public Init() {
        }

        public void setTitle(String title) {
            mTitle = title;
        }

        public void setMessage(String message) {
            mMessage = message;
        }

        @Override
        public JSONObject toJSON() {
            try {
                JSONObject jo = new JSONObject();
                jo.put(KEY_TITLE, mTitle != null ? mTitle : "");
                jo.put(KEY_MESSAGE, mMessage != null ? mMessage : "");
                return jo;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
