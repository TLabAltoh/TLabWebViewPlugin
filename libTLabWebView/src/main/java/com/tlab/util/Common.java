package com.tlab.util;

import org.json.JSONException;
import org.json.JSONObject;

public class Common {
    public abstract static class JSONSerialisable {

        public JSONObject toJSON() {
            return new JSONObject();
        }

        public void overwriteJSON(JSONObject jo) {

        }

        public String marshall() {
            return toJSON().toString();
        }

        public void unMarshall(String js) {
            try {
                overwriteJSON(new JSONObject(js));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
