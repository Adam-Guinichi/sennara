package com.sennara.app.notify;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.data.Session;
import com.sennara.app.data.WeatherRepo;
import com.sennara.app.forecast.Conditions;
import com.sennara.app.ui.SpotDetailActivity;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Geo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AlertWorker extends Worker {
    private static final String NAME = "great-fishing-alerts";
    private static final long HOUR = 3600000L;

    public AlertWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void schedule(Context c) {
        Constraints.Builder constraints = new Constraints.Builder();
        constraints.setRequiredNetworkType(NetworkType.CONNECTED);

        PeriodicWorkRequest.Builder request = new PeriodicWorkRequest.Builder(AlertWorker.class, 6, TimeUnit.HOURS);
        request.setConstraints(constraints.build());

        WorkManager.getInstance(c).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request.build());
    }

    public static void runNow(Context c) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(AlertWorker.class).build();
        WorkManager.getInstance(c).enqueue(request);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context c = getApplicationContext();
        if (!Session.loggedIn(c) || !Session.alerts(c)) {
            return Result.success();
        }
        WeatherRepo.download(c);
        Db db = Db.get(c);
        ArrayList<Models.Spot> spots = db.spots();

        HashSet<Integer> favourites = db.favoriteIds(Session.userId(c));
        Models.Spot nearest = null;
        double nearestKm = Double.MAX_VALUE;
        double[] here = Geo.here(c);
        for (Models.Spot s : spots) {
            double km = Geo.km(here[0], here[1], s.lat, s.lng);
            if (km < nearestKm) {
                nearestKm = km;
                nearest = s;
            }
        }

        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        Models.Spot bestSpot = null;
        Conditions.Window bestWindow = null;
        for (Models.Spot s : spots) {
            boolean check;
            if (favourites.isEmpty()) {
                check = nearest != null && s.id == nearest.id;
            } else {
                check = favourites.contains(s.id);
            }
            if (!check) {
                continue;
            }
            int inSeason = 0;
            for (Models.Fish f : db.fishAtSpot(s.id)) {
                if (f.inSeason(month)) {
                    inSeason++;
                }
            }
            ArrayList<Conditions.Result> results = Conditions.evaluate(s, db.weather(s.id), inSeason);
            Conditions.Window w = Conditions.bestWindow(results, System.currentTimeMillis() + HOUR, 36 * HOUR);
            if (w != null && (bestWindow == null || w.score > bestWindow.score)) {
                bestWindow = w;
                bestSpot = s;
            }
        }
        if (bestSpot == null || bestWindow.level < Conditions.GREAT) {
            return Result.success();
        }

        String key = bestSpot.id + "@" + bestWindow.start;
        SharedPreferences prefs = Session.prefs(c);
        if (key.equals(prefs.getString("last_alert", ""))) {
            return Result.success();
        }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_alert", key);
        editor.apply();

        String when = Fmt.weekday(bestWindow.start, Locale.getDefault()) + " "
                + Fmt.hm(bestWindow.start) + "–" + Fmt.hm(bestWindow.end);
        Intent open = new Intent(c, SpotDetailActivity.class);
        open.putExtra("id", bestSpot.id);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String title = c.getString(R.string.notif_alert_title, bestWindow.score);
        String text = c.getString(R.string.notif_alert_text, bestSpot.name, when);
        Notifier.show(c, Notifier.CHANNEL_ALERTS, 3, title, text, open);
        return Result.success();
    }
}
