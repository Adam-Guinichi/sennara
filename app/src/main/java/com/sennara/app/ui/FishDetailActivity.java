package com.sennara.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.chip.ChipGroup;
import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.ui.views.SeasonBarView;
import com.sennara.app.util.Credits;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Geo;
import com.sennara.app.util.Images;
import com.sennara.app.util.Labels;
import com.sennara.app.util.Ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

public class FishDetailActivity extends BaseActivity implements View.OnClickListener {
    private Models.Fish fish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fish = Db.get(this).fish(getIntent().getIntExtra("id", 0));
        if (fish == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_fish_detail);
        edgeToEdge(findViewById(R.id.root), findViewById(R.id.photo_actions), findViewById(R.id.bottom_area), true);
        String lang = lang();

        Images.load((ImageView) findViewById(R.id.photo), fish.image, 1080);
        photoHeader(300, fish.name(lang));
        setText(R.id.name, fish.name(lang));
        setText(R.id.sci, fish.sci + " · " + fish.family);
        setText(R.id.names, getString(R.string.fish_names, fish.nameEn, fish.nameFr, fish.nameAr));
        setText(R.id.about, fish.about(lang));
        setText(R.id.credit, Credits.line(this, fish.image));

        showPills();
        showSize();
        showSeason();
        addChips(R.id.habitats, fish.habitatList(), "habitat");
        addChips(R.id.baits, fish.baitList(), "bait");
        addChips(R.id.techniques, fish.techniqueList(), "technique");
        showWhere();

        findViewById(R.id.back).setOnClickListener(this);
        findViewById(R.id.wiki).setOnClickListener(this);
        findViewById(R.id.log).setOnClickListener(this);
        findViewById(R.id.rules_link).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.back) {
            finish();
        } else if (id == R.id.wiki) {
            openUrl("https://www.wikidata.org/wiki/" + fish.wikidata);
        } else if (id == R.id.log) {
            Intent intent = new Intent(this, AddCatchActivity.class);
            intent.putExtra("fish_id", fish.id);
            startActivity(intent);
        } else if (id == R.id.rules_link) {
            open(RulesActivity.class);
        }
    }

    private void setText(int id, String text) {
        TextView view = findViewById(id);
        view.setText(text);
    }

    private void showPills() {
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        TextView season = findViewById(R.id.season_pill);
        if (fish.inSeason(month)) {
            season.setText(R.string.in_season);
            season.setTextColor(Ui.color(this, R.color.lv_great));
            season.setBackground(Ui.rounded(Ui.color(this, R.color.lv_great_soft), dp(10)));
        } else {
            season.setText(R.string.off_season);
            season.setTextColor(Ui.color(this, R.color.text2));
            season.setBackground(Ui.rounded(Ui.color(this, R.color.surface2), dp(10)));
        }

        TextView iucn = findViewById(R.id.iucn_pill);
        iucn.setText(getString(R.string.iucn_label, Labels.iucn(this, fish.iucn)));
        boolean threatened = fish.iucn.equals("VU") || fish.iucn.equals("EN") || fish.iucn.equals("CR");
        if (threatened) {
            iucn.setTextColor(Ui.color(this, R.color.lv_poor));
            iucn.setBackground(Ui.rounded(Ui.color(this, R.color.lv_poor_soft), dp(10)));
        } else {
            iucn.setTextColor(Ui.color(this, R.color.primary));
            iucn.setBackground(Ui.rounded(Ui.color(this, R.color.primary_soft), dp(10)));
        }
    }

    private void showSize() {
        TextView size = findViewById(R.id.size_value);
        TextView note = findViewById(R.id.size_note);
        if (fish.minWeightG > 0) {
            size.setText(Fmt.weight(fish.minWeightG));
            note.setText(R.string.size_note_tn);
        } else if (fish.minSizeCm > 0) {
            size.setText(getString(R.string.cm, fish.minSizeCm));
            if (fish.sizeRule.equals("tn_rule")) {
                note.setText(R.string.size_note_tn);
            } else {
                note.setText(R.string.size_note_eu);
            }
        } else {
            size.setText("–");
            note.setText(R.string.size_note_none);
        }
        TextView maxSize = findViewById(R.id.max_size);
        if (fish.maxCm > 0) {
            maxSize.setText(getString(R.string.max_size, fish.maxCm));
            maxSize.setVisibility(View.VISIBLE);
        } else {
            maxSize.setVisibility(View.GONE);
        }
    }

    private void showSeason() {
        String[] letters = new String[12];
        for (int month = 1; month <= 12; month++) {
            letters[month - 1] = Fmt.month(month, locale()).substring(0, 1).toUpperCase(locale());
        }
        SeasonBarView bar = findViewById(R.id.season_bar);
        bar.setSeason(fish.monthFrom, fish.monthTo, letters);
        setText(R.id.season_text, getString(R.string.season_range, Fmt.month(fish.monthFrom, locale()), Fmt.month(fish.monthTo, locale())));
    }

    private void addChips(int groupId, ArrayList<String> codes, String kind) {
        ChipGroup group = findViewById(groupId);
        for (String code : codes) {
            if (kind.equals("habitat")) {
                group.addView(Ui.infoChip(this, Labels.habitat(this, code), R.drawable.ic_place));
            } else if (kind.equals("bait")) {
                group.addView(Ui.infoChip(this, Labels.bait(this, code), R.drawable.ic_hook));
            } else {
                group.addView(Ui.infoChip(this, Labels.technique(this, code), R.drawable.ic_lightbulb));
            }
        }
    }

    private void showWhere() {
        LinearLayout box = findViewById(R.id.where);
        final double[] here = Geo.here(this);
        ArrayList<Models.Spot> spots = Db.get(this).spotsWithFish(fish.id);
        Collections.sort(spots, new Comparator<Models.Spot>() {
            @Override
            public int compare(Models.Spot a, Models.Spot b) {
                return Double.compare(Geo.km(here[0], here[1], a.lat, a.lng), Geo.km(here[0], here[1], b.lat, b.lng));
            }
        });
        for (final Models.Spot spot : spots) {
            View row = getLayoutInflater().inflate(R.layout.item_sky, box, false);
            ImageView icon = row.findViewById(R.id.sky_icon);
            icon.setImageResource(Labels.typeIcon(spot.type));
            TextView label = row.findViewById(R.id.sky_label);
            label.setText(spot.name(lang()));
            TextView distance = row.findViewById(R.id.sky_time);
            distance.setText(Geo.kmText(Geo.km(here[0], here[1], spot.lat, spot.lng)));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(FishDetailActivity.this, SpotDetailActivity.class);
                    intent.putExtra("id", spot.id);
                    startActivity(intent);
                }
            });
            box.addView(row);
        }
        if (spots.isEmpty()) {
            findViewById(R.id.where_title).setVisibility(View.GONE);
        } else {
            findViewById(R.id.where_title).setVisibility(View.VISIBLE);
        }
    }
}
