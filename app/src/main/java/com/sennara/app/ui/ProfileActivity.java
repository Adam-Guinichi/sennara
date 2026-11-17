package com.sennara.app.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.sennara.app.R;
import com.sennara.app.data.Api;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.data.Session;
import com.sennara.app.data.SyncManager;
import com.sennara.app.data.WeatherRepo;
import com.sennara.app.notify.AlertWorker;
import com.sennara.app.notify.Notifier;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Labels;
import com.sennara.app.util.Ui;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ProfileActivity extends BaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupSimple(R.layout.activity_profile, getString(R.string.profile));
        bindLanguageToggle();
        fillForm();
        setupSwitches();

        int[] buttons = {R.id.save, R.id.check_alerts, R.id.test_notif, R.id.sync, R.id.safety,
                R.id.rules, R.id.credits, R.id.tutorials, R.id.my_shop, R.id.admin, R.id.logout};
        for (int id : buttons) {
            findViewById(id).setOnClickListener(this);
        }

        if (Session.isSeller(this) || Session.isAdmin(this)) {
            findViewById(R.id.my_shop).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.my_shop).setVisibility(View.GONE);
        }
        if (Session.isAdmin(this)) {
            findViewById(R.id.admin).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.admin).setVisibility(View.GONE);
        }
        showInfo();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.save) {
            saveProfile();
        } else if (id == R.id.check_alerts) {
            AlertWorker.runNow(this);
            toast(R.string.alert_check_started);
        } else if (id == R.id.test_notif) {
            if (Notifier.allowed(this)) {
                Notifier.show(this, Notifier.CHANNEL_REMINDERS, 4, getString(R.string.notif_test_title),
                        getString(R.string.notif_test_text), null);
            } else {
                askNotificationPermission();
            }
        } else if (id == R.id.sync) {
            sync();
        } else if (id == R.id.safety) {
            open(SafetyActivity.class);
        } else if (id == R.id.rules) {
            open(RulesActivity.class);
        } else if (id == R.id.credits) {
            open(CreditsActivity.class);
        } else if (id == R.id.tutorials) {
            open(TutorialsActivity.class);
        } else if (id == R.id.my_shop) {
            open(SellerActivity.class);
        } else if (id == R.id.admin) {
            open(AdminActivity.class);
        } else if (id == R.id.logout) {
            Session.logout(this);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    private void fillForm() {
        EditText name = findViewById(R.id.name);
        name.setText(Session.name(this));
        EditText phone = findViewById(R.id.phone);
        phone.setText(Session.phone(this));
        EditText city = findViewById(R.id.city);
        city.setText(Session.city(this));

        ChipGroup level = findViewById(R.id.level);
        for (int i = 0; i < level.getChildCount(); i++) {
            View chip = level.getChildAt(i);
            if (Session.level(this).equals(chip.getTag())) {
                level.check(chip.getId());
            }
        }
        ChipGroup styles = findViewById(R.id.styles);
        for (int i = 0; i < styles.getChildCount(); i++) {
            Chip chip = (Chip) styles.getChildAt(i);
            chip.setChecked(Session.styles(this).contains(String.valueOf(chip.getTag())));
        }
    }

    private void setupSwitches() {
        MaterialSwitch reminders = findViewById(R.id.notif_switch);
        reminders.setChecked(Session.notifications(this));
        reminders.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean on) {
                Session.setNotifications(ProfileActivity.this, on);
                if (on) {
                    askNotificationPermission();
                }
            }
        });
        MaterialSwitch alerts = findViewById(R.id.alert_switch);
        alerts.setChecked(Session.alerts(this));
        alerts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean on) {
                Session.setAlerts(ProfileActivity.this, on);
            }
        });
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
        }
    }

    private void showInfo() {
        String name = Session.name(this);
        TextView avatar = findViewById(R.id.avatar);
        avatar.setText(Ui.initials(name));
        TextView nameBig = findViewById(R.id.name_big);
        nameBig.setText(name);
        TextView email = findViewById(R.id.email);
        email.setText(Session.email(this));
        TextView role = findViewById(R.id.role);
        Ui.verdictPill(role, 3);
        role.setText(Labels.role(this, Session.role(this)));
        TextView since = findViewById(R.id.since);
        String created = Session.memberSince(this);
        if (created.length() >= 10) {
            since.setText(getString(R.string.member_since, created.substring(0, 10)));
        } else {
            since.setText("");
        }

        Db db = Db.get(this);
        ArrayList<Models.CatchItem> catches = db.catches(Session.userId(this));
        HashSet<String> species = new HashSet<>();
        for (Models.CatchItem c : catches) {
            species.add(c.species.toLowerCase());
        }
        TextView catchCount = findViewById(R.id.p_catches);
        catchCount.setText(String.valueOf(catches.size()));
        TextView speciesCount = findViewById(R.id.p_species);
        speciesCount.setText(String.valueOf(species.size()));
        TextView favourites = findViewById(R.id.p_favs);
        favourites.setText(String.valueOf(db.favoriteIds(Session.userId(this)).size()));

        long last = Session.lastSync(this);
        String text;
        if (last == 0) {
            text = getString(R.string.never_synced);
        } else {
            text = getString(R.string.last_sync, Fmt.dateTime(last, locale()));
        }
        int pending = db.unsyncedCount(Session.userId(this));
        if (pending > 0) {
            text = text + "\n" + getString(R.string.pending_sync, pending);
        }
        TextView syncText = findViewById(R.id.sync_text);
        syncText.setText(text);
    }

    private void saveProfile() {
        if (text(R.id.name).isEmpty()) {
            toast(R.string.fill_all_fields);
            return;
        }
        ChipGroup levelGroup = findViewById(R.id.level);
        View levelChip = levelGroup.findViewById(levelGroup.getCheckedChipId());
        String level = Session.level(this);
        if (levelChip != null) {
            level = String.valueOf(levelChip.getTag());
        }
        ChipGroup styles = findViewById(R.id.styles);
        String picked = "";
        List<Integer> checked = styles.getCheckedChipIds();
        for (int chipId : checked) {
            if (!picked.isEmpty()) {
                picked = picked + ",";
            }
            picked = picked + findViewById(chipId).getTag();
        }

        HashMap<String, String> params = new HashMap<>();
        params.put("action", "update");
        params.put("name", text(R.id.name));
        params.put("phone", text(R.id.phone));
        params.put("city", text(R.id.city));
        params.put("level", level);
        params.put("styles", picked);
        Api.call(this, "auth.php", params, new Api.Listener() {
            @Override
            public void onResult(JSONObject data, String error) {
                if (error != null) {
                    toast(Api.message(ProfileActivity.this, error));
                    return;
                }
                Session.updateUser(ProfileActivity.this, data.optJSONObject("user"));
                showInfo();
                toast(R.string.saved);
            }
        });
    }

    private void sync() {
        final TextView syncText = findViewById(R.id.sync_text);
        syncText.setText(R.string.syncing);
        SyncManager.sync(this, new SyncManager.Listener() {
            @Override
            public void onProgress(int percent, String status) {
                syncText.setText(status);
            }

            @Override
            public void onDone(final boolean ok, final String error) {
                if (isDestroyed()) {
                    return;
                }
                WeatherRepo.refresh(ProfileActivity.this, true, new WeatherRepo.Listener() {
                    @Override
                    public void onDone(boolean weatherOk) {
                        showInfo();
                        if (ok) {
                            toast(R.string.sync_done);
                        } else {
                            toast(Api.message(ProfileActivity.this, error));
                        }
                    }
                });
            }
        });
    }
}
