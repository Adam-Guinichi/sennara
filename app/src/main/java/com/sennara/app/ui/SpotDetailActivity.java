package com.sennara.app.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sennara.app.R;
import com.sennara.app.data.Api;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.data.Session;
import com.sennara.app.data.SyncManager;
import com.sennara.app.data.WeatherRepo;
import com.sennara.app.forecast.Astro;
import com.sennara.app.forecast.Conditions;
import com.sennara.app.forecast.SpotForecast;
import com.sennara.app.notify.Notifier;
import com.sennara.app.ui.adapters.CardAdapter;
import com.sennara.app.ui.views.HourlyChartView;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Geo;
import com.sennara.app.util.Images;
import com.sennara.app.util.Labels;
import com.sennara.app.util.Ui;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;

public class SpotDetailActivity extends BaseActivity implements View.OnClickListener {
    private static final long MINUTE = 60000L;
    private static final long HOUR = 3600000L;
    private Models.Spot spot;
    private SpotForecast forecast;
    private CardAdapter fishAdapter = new CardAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int id = getIntent().getIntExtra("id", 0);
        spot = Db.get(this).spot(id);
        if (spot == null) {
            finish();
            return;
        }
        setContentView(R.layout.activity_spot);
        edgeToEdge(findViewById(R.id.root), findViewById(R.id.photo_actions), findViewById(R.id.bottom_area), true);
        String lang = lang();

        Images.load((ImageView) findViewById(R.id.photo), spot.image, 1080);
        photoHeader(320, spot.name(lang));
        TextView name = findViewById(R.id.name);
        name.setText(spot.name(lang));
        TextView sub = findViewById(R.id.sub);
        String km = Geo.kmText(Geo.kmFromHere(this, spot.lat, spot.lng));
        sub.setText(getString(R.string.spot_sub, spot.city(lang), Labels.type(this, spot.type), km));
        TextView desc = findViewById(R.id.desc);
        desc.setText(spot.desc(lang));
        TextView bestTime = findViewById(R.id.best_time);
        bestTime.setText(getString(R.string.best_time_line, Labels.bestTime(this, spot.bestTime)));

        RecyclerView fishList = findViewById(R.id.fish_list);
        fishList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        fishList.setAdapter(fishAdapter);

        int[] buttons = {R.id.back, R.id.share, R.id.fav, R.id.directions, R.id.session, R.id.remind, R.id.map_link, R.id.rate};
        for (int button : buttons) {
            findViewById(button).setOnClickListener(this);
        }
        updateFavorite();
        loadReviews();
        if (getIntent().getBooleanExtra("remind", false)) {
            remind();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.back) {
            finish();
        } else if (id == R.id.share) {
            String link = "https://www.openstreetmap.org/?mlat=" + spot.lat + "&mlon=" + spot.lng
                    + "#map=15/" + spot.lat + "/" + spot.lng;
            shareText(getString(R.string.share_spot, spot.name(lang()), link));
        } else if (id == R.id.fav) {
            toggleFavorite();
        } else if (id == R.id.directions) {
            openDirections(spot.lat, spot.lng, spot.name);
        } else if (id == R.id.session) {
            Intent intent = new Intent(this, SessionActivity.class);
            intent.putExtra("spot_id", spot.id);
            startActivity(intent);
        } else if (id == R.id.remind) {
            remind();
        } else if (id == R.id.map_link) {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("lat", spot.lat);
            intent.putExtra("lng", spot.lng);
            startActivity(intent);
        } else if (id == R.id.rate) {
            showRateDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
        WeatherRepo.refresh(this, false, new WeatherRepo.Listener() {
            @Override
            public void onDone(boolean ok) {
                SpotForecast.clear();
                render();
            }
        });
    }

    private void render() {
        forecast = SpotForecast.of(this, spot);
        showVerdict();
        showHours();
        showOutlook();
        showSky();
        showFish();
        showShops();
    }

    private void showVerdict() {
        Conditions.Result now = forecast.now();
        if (now == null) {
            return;
        }
        int color = Ui.color(this, Labels.levelColor(now.level));
        MaterialCardView card = findViewById(R.id.verdict_card);
        card.setCardBackgroundColor(Ui.color(this, Labels.levelSoft(now.level)));
        Ui.verdictPill((TextView) findViewById(R.id.verdict_pill), now.level);
        TextView title = findViewById(R.id.verdict_title);
        title.setText(Labels.verdictTitle(this, now.level));
        title.setTextColor(color);
        TextView score = findViewById(R.id.verdict_score);
        if (now.unsafe) {
            score.setText("!");
        } else {
            score.setText(String.valueOf(now.score));
        }
        score.setTextColor(color);

        ArrayList<Conditions.Reason> reasons = new ArrayList<>();
        if (now.unsafe) {
            reasons.addAll(now.cons);
            reasons.addAll(now.pros);
        } else {
            reasons.addAll(now.pros);
            reasons.addAll(now.cons);
        }
        Ui.reasons((LinearLayout) findViewById(R.id.verdict_reasons), reasons, 6, false);

        TextView source = findViewById(R.id.verdict_source);
        if (forecast.hasWeather) {
            source.setText(getString(R.string.forecast_source, Fmt.hm(WeatherRepo.updatedAt(this))));
        } else {
            source.setText(R.string.weather_offline);
        }
        TextView best = findViewById(R.id.verdict_best);
        Conditions.Window w = forecast.bestWindow(24 * HOUR);
        if (w == null) {
            best.setText(R.string.no_safe_window);
        } else {
            best.setText(getString(R.string.best_window_full, Fmt.weekday(w.start, locale()), Fmt.hm(w.start), Fmt.hm(w.end), w.score));
        }
    }

    private void showHours() {
        HourlyChartView chart = findViewById(R.id.chart);
        final ArrayList<Conditions.Result> next = forecast.upcoming(30);
        chart.setData(next, 30);
        chart.setOnHourSelected(new HourlyChartView.OnHourSelected() {
            @Override
            public void onHour(int index) {
                showHour(next.get(index));
            }
        });
        if (!next.isEmpty()) {
            showHour(next.get(0));
        }
    }

    private void showHour(Conditions.Result r) {
        Conditions.Hour h = r.hour;
        TextView head = findViewById(R.id.hour_head);
        String time = Fmt.weekday(r.t, locale()) + " " + Fmt.hm(r.t);
        head.setText(getString(R.string.hour_title, time, Labels.verdict(this, r.level), r.score));

        tile(R.id.t_wind, Fmt.num(h.wind, 0) + " km/h", getString(R.string.c_wind) + " " + Conditions.compass(h.windDir));
        ImageView arrow = findViewById(R.id.t_wind_arrow);
        if (Double.isNaN(h.windDir)) {
            arrow.setRotation(0);
        } else {
            arrow.setRotation((float) (h.windDir + 180));
        }
        tile(R.id.t_gust, Fmt.num(h.gust, 0) + " km/h", getString(R.string.c_gusts));
        String waveLabel = getString(R.string.c_waves);
        if (!Double.isNaN(h.wavePeriod)) {
            waveLabel = waveLabel + " · " + Fmt.num(h.wavePeriod, 0) + " s";
        }
        tile(R.id.t_wave, Fmt.num(h.wave, 1) + " m", waveLabel);
        tile(R.id.t_sea, Fmt.num(h.sst, 0) + " °C", getString(R.string.c_sea_temp));
        tile(R.id.t_rain, Fmt.num(h.rainProb, 0) + " %", getString(R.string.c_rain));
        tile(R.id.t_pressure, Fmt.num(h.pressure, 0) + " hPa", getString(R.string.c_pressure));
    }

    private void tile(int tileId, String value, String label) {
        View tile = findViewById(tileId);
        TextView valueView = tile.findViewById(R.id.tile_value);
        valueView.setText(value);
        TextView labelView = tile.findViewById(R.id.tile_label);
        labelView.setText(label);
    }

    private void showOutlook() {
        LinearLayout box = findViewById(R.id.outlook);
        box.removeAllViews();
        ArrayList<Conditions.DaySummary> days = Conditions.days(forecast.results);
        for (Conditions.DaySummary d : days) {
            if (d.day + 24 * HOUR < System.currentTimeMillis()) {
                continue;
            }
            if (box.getChildCount() >= 3) {
                break;
            }
            View card = getLayoutInflater().inflate(R.layout.item_day, box, false);
            TextView dayName = card.findViewById(R.id.day_name);
            if (box.getChildCount() == 0) {
                dayName.setText(R.string.today);
            } else {
                dayName.setText(Fmt.weekday(d.day + HOUR, locale()));
            }
            Ui.verdictPill((TextView) card.findViewById(R.id.day_pill), d.level);
            TextView best = card.findViewById(R.id.day_best);
            if (d.best < 0) {
                best.setText(R.string.stay_ashore_short);
            } else {
                best.setText(getString(R.string.day_best, d.best, Fmt.hm(d.bestTime)));
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            if (box.getChildCount() > 0) {
                params.setMarginStart(dp(8));
            }
            box.addView(card, params);
        }
    }

    private void showSky() {
        Astro.Day day = Astro.day(System.currentTimeMillis(), spot.lat, spot.lng);
        long[] dawn = day.dawnWindow();
        long[] dusk = day.duskWindow();
        LinearLayout box = findViewById(R.id.sky);
        box.removeAllViews();
        addSkyRow(box, R.drawable.ic_sunrise, getString(R.string.dawn_window), Fmt.hm(dawn[0]) + " – " + Fmt.hm(dawn[1]));
        addSkyRow(box, R.drawable.ic_sunset, getString(R.string.dusk_window), Fmt.hm(dusk[0]) + " – " + Fmt.hm(dusk[1]));
        for (long[] period : day.majors) {
            addSkyRow(box, R.drawable.ic_moon, getString(R.string.solunar_major), Fmt.hm(period[0]) + " – " + Fmt.hm(period[1]));
        }
        for (long[] period : day.minors) {
            addSkyRow(box, R.drawable.ic_nightlight, getString(R.string.solunar_minor), Fmt.hm(period[0]) + " – " + Fmt.hm(period[1]));
        }
        TextView sun = findViewById(R.id.sun_line);
        sun.setText(getString(R.string.sun_line, Fmt.hm(day.sunrise), Fmt.hm(day.sunset)));
        TextView moon = findViewById(R.id.moon_line);
        moon.setText(getString(R.string.moon_line, Labels.moon(this, day.phase), Math.round(day.illumination * 100)));
    }

    private void addSkyRow(LinearLayout box, int icon, String label, String time) {
        View row = getLayoutInflater().inflate(R.layout.item_sky, box, false);
        ImageView iconView = row.findViewById(R.id.sky_icon);
        iconView.setImageResource(icon);
        TextView labelView = row.findViewById(R.id.sky_label);
        labelView.setText(label);
        TextView timeView = row.findViewById(R.id.sky_time);
        timeView.setText(time);
        box.addView(row);
    }

    private void showFish() {
        final int month = Calendar.getInstance().get(Calendar.MONTH) + 1;
        ArrayList<Models.Fish> list = new ArrayList<>(forecast.fish);
        Collections.sort(list, new Comparator<Models.Fish>() {
            @Override
            public int compare(Models.Fish a, Models.Fish b) {
                return Boolean.compare(b.inSeason(month), a.inSeason(month));
            }
        });

        ArrayList<CardAdapter.Card> cards = new ArrayList<>();
        final HashMap<String, Integer> baitCount = new HashMap<>();
        for (Models.Fish fish : list) {
            boolean inSeason = fish.inSeason(month);
            if (inSeason) {
                for (String bait : fish.baitList()) {
                    if (baitCount.containsKey(bait)) {
                        baitCount.put(bait, baitCount.get(bait) + 1);
                    } else {
                        baitCount.put(bait, 1);
                    }
                }
            }
            CardAdapter.Card card = new CardAdapter.Card(CardAdapter.FISH);
            card.image = fish.image;
            card.title = fish.name(lang());
            card.subtitle = fish.sci;
            if (inSeason) {
                card.pill = getString(R.string.in_season);
                card.pillColor = R.color.lv_great;
                card.pillSoft = R.color.lv_great_soft;
            } else {
                card.pill = getString(R.string.off_season);
                card.pillColor = R.color.text3;
                card.pillSoft = R.color.surface2;
            }
            card.intent = new Intent(this, FishDetailActivity.class);
            card.intent.putExtra("id", fish.id);
            cards.add(card);
        }
        fishAdapter.setCards(cards);
        TextView caption = findViewById(R.id.fish_caption);
        caption.setText(getString(R.string.fish_here_caption, forecast.inSeason, list.size()));

        ArrayList<String> baits = new ArrayList<>(baitCount.keySet());
        Collections.sort(baits, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return baitCount.get(b) - baitCount.get(a);
            }
        });
        ChipGroup chips = findViewById(R.id.baits);
        chips.removeAllViews();
        for (int i = 0; i < baits.size() && i < 5; i++) {
            chips.addView(Ui.infoChip(this, Labels.bait(this, baits.get(i)), R.drawable.ic_hook));
        }
    }

    private void showShops() {
        LinearLayout box = findViewById(R.id.shops);
        box.removeAllViews();
        ArrayList<Models.Shop> shops = Db.get(this).shops();
        Collections.sort(shops, new Comparator<Models.Shop>() {
            @Override
            public int compare(Models.Shop a, Models.Shop b) {
                return Double.compare(Geo.km(spot.lat, spot.lng, a.lat, a.lng), Geo.km(spot.lat, spot.lng, b.lat, b.lng));
            }
        });
        for (int i = 0; i < shops.size() && i < 2; i++) {
            final Models.Shop shop = shops.get(i);
            View row = getLayoutInflater().inflate(R.layout.item_sky, box, false);
            ImageView icon = row.findViewById(R.id.sky_icon);
            icon.setImageResource(R.drawable.ic_store);
            TextView label = row.findViewById(R.id.sky_label);
            label.setText(shop.name);
            TextView distance = row.findViewById(R.id.sky_time);
            distance.setText(Geo.kmText(Geo.km(spot.lat, spot.lng, shop.lat, shop.lng)));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(SpotDetailActivity.this, ShopDetailActivity.class);
                    intent.putExtra("id", shop.id);
                    startActivity(intent);
                }
            });
            box.addView(row);
        }
    }

    // ================================================================ favourite (saved in SQLite, then synced)

    private void toggleFavorite() {
        Db db = Db.get(this);
        int user = Session.userId(this);
        boolean favourite = !db.isFavorite(user, spot.id);
        db.setFavorite(user, spot.id, favourite);
        updateFavorite();
        Ui.pop(findViewById(R.id.fav), 0);
        if (favourite) {
            toast(R.string.fav_added);
        } else {
            toast(R.string.fav_removed);
        }
        SyncManager.push(this, null);
    }

    private void updateFavorite() {
        MaterialButton button = findViewById(R.id.fav);
        if (Db.get(this).isFavorite(Session.userId(this), spot.id)) {
            button.setIconResource(R.drawable.ic_heart_filled);
            button.setIconTintResource(R.color.accent_strong);
        } else {
            button.setIconResource(R.drawable.ic_heart);
            button.setIconTintResource(R.color.text);
        }
    }

    // ================================================================ reminder (local notification)

    private void remind() {
        if (!Notifier.allowed(this)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
            return;
        }
        if (!Session.notifications(this)) {
            toast(R.string.notifications_off);
            return;
        }
        long now = System.currentTimeMillis();
        Conditions.Window w = Conditions.bestWindow(SpotForecast.of(this, spot).results, now + HOUR, 36 * HOUR);
        long start;
        if (w != null) {
            start = w.start;
        } else {
            start = Astro.day(now + 12 * HOUR, spot.lat, spot.lng).dawnWindow()[0];
        }
        long showAt = Math.max(now + 10000, start - 30 * MINUTE);
        Notifier.scheduleReminder(this, spot.id, spot.name(lang()), start, showAt);
        toast(getString(R.string.reminder_set, Fmt.weekday(showAt, locale()) + " " + Fmt.hm(showAt)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 2 && Notifier.allowed(this)) {
            remind();
        }
    }

    // ================================================================ community reviews (PHP/MySQL)

    private void loadReviews() {
        HashMap<String, String> params = new HashMap<>();
        params.put("action", "list");
        params.put("spot_id", String.valueOf(spot.id));
        Api.call(this, "reviews.php", params, new Api.Listener() {
            @Override
            public void onResult(JSONObject data, String error) {
                if (isDestroyed()) {
                    return;
                }
                if (error == null) {
                    showReviews(data);
                } else {
                    showReviews(null);
                }
            }
        });
    }

    private void showReviews(JSONObject data) {
        TextView summary = findViewById(R.id.rating_summary);
        LinearLayout box = findViewById(R.id.reviews);
        box.removeAllViews();
        RatingBar stars = findViewById(R.id.rating_stars);
        if (data == null) {
            summary.setText(R.string.reviews_offline);
            stars.setRating(0);
            return;
        }
        int count = data.optInt("count");
        double average = data.optDouble("avg", 0);
        stars.setRating((float) average);
        if (count == 0) {
            summary.setText(R.string.no_reviews);
        } else {
            summary.setText(getString(R.string.rating_summary, String.format(Locale.US, "%.1f", average), count));
        }
        TextView community = findViewById(R.id.community_line);
        String top = spot.topSpecies;
        if (top.isEmpty()) {
            top = "–";
        }
        community.setText(getString(R.string.community_line, spot.nCatches, top));

        JSONArray reviews = data.optJSONArray("reviews");
        for (int i = 0; reviews != null && i < reviews.length() && i < 3; i++) {
            JSONObject review = reviews.optJSONObject(i);
            View row = getLayoutInflater().inflate(R.layout.item_review, box, false);
            TextView avatar = row.findViewById(R.id.review_avatar);
            avatar.setText(Ui.initials(review.optString("author")));
            TextView author = row.findViewById(R.id.review_author);
            author.setText(review.optString("author"));
            RatingBar reviewStars = row.findViewById(R.id.review_stars);
            reviewStars.setRating(review.optInt("rating"));
            TextView text = row.findViewById(R.id.review_text);
            String comment = review.optString("comment");
            text.setText(comment);
            if (comment.isEmpty()) {
                text.setVisibility(View.GONE);
            } else {
                text.setVisibility(View.VISIBLE);
            }
            box.addView(row);
        }
    }

    private void showRateDialog() {
        View form = getLayoutInflater().inflate(R.layout.dialog_review, null);
        final RatingBar bar = form.findViewById(R.id.dlg_rating);
        final EditText comment = form.findViewById(R.id.dlg_comment);

        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(getString(R.string.rate_title, spot.name(lang())));
        dialog.setView(form);
        dialog.setNegativeButton(R.string.cancel, null);
        dialog.setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                int rating = Math.round(bar.getRating());
                if (rating < 1) {
                    toast(R.string.rating_required);
                    return;
                }
                HashMap<String, String> params = new HashMap<>();
                params.put("action", "save");
                params.put("spot_id", String.valueOf(spot.id));
                params.put("rating", String.valueOf(rating));
                params.put("comment", comment.getText().toString().trim());
                Api.call(SpotDetailActivity.this, "reviews.php", params, new Api.Listener() {
                    @Override
                    public void onResult(JSONObject data, String error) {
                        if (error != null) {
                            toast(Api.message(SpotDetailActivity.this, error));
                            return;
                        }
                        toast(R.string.review_thanks);
                        showReviews(data);
                    }
                });
            }
        });
        dialog.show();
    }
}
