package com.sennara.app.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.chip.ChipGroup;
import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.forecast.Conditions;
import com.sennara.app.forecast.SpotForecast;
import com.sennara.app.util.Geo;
import com.sennara.app.util.Images;
import com.sennara.app.util.Labels;
import com.sennara.app.util.Ui;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;

public class MapActivity extends BaseActivity {
    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setupSimple(R.layout.activity_map, getString(R.string.map));
        edgeToEdge(findViewById(R.id.root), findViewById(R.id.top_bar), findViewById(R.id.bottom_area), false);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        double[] here = Geo.here(this);
        double lat = getIntent().getDoubleExtra("lat", here[0]);
        double lng = getIntent().getDoubleExtra("lng", here[1]);
        if (getIntent().hasExtra("lat")) {
            map.getController().setZoom(13.5);
        } else {
            map.getController().setZoom(8.2);
        }
        map.getController().setCenter(new GeoPoint(lat, lng));

        ChipGroup layers = findViewById(R.id.layers);
        layers.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, List<Integer> checkedIds) {
                drawPins();
            }
        });
        findViewById(R.id.my_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Geo.refresh(MapActivity.this, null);
                double[] position = Geo.here(MapActivity.this);
                map.getController().animateTo(new GeoPoint(position[0], position[1]), 12.0, 600L);
            }
        });
        drawPins();
    }

    private String layer() {
        ChipGroup layers = findViewById(R.id.layers);
        int id = layers.getCheckedChipId();
        if (id == R.id.layer_spots) {
            return "spots";
        }
        if (id == R.id.layer_shops) {
            return "shops";
        }
        return "all";
    }

    private void drawPins() {
        map.getOverlays().clear();
        String layer = layer();

        if (!layer.equals("spots")) {
            Drawable shopPin = makePin(R.color.accent, R.drawable.ic_store, 34);
            for (Models.Shop shop : Db.get(this).shops()) {
                String subtitle = getString(R.string.shop_sub, shop.city, Geo.kmText(Geo.kmFromHere(this, shop.lat, shop.lng)));
                Intent details = new Intent(this, ShopDetailActivity.class);
                details.putExtra("id", shop.id);
                addMarker(shop.lat, shop.lng, shopPin, null, shop.name, subtitle, -1, details);
            }
        }

        if (!layer.equals("shops")) {
            for (Models.Spot spot : Db.get(this).spots()) {
                Conditions.Result now = SpotForecast.of(this, spot).now();
                int level = Conditions.FAIR;
                if (now != null) {
                    level = now.level;
                }
                Drawable spotPin = makePin(Labels.levelColor(level), Labels.typeIcon(spot.type), 40);
                String subtitle = Labels.type(this, spot.type) + " · " + Geo.kmText(Geo.kmFromHere(this, spot.lat, spot.lng));
                Intent details = new Intent(this, SpotDetailActivity.class);
                details.putExtra("id", spot.id);
                addMarker(spot.lat, spot.lng, spotPin, spot.image, spot.name(lang()), subtitle, level, details);
            }
        }

        if (Geo.known(this)) {
            double[] here = Geo.here(this);
            addMarker(here[0], here[1], makePin(R.color.primary, R.drawable.ic_person, 30), null, null, null, -1, null);
        }
        map.invalidate();
    }

    private Drawable makePin(int colorRes, int icon, int sizeDp) {
        int size = dp(sizeDp);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Ui.color(this, R.color.white));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setColor(Ui.color(this, colorRes));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - dp(3), paint);
        Drawable iconDrawable = DrawableCompat.wrap(AppCompatResources.getDrawable(this, icon)).mutate();
        DrawableCompat.setTint(iconDrawable, Ui.color(this, R.color.white));
        int padding = size / 4;
        iconDrawable.setBounds(padding, padding, size - padding, size - padding);
        iconDrawable.draw(canvas);
        return new BitmapDrawable(getResources(), bitmap);
    }

    private void addMarker(double lat, double lng, Drawable icon, final String image, final String title,
                           final String subtitle, final int level, final Intent details) {
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lng));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setIcon(icon);
        marker.setInfoWindow(null);
        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker m, MapView mapView) {
                if (title != null) {
                    showInfo(image, title, subtitle, level, details);
                }
                return true;
            }
        });
        map.getOverlays().add(marker);
    }

    private void showInfo(String image, String title, String subtitle, int level, final Intent details) {
        View card = findViewById(R.id.info_card);
        card.setVisibility(View.VISIBLE);
        Ui.rise(card, 0);

        ImageView photo = findViewById(R.id.info_photo);
        if (image == null) {
            photo.setVisibility(View.GONE);
        } else {
            photo.setVisibility(View.VISIBLE);
            Images.load(photo, image, 300);
        }
        TextView titleView = findViewById(R.id.info_title);
        titleView.setText(title);
        TextView subtitleView = findViewById(R.id.info_sub);
        subtitleView.setText(subtitle);
        TextView pill = findViewById(R.id.info_pill);
        if (level < 0) {
            pill.setVisibility(View.GONE);
        } else {
            pill.setVisibility(View.VISIBLE);
            Ui.verdictPill(pill, level);
        }
        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(details);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        map.onPause();
        super.onPause();
    }
}
