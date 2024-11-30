package com.tlab.widget;

import org.json.JSONObject;

public class DialogInterface {
    public interface OnClickListener {
        void onClick(JSONObject result);
    }

    public interface OnDismissListener {
        void onDismiss();
    }
}
