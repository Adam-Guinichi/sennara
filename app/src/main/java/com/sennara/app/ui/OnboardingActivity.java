package com.sennara.app.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.core.app.ActivityCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.sennara.app.R;
import com.sennara.app.data.Api;
import com.sennara.app.data.Session;
import com.sennara.app.notify.Notifier;
import com.sennara.app.util.Geo;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class OnboardingActivity extends BaseActivity implements View.OnClickListener {
    private ViewFlipper flipper;
    private LinearProgressIndicator progress;
    private String level = "beginner";
    private int[] levelCards = {R.id.lvl_beginner, R.id.lvl_intermediate, R.id.lvl_expert};
    private String[] levelCodes = {"beginner", "intermediate", "expert"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        edgeToEdge(findViewById(R.id.root), findViewById(R.id.ob_top), findViewById(R.id.bottom_area), false);
        flipper = findViewById(R.id.flipper);
        progress = findViewById(R.id.ob_progress);
        TextView hello = findViewById(R.id.ob_hello);
        hello.setText(getString(R.string.ob_hello, Session.name(this)));

        String current = Session.level(this);
        for (int i = 0; i < levelCards.length; i++) {
            MaterialCardView card = findViewById(levelCards[i]);
            card.setOnClickListener(this);
            if (levelCodes[i].equals(current)) {
                card.setChecked(true);
                level = levelCodes[i];
            } else {
                card.setChecked(false);
            }
        }

        ChipGroup styles = findViewById(R.id.ob_styles);
        String saved = Session.styles(this);
        for (int i = 0; i < styles.getChildCount(); i++) {
            Chip chip = (Chip) styles.getChildAt(i);
            chip.setChecked(saved.contains(String.valueOf(chip.getTag())));
        }

        findViewById(R.id.ob_location).setOnClickListener(this);
        findViewById(R.id.ob_notifications).setOnClickListener(this);
        findViewById(R.id.ob_next).setOnClickListener(this);
        findViewById(R.id.ob_skip).setOnClickListener(this);
        showStep(0);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ob_location) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, 1);
        } else if (id == R.id.ob_notifications) {
            if (Build.VERSION.SDK_INT >= 33) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
            }
        } else if (id == R.id.ob_next) {
            int step = flipper.getDisplayedChild();
            if (step < 2) {
                showStep(step + 1);
            } else {
                finishOnboarding();
            }
        } else if (id == R.id.ob_skip) {
            finishOnboarding();
        } else {
            for (int i = 0; i < levelCards.length; i++) {
                MaterialCardView card = findViewById(levelCards[i]);
                if (levelCards[i] == id) {
                    card.setChecked(true);
                    level = levelCodes[i];
                } else {
                    card.setChecked(false);
                }
            }
        }
    }

    private void showStep(int step) {
        flipper.setDisplayedChild(step);
        progress.setProgress((step + 1) * 100 / 3, true);
        TextView next = findViewById(R.id.ob_next);
        if (step == 2) {
            next.setText(R.string.ob_finish);
        } else {
            next.setText(R.string.next);
        }
        updatePermissionButtons();
    }

    private void updatePermissionButtons() {
        TextView location = findViewById(R.id.ob_location);
        if (Geo.hasPermission(this)) {
            location.setText(R.string.ob_location_done);
            location.setEnabled(false);
        } else {
            location.setText(R.string.ob_location);
            location.setEnabled(true);
        }
        TextView notifications = findViewById(R.id.ob_notifications);
        if (Notifier.allowed(this)) {
            notifications.setText(R.string.ob_notif_done);
            notifications.setEnabled(false);
        } else {
            notifications.setText(R.string.ob_notif);
            notifications.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        updatePermissionButtons();
        if (requestCode == 1) {
            Geo.refresh(this, null);
        }
    }

    private void finishOnboarding() {
        ChipGroup styles = findViewById(R.id.ob_styles);
        String picked = "";
        List<Integer> checked = styles.getCheckedChipIds();
        for (int chipId : checked) {
            if (!picked.isEmpty()) {
                picked = picked + ",";
            }
            picked = picked + findViewById(chipId).getTag();
        }
        findViewById(R.id.ob_next).setEnabled(false);

        HashMap<String, String> params = new HashMap<>();
        params.put("action", "update");
        params.put("level", level);
        params.put("styles", picked);
        Api.call(this, "auth.php", params, new Api.Listener() {
            @Override
            public void onResult(JSONObject data, String error) {
                if (error == null) {
                    Session.updateUser(OnboardingActivity.this, data.optJSONObject("user"));
                }
                Session.setOnboarded(OnboardingActivity.this);
                Intent intent = new Intent(OnboardingActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }
}
