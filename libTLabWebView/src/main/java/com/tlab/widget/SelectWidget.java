package com.tlab.widget;

import com.tlab.util.Common;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.geckoview.GeckoSession;

import java.util.ArrayList;

public class SelectWidget {
    public static class Type {
        /**
         * Display choices in a menu that dismisses as soon as an item is chosen.
         */
        public static final int MENU = 1;

        /**
         * Display choices in a list that allows a single selection.
         */
        public static final int SINGLE = 2;

        /**
         * Display choices in a list that allows multiple selections.
         */
        public static final int MULTIPLE = 3;

        protected Type() {
        }
    }

    public static class ModifiableChoice extends Common.JSONSerialisable {
        public static final String KEY_SELECTED = "selected";
        public static final String KEY_TYPE = "type";
        public static final String KEY_LABEL = "label";

        public static class Type {
            public static final int DEFAULT = 1;

            public static final int GROUP = 2;

            public static final int SEPARATOR = 3;

            protected Type() {
            }
        }

        public boolean selected;
        public int type;
        public String label;
        public final GeckoSession.PromptDelegate.ChoicePrompt.Choice choice;

        public ModifiableChoice(GeckoSession.PromptDelegate.ChoicePrompt.Choice c) {
            choice = c;
            selected = c.selected;
            type = Type.DEFAULT;
            if (c.items != null) type = Type.GROUP;
            if (c.separator) type = Type.SEPARATOR;
            label = c.label;
        }

        @Override
        public JSONObject toJSON() {
            try {
                JSONObject jo = super.toJSON();
                jo.put(KEY_SELECTED, selected);
                jo.put(KEY_TYPE, type);
                jo.put(KEY_LABEL, label != null ? label : "");
                return jo;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Init extends BaseWidget.Init {
        public static final String KEY_OPTIONS = "options";
        public static final String KEY_TYPE = "type";

        private ArrayList<JSONObject> mOptions;
        private int mType;

        public void set(int type, ArrayList<JSONObject> options) {
            mType = type;
            mOptions = options;
        }

        @Override
        public JSONObject toJSON() {
            try {
                JSONObject jo = super.toJSON();
                jo.put(KEY_OPTIONS, new JSONArray(mOptions));
                jo.put(KEY_TYPE, mType);
                return jo;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
