package com.sennara.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.ui.adapters.CardAdapter;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Labels;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class FishActivity extends TabActivity {
    private CardAdapter adapter = new CardAdapter();
    private EditText search;
    private ChipGroup filters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupTab(R.layout.activity_fish, R.id.nav_fish);
        RecyclerView grid = findViewById(R.id.grid);
        grid.setLayoutManager(new GridLayoutManager(this, 2));
        grid.setAdapter(adapter);
        search = findViewById(R.id.search);
        filters = findViewById(R.id.filters);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                reload();
            }
        });
        filters.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, List<Integer> checkedIds) {
                reload();
            }
        });
        findViewById(R.id.rules_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(RulesActivity.class);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        String filter = "season";
        View chip = filters.findViewById(filters.getCheckedChipId());
        if (chip != null) {
            filter = String.valueOf(chip.getTag());
        }
        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        ArrayList<Models.Fish> all = Db.get(this).fish();

        ArrayList<CardAdapter.Card> cards = new ArrayList<>();
        int inSeasonCount = 0;
        for (Models.Fish fish : all) {
            boolean inSeason = fish.inSeason(month);
            if (inSeason) {
                inSeasonCount++;
            }
            boolean match = found(fish.nameEn, q) || found(fish.nameFr, q) || found(fish.nameAr, q)
                    || found(fish.sci, q) || found(fish.family, q);
            if (!match) {
                continue;
            }
            if (filter.equals("season") && !inSeason) {
                continue;
            }
            if (filter.equals("cephalopod") && !fish.isCephalopod()) {
                continue;
            }
            if (filter.startsWith("hab_") && !fish.habitatList().contains(filter.substring(4))) {
                continue;
            }

            CardAdapter.Card card = new CardAdapter.Card(CardAdapter.FISH_GRID);
            card.image = fish.image;
            card.title = fish.name(lang());
            card.subtitle = fish.sci;
            boolean threatened = fish.iucn.equals("VU") || fish.iucn.equals("EN") || fish.iucn.equals("CR");
            if (inSeason) {
                card.pill = getString(R.string.in_season);
                card.pillColor = R.color.lv_great;
                card.pillSoft = R.color.lv_great_soft;
            } else if (threatened) {
                card.pill = Labels.iucn(this, fish.iucn);
                card.pillColor = R.color.lv_poor;
                card.pillSoft = R.color.lv_poor_soft;
            }
            card.intent = new Intent(this, FishDetailActivity.class);
            card.intent.putExtra("id", fish.id);
            cards.add(card);
        }
        adapter.setCards(cards);

        TextView count = findViewById(R.id.count);
        count.setText(getString(R.string.fish_count, all.size(), inSeasonCount, Fmt.month(month, locale())));
        if (cards.isEmpty()) {
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.empty).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onReselected() {
        RecyclerView grid = findViewById(R.id.grid);
        grid.smoothScrollToPosition(0);
    }
}
