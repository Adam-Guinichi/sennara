package com.sennara.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.util.Achievements;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Images;
import com.sennara.app.util.Ui;

import java.io.File;
import java.util.ArrayList;

public class CatchSuccessActivity extends BaseActivity implements View.OnClickListener {
    private String detail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catch_success);
        edgeToEdge(findViewById(R.id.root), findViewById(R.id.root), findViewById(R.id.root), false);
        Models.CatchItem c = Db.get(this).catchItem(getIntent().getStringExtra("uuid"));
        if (c == null) {
            finish();
            return;
        }

        ImageView photo = findViewById(R.id.photo);
        if (!c.photo.isEmpty() && new File(c.photo).exists()) {
            photo.setImageBitmap(Images.decode(new File(c.photo), 600));
        } else if (c.fishId > 0) {
            Models.Fish fish = Db.get(this).fish(c.fishId);
            if (fish != null) {
                Images.load(photo, fish.image, 600);
            }
        }

        TextView title = findViewById(R.id.title);
        if (c.released) {
            title.setText(R.string.success_released);
        } else {
            title.setText(R.string.success_title);
        }
        detail = c.species;
        if (c.weightG > 0) {
            detail = detail + " · " + Fmt.weight(c.weightG);
        }
        if (c.lengthCm > 0) {
            detail = detail + " · " + getString(R.string.cm, c.lengthCm);
        }
        TextView detailView = findViewById(R.id.detail);
        detailView.setText(detail);
        TextView where = findViewById(R.id.where);
        if (c.spotName.isEmpty()) {
            where.setText(Fmt.dateTime(c.caughtAt, locale()));
        } else {
            where.setText(c.spotName + " · " + Fmt.hm(c.caughtAt));
        }
        if (getIntent().getBooleanExtra("pb", false)) {
            findViewById(R.id.pb).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.pb).setVisibility(View.GONE);
        }

        ArrayList<String> ids = getIntent().getStringArrayListExtra("badges");
        if (ids == null) {
            ids = new ArrayList<>();
        }
        LinearLayout box = findViewById(R.id.badges);
        for (Achievements.Badge badge : Achievements.compute(Db.get(this).catches(c.userId))) {
            if (!ids.contains(badge.id)) {
                continue;
            }
            View card = getLayoutInflater().inflate(R.layout.card_badge, box, false);
            ImageView icon = card.findViewById(R.id.card_image);
            icon.setImageResource(badge.icon);
            Ui.tint(icon, R.color.accent_strong);
            TextView badgeTitle = card.findViewById(R.id.card_title);
            badgeTitle.setText(badge.title);
            box.addView(card);
            Ui.pop(card, 700 + box.getChildCount() * 120L);
        }
        if (ids.isEmpty()) {
            findViewById(R.id.badges_title).setVisibility(View.GONE);
        } else {
            findViewById(R.id.badges_title).setVisibility(View.VISIBLE);
        }

        Ui.pop(findViewById(R.id.photo_frame), 100);
        Ui.pop(findViewById(R.id.check), 450);
        Ui.rise(findViewById(R.id.title), 300);
        Ui.rise(findViewById(R.id.detail), 380);
        sparkle(R.id.s1, 500);
        sparkle(R.id.s2, 620);
        sparkle(R.id.s3, 740);

        findViewById(R.id.share).setOnClickListener(this);
        findViewById(R.id.done).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.share) {
            shareText(getString(R.string.share_catch, detail));
        } else if (v.getId() == R.id.done) {
            finish();
        }
    }

    private void sparkle(int id, long delay) {
        View star = findViewById(id);
        Ui.pop(star, delay);
        star.animate().rotationBy(90).setStartDelay(delay).setDuration(900).start();
    }
}
