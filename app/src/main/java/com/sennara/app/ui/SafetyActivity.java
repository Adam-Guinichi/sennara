package com.sennara.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.forecast.Conditions;
import com.sennara.app.forecast.SpotForecast;
import com.sennara.app.util.Geo;
import com.sennara.app.util.Labels;
import com.sennara.app.util.Ui;

import java.util.Locale;

public class SafetyActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupSimple(R.layout.activity_safety, getString(R.string.safety_title));
        showNearestVerdict();
        showEmergencyNumbers();

        findViewById(R.id.share_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double[] here = Geo.here(SafetyActivity.this);
                String link = String.format(Locale.US, "https://www.openstreetmap.org/?mlat=%.5f&mlon=%.5f#map=16/%.5f/%.5f",
                        here[0], here[1], here[0], here[1]);
                sms("", getString(R.string.sms_location, link));
            }
        });
    }

    private void showNearestVerdict() {
        double[] here = Geo.here(this);
        Models.Spot nearest = null;
        double nearestKm = Double.MAX_VALUE;
        for (Models.Spot s : Db.get(this).spots()) {
            double km = Geo.km(here[0], here[1], s.lat, s.lng);
            if (km < nearestKm) {
                nearestKm = km;
                nearest = s;
            }
        }
        if (nearest == null) {
            return;
        }
        Conditions.Result now = SpotForecast.of(this, nearest).now();
        if (now == null) {
            return;
        }
        Ui.verdictPill((TextView) findViewById(R.id.pill), now.level);
        TextView verdict = findViewById(R.id.verdict);
        verdict.setText(getString(R.string.safety_now, nearest.name(lang()), Labels.verdictTitle(this, now.level)));
        Ui.reasons((LinearLayout) findViewById(R.id.reasons), now.cons, 5, false);
    }

    private void showEmergencyNumbers() {
        int[] labels = {R.string.em_samu, R.string.em_civil, R.string.em_police, R.string.em_guard, R.string.em_sea};
        String[] numbers = getResources().getStringArray(R.array.emergency_numbers);
        LinearLayout box = findViewById(R.id.numbers);
        for (int i = 0; i < numbers.length; i++) {
            final String number = numbers[i];
            View row = getLayoutInflater().inflate(R.layout.item_sky, box, false);
            ImageView icon = row.findViewById(R.id.sky_icon);
            icon.setImageResource(R.drawable.ic_call);
            TextView label = row.findViewById(R.id.sky_label);
            label.setText(labels[i]);
            TextView numberView = row.findViewById(R.id.sky_time);
            numberView.setText(number);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dial(number);
                }
            });
            box.addView(row);
        }
    }
}
