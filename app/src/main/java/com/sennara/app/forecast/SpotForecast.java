package com.sennara.app.forecast;

import android.content.Context;

import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.data.WeatherRepo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class SpotForecast {
    private static final long HOUR = 3600000L;
    private static HashMap<Integer, SpotForecast> cache = new HashMap<>();
    private static long cacheKey = 0;

    public Models.Spot spot;
    public ArrayList<Conditions.Result> results;
    public ArrayList<Models.Fish> fish;
    public int inSeason;
    public boolean hasWeather;

    public static SpotForecast of(Context c, Models.Spot spot) {
        long key = WeatherRepo.updatedAt(c) * 31 + System.currentTimeMillis() / HOUR;
        if (key != cacheKey) {
            cache.clear();
            cacheKey = key;
        }
        SpotForecast saved = cache.get(spot.id);
        if (saved != null) {
            return saved;
        }

        Db db = Db.get(c);
        SpotForecast f = new SpotForecast();
        f.spot = spot;
        f.fish = db.fishAtSpot(spot.id);

        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        f.inSeason = 0;
        for (Models.Fish x : f.fish) {
            if (x.inSeason(month)) {
                f.inSeason++;
            }
        }

        ArrayList<Conditions.Hour> hours = db.weather(spot.id);
        f.hasWeather = !hours.isEmpty() && hours.get(hours.size() - 1).t > System.currentTimeMillis();
        if (!f.hasWeather) {
            hours = hoursWithoutWeather();
        }
        f.results = Conditions.evaluate(spot, hours, f.inSeason);

        cache.put(spot.id, f);
        return f;
    }

    public static void clear() {
        cache.clear();
    }

    private static ArrayList<Conditions.Hour> hoursWithoutWeather() {
        ArrayList<Conditions.Hour> list = new ArrayList<>();
        long start = System.currentTimeMillis() / HOUR * HOUR - 3 * HOUR;
        for (int i = 0; i < 51; i++) {
            Conditions.Hour h = new Conditions.Hour();
            h.t = start + i * HOUR;
            h.temp = Double.NaN;
            h.rainProb = Double.NaN;
            h.rain = Double.NaN;
            h.cloud = Double.NaN;
            h.pressure = Double.NaN;
            h.wind = Double.NaN;
            h.windDir = Double.NaN;
            h.gust = Double.NaN;
            h.wave = Double.NaN;
            h.wavePeriod = Double.NaN;
            h.sst = Double.NaN;
            list.add(h);
        }
        return list;
    }

    public Conditions.Result now() {
        return Conditions.now(results);
    }

    public ArrayList<Conditions.Result> upcoming(int max) {
        ArrayList<Conditions.Result> list = new ArrayList<>();
        long from = System.currentTimeMillis() / HOUR * HOUR;
        for (Conditions.Result r : results) {
            if (r.t >= from && list.size() < max) {
                list.add(r);
            }
        }
        return list;
    }

    public Conditions.Window bestWindow(long span) {
        return Conditions.bestWindow(results, System.currentTimeMillis(), span);
    }

    public int bestToday() {
        long now = System.currentTimeMillis();
        long endOfToday = Astro.dayStart(now) + 24 * HOUR;
        int best = -1;
        for (Conditions.Result r : results) {
            if (r.t + HOUR < now || r.t >= endOfToday || r.unsafe) {
                continue;
            }
            if (r.score > best) {
                best = r.score;
            }
        }
        return best;
    }
}
