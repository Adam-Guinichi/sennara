package com.sennara.app.util;

import android.content.Context;

import com.sennara.app.R;
import com.sennara.app.data.Db;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Credits {

    public static JSONArray all(Context c) {
        String json = Db.get(c).getValue("credits");
        if (json == null) {
            return new JSONArray();
        }
        try {
            return new JSONArray(json);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public static JSONObject of(Context c, String path) {
        JSONArray all = all(c);
        for (int i = 0; i < all.length(); i++) {
            JSONObject o = all.optJSONObject(i);
            if (o != null && path.equals(o.optString("file"))) {
                return o;
            }
        }
        return null;
    }

    public static String line(Context c, String path) {
        if (path == null) {
            return "";
        }
        JSONObject o = of(c, path);
        if (o == null) {
            return "";
        }
        return c.getString(R.string.photo_credit, o.optString("author"), o.optString("license"));
    }
}
