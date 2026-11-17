package com.sennara.app.data;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.sennara.app.forecast.Conditions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class WeatherRepo {

    public interface Listener {
        void onDone(boolean ok);
    }

    private static final long MAX_AGE = 45 * 60000L;
    private static boolean running = false;

    public static long updatedAt(Context c) {
        String value = Db.get(c).getValue("weather_at");
        if (value == null) {
            return 0;
        }
        return Long.parseLong(value);
    }

    public static boolean fresh(Context c) {
        return System.currentTimeMillis() - updatedAt(c) < MAX_AGE;
    }

    public static void refresh(Context c, boolean force, Listener listener) {
        if ((!force && fresh(c)) || running) {
            if (listener != null) {
                listener.onDone(true);
            }
            return;
        }
        running = true;
        WeatherTask task = new WeatherTask(c, listener);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static class WeatherTask extends AsyncTask<Void, Void, Boolean> {
        private Context context;
        private Listener listener;

        WeatherTask(Context c, Listener listener) {
            this.context = c.getApplicationContext();
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(Void... nothing) {
            return download(context);
        }

        @Override
        protected void onPostExecute(Boolean ok) {
            running = false;
            if (listener != null) {
                listener.onDone(ok);
            }
        }
    }

    public static boolean download(Context c) {
        ArrayList<Models.Spot> spots = Db.get(c).spots();
        if (spots.isEmpty()) {
            return false;
        }

        String lat = "";
        String lng = "";
        for (int i = 0; i < spots.size(); i++) {
            if (i > 0) {
                lat = lat + ",";
                lng = lng + ",";
            }
            lat = lat + String.format(Locale.US, "%.4f", spots.get(i).lat);
            lng = lng + String.format(Locale.US, "%.4f", spots.get(i).lng);
        }
        String where = "latitude=" + lat + "&longitude=" + lng + "&timezone=auto&forecast_days=3&timeformat=unixtime";

        try {
            String weatherUrl = "https://api.open-meteo.com/v1/forecast?" + where
                    + "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,cloud_cover,pressure_msl,"
                    + "wind_speed_10m,wind_direction_10m,wind_gusts_10m&wind_speed_unit=kmh";
            JSONArray weather = toArray(Api.get(weatherUrl));

            JSONArray marine = null;
            try {
                String marineUrl = "https://marine-api.open-meteo.com/v1/marine?" + where
                        + "&hourly=wave_height,wave_period,sea_surface_temperature";
                marine = toArray(Api.get(marineUrl));
            } catch (Exception e) {
            }

            HashMap<Integer, ArrayList<Conditions.Hour>> bySpot = new HashMap<>();
            for (int i = 0; i < spots.size() && i < weather.length(); i++) {
                JSONObject marineOfSpot = null;
                if (marine != null) {
                    marineOfSpot = marine.optJSONObject(i);
                }
                bySpot.put(spots.get(i).id, readHours(weather.getJSONObject(i), marineOfSpot));
            }
            Db.get(c).replaceWeather(bySpot);
            Db.get(c).setValue("weather_at", String.valueOf(System.currentTimeMillis()));
            return true;
        } catch (Exception e) {
            Log.w("Sennara", "Weather download failed", e);
            return false;
        }
    }

    private static JSONArray toArray(String json) throws Exception {
        String text = json.trim();
        if (text.startsWith("[")) {
            return new JSONArray(text);
        }
        JSONArray array = new JSONArray();
        array.put(new JSONObject(text));
        return array;
    }

    private static ArrayList<Conditions.Hour> readHours(JSONObject weather, JSONObject marine) {
        ArrayList<Conditions.Hour> list = new ArrayList<>();
        JSONObject hourly = weather.optJSONObject("hourly");
        if (hourly == null) {
            return list;
        }
        JSONArray times = hourly.optJSONArray("time");

        JSONObject marineHourly = null;
        HashMap<Long, Integer> marineIndex = new HashMap<>();
        if (marine != null) {
            marineHourly = marine.optJSONObject("hourly");
        }
        if (marineHourly != null) {
            JSONArray marineTimes = marineHourly.optJSONArray("time");
            for (int i = 0; marineTimes != null && i < marineTimes.length(); i++) {
                marineIndex.put(marineTimes.optLong(i), i);
            }
        }

        for (int i = 0; times != null && i < times.length(); i++) {
            long seconds = times.optLong(i);
            Conditions.Hour h = new Conditions.Hour();
            h.t = seconds * 1000L;
            h.temp = number(hourly, "temperature_2m", i);
            h.rainProb = number(hourly, "precipitation_probability", i);
            h.rain = number(hourly, "precipitation", i);
            double code = number(hourly, "weather_code", i);
            if (Double.isNaN(code)) {
                h.code = 0;
            } else {
                h.code = (int) code;
            }
            h.cloud = number(hourly, "cloud_cover", i);
            h.pressure = number(hourly, "pressure_msl", i);
            h.wind = number(hourly, "wind_speed_10m", i);
            h.windDir = number(hourly, "wind_direction_10m", i);
            h.gust = number(hourly, "wind_gusts_10m", i);

            Integer m = marineIndex.get(seconds);
            if (m == null) {
                h.wave = Double.NaN;
                h.wavePeriod = Double.NaN;
                h.sst = Double.NaN;
            } else {
                h.wave = number(marineHourly, "wave_height", m);
                h.wavePeriod = number(marineHourly, "wave_period", m);
                h.sst = number(marineHourly, "sea_surface_temperature", m);
            }
            list.add(h);
        }
        return list;
    }

    private static double number(JSONObject hourly, String key, int i) {
        JSONArray values = hourly.optJSONArray(key);
        if (values == null || i >= values.length() || values.isNull(i)) {
            return Double.NaN;
        }
        return values.optDouble(i, Double.NaN);
    }
}
