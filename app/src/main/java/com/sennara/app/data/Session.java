package com.sennara.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class Session {
    public static final String SERVER = "https://sennara.alwaysdata.net/";

    public static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("sennara", Context.MODE_PRIVATE);
    }

    // ================================================================ login

    public static boolean loggedIn(Context c) {
        return !token(c).isEmpty();
    }

    public static String token(Context c) {
        return prefs(c).getString("token", "");
    }

    public static void save(Context c, JSONObject user, String token) {
        SharedPreferences.Editor editor = prefs(c).edit();
        editor.putString("token", token);
        editor.putString("user", user.toString());
        editor.apply();
    }

    public static void updateUser(Context c, JSONObject user) {
        if (user == null) {
            return;
        }
        SharedPreferences.Editor editor = prefs(c).edit();
        editor.putString("user", user.toString());
        editor.apply();
    }

    public static void logout(Context c) {
        SharedPreferences.Editor editor = prefs(c).edit();
        editor.remove("token");
        editor.remove("user");
        editor.remove("last_sync");
        editor.apply();
    }

    private static JSONObject user(Context c) {
        try {
            return new JSONObject(prefs(c).getString("user", "{}"));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public static int userId(Context c) {
        return user(c).optInt("id");
    }

    public static String name(Context c) {
        return user(c).optString("name");
    }

    public static String email(Context c) {
        return user(c).optString("email");
    }

    public static String role(Context c) {
        return user(c).optString("role", "fisher");
    }

    public static String phone(Context c) {
        return user(c).optString("phone");
    }

    public static String city(Context c) {
        return user(c).optString("city");
    }

    public static String level(Context c) {
        return user(c).optString("level", "beginner");
    }

    public static String styles(Context c) {
        return user(c).optString("styles");
    }

    public static String memberSince(Context c) {
        return user(c).optString("created_at");
    }

    public static boolean isAdmin(Context c) {
        return role(c).equals("admin");
    }

    public static boolean isSeller(Context c) {
        return role(c).equals("seller");
    }

    // ================================================================ server address

    public static String server(Context c) {
        return SERVER;
    }

    // ================================================================ settings

    public static boolean notifications(Context c) {
        return prefs(c).getBoolean("notifications", true);
    }

    public static void setNotifications(Context c, boolean on) {
        SharedPreferences.Editor editor = prefs(c).edit();
        editor.putBoolean("notifications", on);
        editor.apply();
    }

    public static boolean alerts(Context c) {
        return prefs(c).getBoolean("alerts", true);
    }

    public static void setAlerts(Context c, boolean on) {
        SharedPreferences.Editor editor = prefs(c).edit();
        editor.putBoolean("alerts", on);
        editor.apply();
    }

    public static boolean onboarded(Context c) {
        return prefs(c).getBoolean("onboarded_" + userId(c), false);
    }

    public static void setOnboarded(Context c) {
        SharedPreferences.Editor editor = prefs(c).edit();
        editor.putBoolean("onboarded_" + userId(c), true);
        editor.apply();
    }

    public static long lastSync(Context c) {
        return prefs(c).getLong("last_sync", 0);
    }

    public static void setLastSync(Context c, long time) {
        SharedPreferences.Editor editor = prefs(c).edit();
        editor.putLong("last_sync", time);
        editor.apply();
    }

    // ================================================================ recent searches (spots screen)

    public static String[] recentSearches(Context c) {
        String saved = prefs(c).getString("recent_searches", "");
        if (saved.isEmpty()) {
            return new String[0];
        }
        return saved.split("\n");
    }

    public static void addRecentSearch(Context c, String query) {
        query = query.trim();
        if (query.length() < 2) {
            return;
        }
        String text = query;
        int kept = 1;
        for (String old : recentSearches(c)) {
            if (kept >= 5) {
                break;
            }
            if (!old.equalsIgnoreCase(query)) {
                text = text + "\n" + old;
                kept++;
            }
        }
        SharedPreferences.Editor editor = prefs(c).edit();
        editor.putString("recent_searches", text);
        editor.apply();
    }
}
