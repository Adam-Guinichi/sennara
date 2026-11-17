package com.sennara.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.data.Session;
import com.sennara.app.data.WeatherRepo;
import com.sennara.app.forecast.Conditions;
import com.sennara.app.forecast.SpotForecast;
import com.sennara.app.ui.adapters.RowAdapter;
import com.sennara.app.util.Geo;
import com.sennara.app.util.Labels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class SpotsActivity extends TabActivity implements View.OnClickListener {
    private static final String[] POPULAR = {"Bizerte", "Djerba", "Cap Bon", "Dorade", "Loup", "Poulpe"};
    private RowAdapter adapter;
    private EditText search;
    private ChipGroup filters;
    private ChipGroup suggestions;

    private static class Item {
        Models.Spot spot;
        Conditions.Result now;
        double km;
        String fishNames;
        boolean favourite;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupTab(R.layout.activity_spots, R.id.nav_spots);
        search = findViewById(R.id.search);
        filters = findViewById(R.id.filters);
        suggestions = findViewById(R.id.suggestions);

        ListView list = findViewById(R.id.list);
        adapter = new RowAdapter(this);
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(R.id.empty));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Models.Spot spot = (Models.Spot) adapter.getItem(position).tag;
                if (!query().isEmpty()) {
                    Session.addRecentSearch(SpotsActivity.this, query());
                }
                Intent intent = new Intent(SpotsActivity.this, SpotDetailActivity.class);
                intent.putExtra("id", spot.id);
                startActivity(intent);
            }
        });

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
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    Session.addRecentSearch(SpotsActivity.this, query());
                }
                return false;
            }
        });
        filters.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, List<Integer> checkedIds) {
                reload();
            }
        });

        findViewById(R.id.map_btn).setOnClickListener(this);
        findViewById(R.id.add_spot).setOnClickListener(this);
        findViewById(R.id.empty_action).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.map_btn) {
            open(MapActivity.class);
        } else {
            open(AddSpotActivity.class);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
        Geo.refresh(this, new Runnable() {
            @Override
            public void run() {
                reload();
            }
        });
        WeatherRepo.refresh(this, false, new WeatherRepo.Listener() {
            @Override
            public void onDone(boolean ok) {
                SpotForecast.clear();
                reload();
            }
        });
    }

    private String query() {
        return search.getText().toString().trim().toLowerCase(Locale.ROOT);
    }

    private String filter() {
        View chip = filters.findViewById(filters.getCheckedChipId());
        if (chip == null) {
            return "near";
        }
        return String.valueOf(chip.getTag());
    }

    private void showSuggestions() {
        suggestions.removeAllViews();
        String[] recent = Session.recentSearches(this);
        ArrayList<String> items = new ArrayList<>();
        for (String r : recent) {
            items.add(r);
        }
        for (String p : POPULAR) {
            if (items.size() < 8 && !items.contains(p)) {
                items.add(p);
            }
        }
        for (final String text : items) {
            Chip chip = new Chip(this);
            chip.setText(text);
            chip.setCheckable(false);
            boolean isRecent = false;
            for (String r : recent) {
                if (r.equals(text)) {
                    isRecent = true;
                }
            }
            if (isRecent) {
                chip.setChipIconResource(R.drawable.ic_clock);
            } else {
                chip.setChipIconResource(R.drawable.ic_trend_up);
            }
            chip.setChipIconVisible(true);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    search.setText(text);
                    search.setSelection(text.length());
                }
            });
            suggestions.addView(chip);
        }
    }

    private void reload() {
        String q = query();
        String filter = filter();
        String lang = lang();
        if (q.isEmpty()) {
            findViewById(R.id.suggest_box).setVisibility(View.VISIBLE);
            showSuggestions();
        } else {
            findViewById(R.id.suggest_box).setVisibility(View.GONE);
        }

        Db db = Db.get(this);
        HashSet<Integer> favourites = db.favoriteIds(Session.userId(this));
        double[] here = Geo.here(this);

        ArrayList<Item> items = new ArrayList<>();
        for (Models.Spot s : db.spots()) {
            SpotForecast forecast = SpotForecast.of(this, s);
            String fishNames = "";
            boolean fishMatch = false;
            for (Models.Fish fish : forecast.fish) {
                if (fishNames.length() < 60) {
                    if (!fishNames.isEmpty()) {
                        fishNames = fishNames + ", ";
                    }
                    fishNames = fishNames + fish.name(lang);
                }
                if (found(fish.nameEn, q) || found(fish.nameFr, q) || found(fish.nameAr, q) || found(fish.sci, q)) {
                    fishMatch = true;
                }
            }
            boolean textMatch = found(s.name, q) || found(s.nameAr, q) || found(s.city, q) || found(s.cityAr, q)
                    || found(Labels.type(this, s.type), q) || fishMatch;
            if (q.equals("cap bon") && s.lng > 10.5 && s.lat > 36.4 && s.lat < 37.1) {
                textMatch = true;
            }
            if (!textMatch) {
                continue;
            }
            if (filter.equals("fav") && !favourites.contains(s.id)) {
                continue;
            }
            if (filter.startsWith("type_") && !s.type.equals(filter.substring(5))) {
                continue;
            }
            Item item = new Item();
            item.spot = s;
            item.now = forecast.now();
            item.km = Geo.km(here[0], here[1], s.lat, s.lng);
            item.fishNames = fishNames;
            item.favourite = favourites.contains(s.id);
            items.add(item);
        }

        if (filter.equals("best")) {
            Collections.sort(items, new Comparator<Item>() {
                @Override
                public int compare(Item a, Item b) {
                    return scoreOf(b) - scoreOf(a);
                }
            });
        } else if (filter.equals("rated")) {
            Collections.sort(items, new Comparator<Item>() {
                @Override
                public int compare(Item a, Item b) {
                    return Double.compare(b.spot.avgRating, a.spot.avgRating);
                }
            });
        } else {
            Collections.sort(items, new Comparator<Item>() {
                @Override
                public int compare(Item a, Item b) {
                    return Double.compare(a.km, b.km);
                }
            });
        }

        ArrayList<RowAdapter.Row> rows = new ArrayList<>();
        for (Item item : items) {
            Models.Spot s = item.spot;
            RowAdapter.Row row = new RowAdapter.Row();
            row.image = s.image;
            row.icon = Labels.typeIcon(s.type);
            row.title = s.name(lang);
            row.subtitle = Geo.kmText(item.km) + " · " + Labels.type(this, s.type);
            if (s.nReviews > 0) {
                row.subtitle = row.subtitle + " · ★ " + String.format(Locale.US, "%.1f", s.avgRating);
            }
            row.extra = item.fishNames;
            if (item.now != null) {
                String badge = Labels.verdict(this, item.now.level);
                if (!item.now.unsafe) {
                    badge = badge + " " + item.now.score;
                }
                row.setBadge(badge, Labels.levelColor(item.now.level), Labels.levelSoft(item.now.level));
            }
            if (item.favourite) {
                row.trailing = R.drawable.ic_heart_filled;
                row.trailingColor = R.color.accent;
            }
            row.tag = s;
            rows.add(row);
        }
        adapter.setRows(rows);
        TextView count = findViewById(R.id.count);
        count.setText(getString(R.string.spots_count, rows.size()));
    }

    private static int scoreOf(Item item) {
        if (item.now == null || item.now.unsafe) {
            return -1;
        }
        return item.now.score;
    }

    @Override
    protected void onReselected() {
        ListView list = findViewById(R.id.list);
        list.smoothScrollToPosition(0);
    }
}
