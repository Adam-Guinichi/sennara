package com.sennara.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sennara.app.R;
import com.sennara.app.util.Credits;
import com.sennara.app.util.Images;

import org.json.JSONArray;
import org.json.JSONObject;

public class CreditsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupSimple(R.layout.activity_credits, getString(R.string.credits_title));
        showSources();
        showPhotoCredits();
    }

    private void showSources() {
        String[][] sources = {
                {getString(R.string.src_osm), "https://www.openstreetmap.org/copyright"},
                {getString(R.string.src_meteo), "https://open-meteo.com/"},
                {getString(R.string.src_wikidata), "https://www.wikidata.org/"},
                {getString(R.string.src_commons), "https://commons.wikimedia.org/"},
                {getString(R.string.src_eu), "https://eur-lex.europa.eu/eli/reg/2006/1967/oj"},
                {getString(R.string.src_tn), "https://www.webdo.tn/fr/actualite/national/peche-au-poulpe-linterdiction-prolongee-jusquau-15-novembre/405590/"},
                {getString(R.string.src_astro), "https://stjarnhimlen.se/comp/ppcomp.html"},
                {getString(R.string.src_icons), "https://fonts.google.com/icons"},
                {getString(R.string.src_font), "https://github.com/IBM/plex"},
        };
        LinearLayout box = findViewById(R.id.sources);
        for (final String[] source : sources) {
            View row = getLayoutInflater().inflate(R.layout.item_sky, box, false);
            ImageView icon = row.findViewById(R.id.sky_icon);
            icon.setImageResource(R.drawable.ic_globe);
            TextView label = row.findViewById(R.id.sky_label);
            label.setText(source[0]);
            TextView empty = row.findViewById(R.id.sky_time);
            empty.setText("");
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openUrl(source[1]);
                }
            });
            box.addView(row);
        }
    }

    private void showPhotoCredits() {
        LinearLayout box = findViewById(R.id.photos);
        JSONArray all = Credits.all(this);
        for (int i = 0; i < all.length(); i++) {
            final JSONObject credit = all.optJSONObject(i);
            View row = getLayoutInflater().inflate(R.layout.item_credit, box, false);
            ImageView image = row.findViewById(R.id.c_image);
            Images.load(image, credit.optString("file"), 160);
            TextView title = row.findViewById(R.id.c_title);
            title.setText(credit.optString("title"));
            TextView author = row.findViewById(R.id.c_author);
            author.setText(getString(R.string.photo_credit, credit.optString("author"), credit.optString("license")));
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openUrl(credit.optString("source"));
                }
            });
            box.addView(row);
        }
        if (all.length() == 0) {
            findViewById(R.id.no_credits).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.no_credits).setVisibility(View.GONE);
        }
    }
}
