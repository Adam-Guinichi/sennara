package com.sennara.app.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.data.Session;
import com.sennara.app.forecast.Conditions;
import com.sennara.app.forecast.SpotForecast;
import com.sennara.app.notify.Notifier;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Geo;
import com.sennara.app.util.Images;
import com.sennara.app.util.Labels;
import com.sennara.app.util.Ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SessionActivity extends BaseActivity implements View.OnClickListener {
    private int[] goalChips = {R.id.g30, R.id.g60, R.id.g120, R.id.g180};
    private int[] goalMinutes = {30, 60, 120, 180};

    private SharedPreferences prefs;
    private Timer timer;
    private ArrayList<Models.Spot> spots = new ArrayList<>();
    private TextView clock;
    private TextView percent;
    private TextView count;
    private CircularProgressIndicator ring;
    private Button toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupSimple(R.layout.activity_session, getString(R.string.qa_session));
        prefs = Session.prefs(this);
        clock = findViewById(R.id.timer);
        percent = findViewById(R.id.percent);
        count = findViewById(R.id.count);
        ring = findViewById(R.id.ring);
        toggle = findViewById(R.id.btn_toggle);

        setupSpotSpinner();
        setupGoalChips();
        toggle.setOnClickListener(this);
        findViewById(R.id.btn_catch).setOnClickListener(this);
        findViewById(R.id.btn_finish).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_toggle) {
            if (isRunning()) {
                pause();
            } else {
                start();
            }
        } else if (id == R.id.btn_catch) {
            Intent intent = new Intent(this, AddCatchActivity.class);
            intent.putExtra("spot_id", prefs.getInt("s_spot", 0));
            startActivity(intent);
        } else if (id == R.id.btn_finish) {
            finishSession();
        }
    }

    private void setupSpotSpinner() {
        final double[] here = Geo.here(this);
        spots.addAll(Db.get(this).spots());
        Collections.sort(spots, new Comparator<Models.Spot>() {
            @Override
            public int compare(Models.Spot a, Models.Spot b) {
                return Double.compare(Geo.km(here[0], here[1], a.lat, a.lng), Geo.km(here[0], here[1], b.lat, b.lng));
            }
        });
        ArrayList<String> names = new ArrayList<>();
        int wanted = getIntent().getIntExtra("spot_id", prefs.getInt("s_spot", 0));
        int position = 0;
        for (int i = 0; i < spots.size(); i++) {
            names.add(spots.get(i).name(lang()));
            if (spots.get(i).id == wanted) {
                position = i;
            }
        }
        Spinner spinner = findViewById(R.id.spot);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (!spots.isEmpty()) {
            spinner.setSelection(position);
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int i, long id) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("s_spot", spots.get(i).id);
                editor.apply();
                showSpot(spots.get(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupGoalChips() {
        ChipGroup goals = findViewById(R.id.goal_chips);
        int goal = prefs.getInt("s_goal", 60);
        for (int i = 0; i < goalChips.length; i++) {
            if (goalMinutes[i] == goal) {
                goals.check(goalChips[i]);
            }
        }
        goals.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, List<Integer> checkedIds) {
                for (int i = 0; i < goalChips.length; i++) {
                    if (group.getCheckedChipId() == goalChips[i]) {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putInt("s_goal", goalMinutes[i]);
                        editor.apply();
                    }
                }
                if (isRunning()) {
                    scheduleGoalAlert();
                }
                tick();
            }
        });
    }

    private void showSpot(Models.Spot spot) {
        Images.load((ImageView) findViewById(R.id.spot_photo), spot.image, 900);
        Conditions.Result now = SpotForecast.of(this, spot).now();
        if (now != null) {
            Ui.verdictPillSolid((TextView) findViewById(R.id.spot_pill), now.level);
            TextView verdict = findViewById(R.id.spot_verdict);
            verdict.setText(Labels.verdictTitle(this, now.level));
        }
    }

    private boolean isRunning() {
        return prefs.getBoolean("s_running", false);
    }

    private long elapsed() {
        long total = prefs.getLong("s_acc", 0);
        if (isRunning()) {
            total = total + System.currentTimeMillis() - prefs.getLong("s_start", 0);
        }
        return total;
    }

    private long goalMs() {
        return prefs.getInt("s_goal", 60) * 60000L;
    }

    private void start() {
        long now = System.currentTimeMillis();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("s_running", true);
        editor.putLong("s_start", now);
        if (prefs.getLong("s_begin", 0) == 0) {
            editor.putLong("s_begin", now);
        }
        editor.apply();
        if (!Notifier.allowed(this)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
        }
        scheduleGoalAlert();
        tick();
    }

    private void pause() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("s_acc", elapsed());
        editor.putBoolean("s_running", false);
        editor.apply();
        Notifier.cancelGoal(this);
        tick();
    }

    private void scheduleGoalAlert() {
        long left = goalMs() - elapsed();
        if (left > 0) {
            Notifier.scheduleGoal(this, System.currentTimeMillis() + left);
        } else {
            Notifier.cancelGoal(this);
        }
    }

    private void finishSession() {
        long begin = prefs.getLong("s_begin", 0);
        if (begin == 0) {
            toast(R.string.session_not_started);
            return;
        }
        long total = elapsed();
        int catches = 0;
        int grams = 0;
        int released = 0;
        for (Models.CatchItem c : Db.get(this).catches(Session.userId(this))) {
            if (c.caughtAt < begin) {
                continue;
            }
            catches++;
            grams = grams + c.weightG;
            if (c.released) {
                released++;
            }
        }
        Notifier.cancelGoal(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("s_running");
        editor.remove("s_start");
        editor.remove("s_acc");
        editor.remove("s_begin");
        editor.apply();
        tick();

        View card = getLayoutInflater().inflate(R.layout.dialog_session_summary, null);
        TextView time = card.findViewById(R.id.sum_time);
        time.setText(Fmt.hms(total));
        TextView catchCount = card.findViewById(R.id.sum_catches);
        catchCount.setText(String.valueOf(catches));
        TextView kg = card.findViewById(R.id.sum_kg);
        kg.setText(Fmt.num(grams / 1000.0, 1));
        TextView message = card.findViewById(R.id.sum_message);
        if (catches == 0) {
            message.setText(R.string.session_zero);
        } else if (released > 0) {
            message.setText(getString(R.string.session_released, released));
        } else {
            message.setText(R.string.session_good);
        }
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setView(card);
        dialog.setPositiveButton(R.string.done, null);
        dialog.show();
    }

    private void tick() {
        long elapsed = elapsed();
        int pct = (int) (elapsed * 100 / goalMs());
        if (pct > 100) {
            pct = 100;
        }
        clock.setText(Fmt.hms(elapsed));
        ring.setProgress(pct);
        percent.setText(getString(R.string.percent_of_goal, pct));
        if (isRunning()) {
            toggle.setText(R.string.pause);
        } else if (elapsed > 0) {
            toggle.setText(R.string.resume);
        } else {
            toggle.setText(R.string.start);
        }
        long begin = prefs.getLong("s_begin", 0);
        int n = 0;
        if (begin > 0) {
            n = Db.get(this).catchCountSince(Session.userId(this), begin);
        }
        count.setText(getString(R.string.session_catches, n));
    }

    @Override
    protected void onResume() {
        super.onResume();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tick();
                    }
                });
            }
        }, 0, 1000);
    }

    @Override
    protected void onPause() {
        timer.cancel();
        super.onPause();
    }
}
