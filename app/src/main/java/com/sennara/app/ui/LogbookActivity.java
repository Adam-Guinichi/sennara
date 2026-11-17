package com.sennara.app.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.data.Session;
import com.sennara.app.data.SyncManager;
import com.sennara.app.ui.adapters.CardAdapter;
import com.sennara.app.ui.adapters.RowAdapter;
import com.sennara.app.ui.views.MonthChartView;
import com.sennara.app.util.Achievements;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Labels;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class LogbookActivity extends TabActivity implements View.OnClickListener {
    private CardAdapter badges = new CardAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupTab(R.layout.activity_logbook, R.id.nav_log);
        RecyclerView badgeList = findViewById(R.id.badges);
        badgeList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        badgeList.setAdapter(badges);

        findViewById(R.id.add).setOnClickListener(this);
        findViewById(R.id.session_card).setOnClickListener(this);
        findViewById(R.id.sync).setOnClickListener(this);
        ChipGroup sort = findViewById(R.id.sort);
        sort.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, List<Integer> checkedIds) {
                reload();
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.add) {
            open(AddCatchActivity.class);
        } else if (id == R.id.session_card) {
            open(SessionActivity.class);
        } else if (id == R.id.sync) {
            toast(R.string.syncing);
            SyncManager.push(this, new Runnable() {
                @Override
                public void run() {
                    reload();
                    toast(R.string.sync_done);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        ArrayList<Models.CatchItem> catches = Db.get(this).catches(Session.userId(this));
        showStats(catches);
        showSessionCard();
        showMonths(catches);
        showBadges(catches);
        showList(catches);
    }

    private void showStats(ArrayList<Models.CatchItem> catches) {
        int grams = 0;
        int heaviest = 0;
        HashSet<String> species = new HashSet<>();
        Models.CatchItem biggest = null;
        for (Models.CatchItem x : catches) {
            grams = grams + x.weightG;
            species.add(x.species.toLowerCase());
            if (x.weightG > heaviest) {
                heaviest = x.weightG;
                biggest = x;
            }
        }
        TextView total = findViewById(R.id.stat_catches);
        total.setText(String.valueOf(catches.size()));
        TextView kg = findViewById(R.id.stat_kg);
        kg.setText(Fmt.num(grams / 1000.0, 1));
        TextView speciesCount = findViewById(R.id.stat_species);
        speciesCount.setText(String.valueOf(species.size()));
        TextView best = findViewById(R.id.stat_best);
        if (biggest == null) {
            best.setText(R.string.no_best_yet);
        } else {
            best.setText(getString(R.string.best_catch, biggest.species, Fmt.weight(biggest.weightG)));
        }
    }

    private void showSessionCard() {
        SharedPreferences prefs = Session.prefs(this);
        boolean running = prefs.getBoolean("s_running", false) || prefs.getLong("s_begin", 0) > 0;
        TextView title = findViewById(R.id.session_title);
        TextView text = findViewById(R.id.session_text);
        if (running) {
            title.setText(R.string.session_running);
            text.setText(R.string.session_running_text);
        } else {
            title.setText(R.string.qa_session);
            text.setText(R.string.session_card_text);
        }
    }

    private void showMonths(ArrayList<Models.CatchItem> catches) {
        int[] counts = new int[6];
        String[] names = new String[6];
        for (int i = 0; i < 6; i++) {
            Calendar month = Calendar.getInstance();
            month.add(Calendar.MONTH, i - 5);
            names[i] = Fmt.month(month.get(Calendar.MONTH) + 1, locale());
        }
        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH);
        Calendar date = Calendar.getInstance();
        for (Models.CatchItem x : catches) {
            date.setTimeInMillis(x.caughtAt);
            int monthsAgo = (currentYear - date.get(Calendar.YEAR)) * 12 + currentMonth - date.get(Calendar.MONTH);
            if (monthsAgo >= 0 && monthsAgo < 6) {
                counts[5 - monthsAgo]++;
            }
        }
        MonthChartView chart = findViewById(R.id.month_chart);
        chart.setData(counts, names);
    }

    private void showBadges(ArrayList<Models.CatchItem> catches) {
        ArrayList<CardAdapter.Card> cards = new ArrayList<>();
        int unlocked = 0;
        for (final Achievements.Badge badge : Achievements.compute(catches)) {
            CardAdapter.Card card = new CardAdapter.Card(CardAdapter.BADGE);
            card.icon = badge.icon;
            card.title = getString(badge.title);
            card.dimmed = !badge.unlocked;
            if (badge.unlocked) {
                unlocked++;
            }
            card.listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(LogbookActivity.this);
                    dialog.setTitle(badge.title);
                    dialog.setMessage(badge.description);
                    dialog.setPositiveButton(R.string.ok, null);
                    dialog.show();
                }
            };
            cards.add(card);
        }
        badges.setCards(cards);
        TextView count = findViewById(R.id.badges_count);
        count.setText(getString(R.string.badges_count, unlocked, cards.size()));
    }

    private void showList(ArrayList<Models.CatchItem> catches) {
        ChipGroup sortGroup = findViewById(R.id.sort);
        View chip = sortGroup.findViewById(sortGroup.getCheckedChipId());
        String sort = "new";
        if (chip != null) {
            sort = String.valueOf(chip.getTag());
        }
        ArrayList<Models.CatchItem> sorted = new ArrayList<>(catches);
        if (sort.equals("heavy")) {
            Collections.sort(sorted, new Comparator<Models.CatchItem>() {
                @Override
                public int compare(Models.CatchItem a, Models.CatchItem b) {
                    return b.weightG - a.weightG;
                }
            });
        } else if (sort.equals("species")) {
            Collections.sort(sorted, new Comparator<Models.CatchItem>() {
                @Override
                public int compare(Models.CatchItem a, Models.CatchItem b) {
                    return a.species.compareToIgnoreCase(b.species);
                }
            });
        }

        HashMap<Integer, String> fishImages = new HashMap<>();
        for (Models.Fish fish : Db.get(this).fish()) {
            fishImages.put(fish.id, fish.image);
        }

        LinearLayout box = findViewById(R.id.list);
        box.removeAllViews();
        RowAdapter adapter = new RowAdapter(this);
        for (int i = 0; i < sorted.size(); i++) {
            final Models.CatchItem x = sorted.get(i);
            RowAdapter.Row row = new RowAdapter.Row();
            if (!x.photo.isEmpty() && new File(x.photo).exists()) {
                row.image = x.photo;
            } else {
                row.image = fishImages.get(x.fishId);
            }
            row.icon = R.drawable.ic_hook;
            row.title = x.species;
            if (x.weightG > 0) {
                row.title = row.title + " · " + Fmt.weight(x.weightG);
            }
            row.subtitle = Fmt.dateTime(x.caughtAt, locale());
            if (!x.spotName.isEmpty()) {
                row.subtitle = x.spotName + " · " + row.subtitle;
            }
            if (x.released) {
                row.extra = getString(R.string.released);
            } else if (!x.bait.isEmpty()) {
                row.extra = getString(R.string.bait_used, Labels.bait(this, x.bait));
            }
            if (x.synced) {
                row.setBadge(getString(R.string.synced), R.color.lv_great, R.color.lv_great_soft);
            } else {
                row.setBadge(getString(R.string.waiting), R.color.lv_fair, R.color.lv_fair_soft);
            }
            adapter.add(row);

            View view = adapter.getView(i, null, box);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(LogbookActivity.this, AddCatchActivity.class);
                    intent.putExtra("uuid", x.uuid);
                    startActivity(intent);
                }
            });
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    confirmDelete(x.uuid);
                    return true;
                }
            });
            box.addView(view);
        }

        if (sorted.isEmpty()) {
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.empty).setVisibility(View.GONE);
        }
        int pending = Db.get(this).unsyncedCount(Session.userId(this));
        TextView pendingText = findViewById(R.id.pending);
        if (pending > 0) {
            pendingText.setText(getString(R.string.pending_sync, pending));
        } else {
            pendingText.setText(R.string.all_synced);
        }
    }

    private void confirmDelete(final String uuid) {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setMessage(R.string.delete_catch_question);
        dialog.setNegativeButton(R.string.cancel, null);
        dialog.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                Db.get(LogbookActivity.this).deleteCatch(uuid);
                reload();
                SyncManager.push(LogbookActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        reload();
                    }
                });
            }
        });
        dialog.show();
    }

    @Override
    protected void onReselected() {
        findViewById(R.id.scroll).scrollTo(0, 0);
    }
}
