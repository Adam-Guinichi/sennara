package com.sennara.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.sennara.app.R;
import com.sennara.app.data.Api;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.data.Session;
import com.sennara.app.data.SyncManager;
import com.sennara.app.data.WeatherRepo;
import com.sennara.app.forecast.Conditions;
import com.sennara.app.forecast.SpotForecast;
import com.sennara.app.ui.adapters.CardAdapter;
import com.sennara.app.ui.views.HourlyChartView;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Geo;
import com.sennara.app.util.Images;
import com.sennara.app.util.Labels;
import com.sennara.app.util.Ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class HomeActivity extends TabActivity implements View.OnClickListener {
    private CountDownTimer countdown;
    private Models.Spot nearest;
    private double[] here;
    private CardAdapter fishAdapter = new CardAdapter();
    private CardAdapter spotAdapter = new CardAdapter();
    private CardAdapter learnAdapter = new CardAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupTab(R.layout.activity_home, R.id.nav_home);
        horizontalList(R.id.fish_list, fishAdapter);
        horizontalList(R.id.spot_list, spotAdapter);
        horizontalList(R.id.learn_list, learnAdapter);

        int[] buttons = {R.id.avatar, R.id.hero, R.id.qa_session, R.id.qa_catch, R.id.qa_safety, R.id.fish_all,
                R.id.spot_all, R.id.learn_all, R.id.shop_all, R.id.sync_btn, R.id.window_remind};
        for (int id : buttons) {
            findViewById(id).setOnClickListener(this);
        }
        Ui.rise(findViewById(R.id.hero), 60);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.avatar) {
            open(ProfileActivity.class);
        } else if (id == R.id.hero) {
            openSpot(nearest);
        } else if (id == R.id.qa_session) {
            open(SessionActivity.class);
        } else if (id == R.id.qa_catch) {
            open(AddCatchActivity.class);
        } else if (id == R.id.qa_safety) {
            open(SafetyActivity.class);
        } else if (id == R.id.fish_all) {
            nav.setSelectedItemId(R.id.nav_fish);
        } else if (id == R.id.spot_all) {
            nav.setSelectedItemId(R.id.nav_spots);
        } else if (id == R.id.learn_all) {
            open(TutorialsActivity.class);
        } else if (id == R.id.shop_all) {
            nav.setSelectedItemId(R.id.nav_shops);
        } else if (id == R.id.sync_btn) {
            syncNow();
        } else if (id == R.id.window_remind && nearest != null) {
            Intent intent = new Intent(this, SpotDetailActivity.class);
            intent.putExtra("id", nearest.id);
            intent.putExtra("remind", true);
            startActivity(intent);
        }
    }

    private void horizontalList(int id, CardAdapter adapter) {
        RecyclerView list = findViewById(id);
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        list.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String name = Session.name(this);
        String firstName = name;
        if (name.contains(" ")) {
            firstName = name.substring(0, name.indexOf(' '));
        }
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int greeting;
        if (hour < 12) {
            greeting = R.string.good_morning;
        } else if (hour < 18) {
            greeting = R.string.good_afternoon;
        } else {
            greeting = R.string.good_evening;
        }
        TextView hello = findViewById(R.id.hello);
        hello.setText(getString(greeting, firstName));
        TextView today = findViewById(R.id.today);
        today.setText(Fmt.longDay(System.currentTimeMillis(), locale()));
        TextView avatar = findViewById(R.id.avatar);
        avatar.setText(Ui.initials(name));

        render();
        Geo.refresh(this, new Runnable() {
            @Override
            public void run() {
                render();
            }
        });
        WeatherRepo.refresh(this, false, new WeatherRepo.Listener() {
            @Override
            public void onDone(boolean ok) {
                SpotForecast.clear();
                render();
            }
        });
    }

    @Override
    protected void onPause() {
        if (countdown != null) {
            countdown.cancel();
        }
        super.onPause();
    }

    @Override
    protected void onReselected() {
        findViewById(R.id.scroll).scrollTo(0, 0);
    }

    private void openSpot(Models.Spot spot) {
        if (spot == null) {
            return;
        }
        Intent intent = new Intent(this, SpotDetailActivity.class);
        intent.putExtra("id", spot.id);
        startActivity(intent);
    }

    private double distance(double lat, double lng) {
        return Geo.km(here[0], here[1], lat, lng);
    }

    private void render() {
        ArrayList<Models.Spot> spots = Db.get(this).spots();
        here = Geo.here(this);

        nearest = null;
        double nearestKm = Double.MAX_VALUE;
        for (Models.Spot s : spots) {
            double km = distance(s.lat, s.lng);
            if (km < nearestKm) {
                nearestKm = km;
                nearest = s;
            }
        }
        if (nearest == null) {
            findViewById(R.id.empty_box).setVisibility(View.VISIBLE);
            findViewById(R.id.hero).setVisibility(View.GONE);
            return;
        }
        findViewById(R.id.empty_box).setVisibility(View.GONE);
        findViewById(R.id.hero).setVisibility(View.VISIBLE);

        SpotForecast forecast = SpotForecast.of(this, nearest);
        showVerdict(forecast, nearestKm);
        showBestWindow(forecast);
        showHours(forecast);
        showFishNearby(spots);
        showBestSpots(spots);
        showTutorials();
        showShops();
        showSyncState();
    }

    private void showVerdict(SpotForecast forecast, double km) {
        Images.load((ImageView) findViewById(R.id.hero_photo), nearest.image, 900);
        TextView spotName = findViewById(R.id.hero_spot);
        spotName.setText(getString(R.string.near_you, nearest.name(lang()), Geo.kmText(km)));

        Conditions.Result now = forecast.now();
        if (now == null) {
            return;
        }
        Ui.verdictPillSolid((TextView) findViewById(R.id.hero_pill), now.level);
        TextView title = findViewById(R.id.hero_title);
        title.setText(Labels.verdictTitle(this, now.level));

        TextView score = findViewById(R.id.hero_score);
        CircularProgressIndicator ring = findViewById(R.id.hero_ring);
        if (now.unsafe) {
            score.setText("!");
            ring.setIndicatorColor(Ui.color(this, R.color.lv_unsafe));
            ring.setProgress(100, true);
        } else {
            score.setText(String.valueOf(now.score));
            ring.setIndicatorColor(Ui.color(this, R.color.accent));
            ring.setProgress(now.score, true);
        }

        ArrayList<Conditions.Reason> reasons = new ArrayList<>();
        if (now.unsafe) {
            reasons.addAll(now.cons);
        } else {
            reasons.addAll(now.pros);
            reasons.addAll(now.cons);
        }
        Ui.reasons((LinearLayout) findViewById(R.id.hero_reasons), reasons, 3, true);

        TextView weather = findViewById(R.id.hero_weather);
        if (forecast.hasWeather) {
            Conditions.Hour h = now.hour;
            weather.setText(getString(R.string.weather_line, Fmt.num(h.wind, 0), Conditions.compass(h.windDir),
                    Fmt.num(h.wave, 1), Fmt.num(h.temp, 0)));
        } else {
            weather.setText(R.string.weather_offline);
        }
    }

    private void showBestWindow(SpotForecast forecast) {
        if (countdown != null) {
            countdown.cancel();
        }
        View card = findViewById(R.id.window_card);
        Conditions.Window w = forecast.bestWindow(24 * 3600000L);
        if (w == null) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        TextView time = findViewById(R.id.window_time);
        time.setText(getString(R.string.best_window_line, Fmt.weekday(w.start, locale()), Fmt.hm(w.start), Fmt.hm(w.end)));
        Ui.scorePill((TextView) findViewById(R.id.window_score), w.score, w.level);

        TextView label = findViewById(R.id.window_label);
        final TextView clock = findViewById(R.id.window_clock);
        long now = System.currentTimeMillis();
        long target;
        if (now >= w.start) {
            label.setText(R.string.window_now);
            target = w.end;
        } else {
            label.setText(R.string.window_starts_in);
            target = w.start;
        }
        long left = Math.max(1000, target - now);
        countdown = new CountDownTimer(left, 1000) {
            @Override
            public void onTick(long millisLeft) {
                clock.setText(Fmt.hms(millisLeft));
            }

            @Override
            public void onFinish() {
                render();
            }
        };
        countdown.start();
    }

    private void showHours(final SpotForecast forecast) {
        HourlyChartView chart = findViewById(R.id.hours_chart);
        final ArrayList<Conditions.Result> next = forecast.upcoming(24);
        chart.setData(next, 24);
        chart.setOnHourSelected(new HourlyChartView.OnHourSelected() {
            @Override
            public void onHour(int index) {
                showHour(next.get(index), forecast.hasWeather);
            }
        });
        if (!next.isEmpty()) {
            showHour(next.get(0), forecast.hasWeather);
        }
    }

    private void showHour(Conditions.Result r, boolean hasWeather) {
        TextView title = findViewById(R.id.hour_title);
        title.setText(getString(R.string.hour_title, Fmt.hm(r.t), Labels.verdict(this, r.level), r.score));
        TextView detail = findViewById(R.id.hour_detail);
        if (hasWeather) {
            Conditions.Hour h = r.hour;
            detail.setText(getString(R.string.hour_detail, Fmt.num(h.wind, 0), Conditions.compass(h.windDir),
                    Fmt.num(h.gust, 0), Fmt.num(h.wave, 1), Fmt.num(h.rainProb, 0), Fmt.num(h.temp, 0)));
        } else {
            detail.setText(R.string.weather_offline);
        }
    }

    private void showFishNearby(ArrayList<Models.Spot> spots) {
        Db db = Db.get(this);
        HashMap<Integer, ArrayList<Integer>> spotFish = db.spotFishMap();

        ArrayList<Models.Spot> near = new ArrayList<>(spots);
        Collections.sort(near, new Comparator<Models.Spot>() {
            @Override
            public int compare(Models.Spot a, Models.Spot b) {
                return Double.compare(distance(a.lat, a.lng), distance(b.lat, b.lng));
            }
        });

        final HashMap<Integer, Integer> count = new HashMap<>();
        for (int i = 0; i < near.size(); i++) {
            Models.Spot s = near.get(i);
            if (i >= 4 && distance(s.lat, s.lng) > 60) {
                break;
            }
            ArrayList<Integer> fishIds = spotFish.get(s.id);
            if (fishIds == null) {
                continue;
            }
            for (int fishId : fishIds) {
                if (count.containsKey(fishId)) {
                    count.put(fishId, count.get(fishId) + 1);
                } else {
                    count.put(fishId, 1);
                }
            }
        }

        int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        ArrayList<Models.Fish> list = new ArrayList<>();
        for (Models.Fish fish : db.fish()) {
            if (count.containsKey(fish.id) && fish.inSeason(month)) {
                list.add(fish);
            }
        }
        Collections.sort(list, new Comparator<Models.Fish>() {
            @Override
            public int compare(Models.Fish a, Models.Fish b) {
                return count.get(b.id) - count.get(a.id);
            }
        });

        ArrayList<CardAdapter.Card> cards = new ArrayList<>();
        for (Models.Fish fish : list) {
            CardAdapter.Card card = new CardAdapter.Card(CardAdapter.FISH);
            card.image = fish.image;
            card.title = fish.name(lang());
            card.subtitle = fish.sci;
            card.pill = getString(R.string.in_season);
            card.pillColor = R.color.lv_great;
            card.pillSoft = R.color.lv_great_soft;
            card.intent = new Intent(this, FishDetailActivity.class);
            card.intent.putExtra("id", fish.id);
            cards.add(card);
        }
        fishAdapter.setCards(cards);
        TextView caption = findViewById(R.id.fish_caption);
        caption.setText(getString(R.string.fish_near_caption, list.size(), Fmt.month(month, locale())));
    }

    private static class SpotScore {
        Models.Spot spot;
        int best;
        double km;
    }

    private void showBestSpots(ArrayList<Models.Spot> spots) {
        ArrayList<SpotScore> scores = new ArrayList<>();
        for (Models.Spot s : spots) {
            SpotScore item = new SpotScore();
            item.spot = s;
            item.best = SpotForecast.of(this, s).bestToday();
            item.km = distance(s.lat, s.lng);
            scores.add(item);
        }
        Collections.sort(scores, new Comparator<SpotScore>() {
            @Override
            public int compare(SpotScore a, SpotScore b) {
                if (a.best != b.best) {
                    return b.best - a.best;
                }
                return Double.compare(a.km, b.km);
            }
        });

        ArrayList<CardAdapter.Card> cards = new ArrayList<>();
        for (int i = 0; i < scores.size() && i < 8; i++) {
            SpotScore item = scores.get(i);
            int level;
            if (item.best < 0) {
                level = Conditions.UNSAFE;
            } else {
                level = Conditions.level(item.best);
            }
            CardAdapter.Card card = new CardAdapter.Card(CardAdapter.SPOT);
            card.image = item.spot.image;
            card.title = item.spot.name(lang());
            card.subtitle = getString(R.string.spot_card_sub, Geo.kmText(item.km), Labels.type(this, item.spot.type));
            if (item.best < 0) {
                card.pill = Labels.verdict(this, level);
            } else {
                card.pill = getString(R.string.best_today_pill, item.best);
            }
            card.pillColor = Labels.levelColor(level);
            card.pillSoft = Labels.levelSoft(level);
            card.intent = new Intent(this, SpotDetailActivity.class);
            card.intent.putExtra("id", item.spot.id);
            cards.add(card);
        }
        spotAdapter.setCards(cards);
    }

    private void showTutorials() {
        ArrayList<Models.Tutorial> list = Db.get(this).tutorials();
        if (!Session.level(this).equals("beginner")) {
            Collections.sort(list, new Comparator<Models.Tutorial>() {
                @Override
                public int compare(Models.Tutorial a, Models.Tutorial b) {
                    return Boolean.compare(a.level.equals("beginner"), b.level.equals("beginner"));
                }
            });
        }
        ArrayList<CardAdapter.Card> cards = new ArrayList<>();
        for (Models.Tutorial t : list) {
            CardAdapter.Card card = new CardAdapter.Card(CardAdapter.TUTORIAL);
            card.image = t.image;
            card.title = t.title(lang());
            card.subtitle = getString(R.string.minutes_read, t.minutes);
            card.pill = Labels.level(this, t.level);
            card.intent = new Intent(this, TutorialDetailActivity.class);
            card.intent.putExtra("id", t.id);
            cards.add(card);
        }
        learnAdapter.setCards(cards);
    }

    private void showShops() {
        LinearLayout box = findViewById(R.id.shops_box);
        box.removeAllViews();
        ArrayList<Models.Shop> shops = Db.get(this).shops();
        Collections.sort(shops, new Comparator<Models.Shop>() {
            @Override
            public int compare(Models.Shop a, Models.Shop b) {
                return Double.compare(distance(a.lat, a.lng), distance(b.lat, b.lng));
            }
        });
        for (int i = 0; i < shops.size() && i < 2; i++) {
            final Models.Shop shop = shops.get(i);
            View row = getLayoutInflater().inflate(R.layout.item_row, box, false);
            row.findViewById(R.id.row_photo).setVisibility(View.GONE);
            ImageView icon = row.findViewById(R.id.row_icon);
            icon.setImageResource(R.drawable.ic_store);
            TextView title = row.findViewById(R.id.row_title);
            title.setText(shop.name);
            TextView subtitle = row.findViewById(R.id.row_subtitle);
            subtitle.setText(getString(R.string.shop_sub, shop.city, Geo.kmText(distance(shop.lat, shop.lng))));
            row.findViewById(R.id.row_extra).setVisibility(View.GONE);
            row.findViewById(R.id.row_badge).setVisibility(View.GONE);
            ImageView trailing = row.findViewById(R.id.row_trailing);
            trailing.setImageResource(R.drawable.ic_chevron);
            Ui.tint(trailing, R.color.text3);
            row.setPaddingRelative(0, row.getPaddingTop(), 0, row.getPaddingBottom());
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(HomeActivity.this, ShopDetailActivity.class);
                    intent.putExtra("id", shop.id);
                    startActivity(intent);
                }
            });
            box.addView(row);
        }
    }

    private void showSyncState() {
        long last = Session.lastSync(this);
        String text;
        if (last == 0) {
            text = getString(R.string.never_synced);
        } else {
            text = getString(R.string.last_sync, Fmt.dateTime(last, locale()));
        }
        int pending = Db.get(this).unsyncedCount(Session.userId(this));
        if (pending > 0) {
            text = text + " · " + getString(R.string.pending_sync, pending);
        }
        TextView syncText = findViewById(R.id.sync_text);
        syncText.setText(text);
    }

    private void syncNow() {
        final View spinner = findViewById(R.id.sync_spin);
        final View button = findViewById(R.id.sync_btn);
        spinner.setVisibility(View.VISIBLE);
        button.setEnabled(false);
        SyncManager.sync(this, new SyncManager.Listener() {
            @Override
            public void onProgress(int percent, String status) {
            }

            @Override
            public void onDone(final boolean ok, final String error) {
                WeatherRepo.refresh(HomeActivity.this, true, new WeatherRepo.Listener() {
                    @Override
                    public void onDone(boolean weatherOk) {
                        spinner.setVisibility(View.GONE);
                        button.setEnabled(true);
                        if (ok) {
                            toast(R.string.sync_done);
                        } else {
                            toast(Api.message(HomeActivity.this, error));
                        }
                        SpotForecast.clear();
                        render();
                    }
                });
            }
        });
    }
}
