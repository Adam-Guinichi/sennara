package com.sennara.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.sennara.app.R;
import com.sennara.app.data.Db;
import com.sennara.app.data.Models;
import com.sennara.app.data.Session;
import com.sennara.app.data.SyncManager;
import com.sennara.app.ui.adapters.CardAdapter;
import com.sennara.app.util.Achievements;
import com.sennara.app.util.Fmt;
import com.sennara.app.util.Geo;
import com.sennara.app.util.Images;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

public class AddCatchActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_PHOTO = 1;

    private Models.CatchItem item;
    private ArrayList<Models.Fish> fish = new ArrayList<>();
    private ArrayList<Models.Spot> spots = new ArrayList<>();
    private CardAdapter picker = new CardAdapter();
    private Models.Fish selected;
    private String pendingPhoto = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Db db = Db.get(this);

        String uuid = getIntent().getStringExtra("uuid");
        item = null;
        if (uuid != null) {
            item = db.catchItem(uuid);
        }
        boolean editing = item != null;
        if (!editing) {
            item = new Models.CatchItem();
            item.uuid = UUID.randomUUID().toString();
            item.userId = Session.userId(this);
            item.caughtAt = System.currentTimeMillis();
            item.spotId = getIntent().getIntExtra("spot_id", Session.prefs(this).getInt("s_spot", 0));
            item.fishId = getIntent().getIntExtra("fish_id", 0);
        }
        if (savedInstanceState != null) {
            pendingPhoto = savedInstanceState.getString("pendingPhoto", "");
            item.photo = savedInstanceState.getString("photo", item.photo);
        }
        if (editing) {
            setupSimple(R.layout.activity_add_catch, getString(R.string.edit_catch));
        } else {
            setupSimple(R.layout.activity_add_catch, getString(R.string.add_catch));
        }

        fish.addAll(db.fish());
        RecyclerView pickerList = findViewById(R.id.fish_picker);
        pickerList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        pickerList.setAdapter(picker);
        for (Models.Fish f : fish) {
            if (f.id == item.fishId) {
                selected = f;
            }
        }
        showPicker();

        setupSpotSpinner(editing);

        EditText weight = findViewById(R.id.weight);
        EditText length = findViewById(R.id.length);
        if (editing && savedInstanceState == null) {
            fillForm(weight, length);
        }
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkSize();
            }
        };
        weight.addTextChangedListener(watcher);
        length.addTextChangedListener(watcher);

        showPhoto();
        checkSize();
        findViewById(R.id.btn_photo).setOnClickListener(this);
        findViewById(R.id.submit).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_photo) {
            takePhoto();
        } else if (v.getId() == R.id.submit) {
            save();
        }
    }

    private void setupSpotSpinner(boolean editing) {
        final double[] here = Geo.here(this);
        spots.addAll(Db.get(this).spots());
        Collections.sort(spots, new Comparator<Models.Spot>() {
            @Override
            public int compare(Models.Spot a, Models.Spot b) {
                return Double.compare(Geo.km(here[0], here[1], a.lat, a.lng), Geo.km(here[0], here[1], b.lat, b.lng));
            }
        });
        ArrayList<String> names = new ArrayList<>();
        names.add(getString(R.string.other_place));
        int position = 0;
        if (!editing && item.spotId == 0 && !spots.isEmpty()) {
            Models.Spot first = spots.get(0);
            if (Geo.km(here[0], here[1], first.lat, first.lng) < 3) {
                position = 1;
            }
        }
        for (int i = 0; i < spots.size(); i++) {
            names.add(spots.get(i).name(lang()));
            if (spots.get(i).id == item.spotId) {
                position = i + 1;
            }
        }
        Spinner spinner = findViewById(R.id.spot);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(position);
    }

    private void fillForm(EditText weight, EditText length) {
        if (item.fishId == 0) {
            EditText other = findViewById(R.id.other_species);
            other.setText(item.species);
        }
        if (item.weightG > 0) {
            weight.setText(String.valueOf(item.weightG));
        }
        if (item.lengthCm > 0) {
            length.setText(String.valueOf(item.lengthCm));
        }
        EditText note = findViewById(R.id.note);
        note.setText(item.note);
        MaterialSwitch released = findViewById(R.id.released);
        released.setChecked(item.released);
        ChipGroup baits = findViewById(R.id.baits);
        for (int i = 0; i < baits.getChildCount(); i++) {
            View chip = baits.getChildAt(i);
            if (item.bait.equals(chip.getTag())) {
                baits.check(chip.getId());
            }
        }
    }

    private void showPicker() {
        ArrayList<CardAdapter.Card> cards = new ArrayList<>();
        for (final Models.Fish f : fish) {
            CardAdapter.Card card = new CardAdapter.Card(CardAdapter.FISH_PICK);
            card.image = f.image;
            card.title = f.name(lang());
            card.selected = selected != null && selected.id == f.id;
            card.listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (selected != null && selected.id == f.id) {
                        selected = null;
                    } else {
                        selected = f;
                    }
                    showPicker();
                    checkSize();
                }
            };
            cards.add(card);
        }
        picker.setCards(cards);

        TextView picked = findViewById(R.id.picked);
        if (selected == null) {
            findViewById(R.id.other_layout).setVisibility(View.VISIBLE);
            picked.setText(R.string.pick_fish);
        } else {
            findViewById(R.id.other_layout).setVisibility(View.GONE);
            picked.setText(selected.name(lang()) + " · " + selected.sci);
        }
    }

    private int number(int id) {
        try {
            return Integer.parseInt(text(id));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void checkSize() {
        TextView warning = findViewById(R.id.size_warning);
        String text = null;
        if (selected != null) {
            int length = number(R.id.length);
            int grams = number(R.id.weight);
            boolean threatened = selected.iucn.equals("VU") || selected.iucn.equals("EN") || selected.iucn.equals("CR");
            if (selected.minWeightG > 0 && grams > 0 && grams < selected.minWeightG) {
                text = getString(R.string.warn_weight, selected.name(lang()), Fmt.weight(selected.minWeightG));
            } else if (selected.minSizeCm > 0 && length > 0 && length < selected.minSizeCm) {
                if (selected.sizeRule.equals("tn_rule")) {
                    text = getString(R.string.warn_size_tn, selected.name(lang()), selected.minSizeCm);
                } else {
                    text = getString(R.string.warn_size_eu, selected.name(lang()), selected.minSizeCm);
                }
            } else if (threatened) {
                text = getString(R.string.warn_threatened, selected.name(lang()));
            }
        }
        if (text == null) {
            warning.setVisibility(View.GONE);
        } else {
            warning.setVisibility(View.VISIBLE);
            warning.setText(text);
            MaterialSwitch released = findViewById(R.id.released);
            released.setChecked(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("pendingPhoto", pendingPhoto);
        out.putString("photo", item.photo);
    }

    // ================================================================ camera (implicit intent + startActivityForResult)

    private void takePhoto() {
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "catch_" + System.currentTimeMillis() + ".jpg");
        pendingPhoto = file.getAbsolutePath();
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_PHOTO);
        } catch (ActivityNotFoundException e) {
            toast(R.string.error_no_app);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PHOTO && resultCode == RESULT_OK) {
            item.photo = pendingPhoto;
            showPhoto();
        }
    }

    private void showPhoto() {
        ImageView view = findViewById(R.id.photo);
        boolean hasPhoto = item.photo != null && !item.photo.isEmpty() && new File(item.photo).exists();
        if (hasPhoto) {
            view.setVisibility(View.VISIBLE);
            view.setImageBitmap(Images.decode(new File(item.photo), 900));
        } else {
            view.setVisibility(View.GONE);
        }
    }

    // ================================================================ save

    private void save() {
        if (selected == null) {
            String other = text(R.id.other_species);
            if (other.isEmpty()) {
                toast(R.string.species_required);
                return;
            }
            item.fishId = 0;
            item.species = other;
        } else {
            item.fishId = selected.id;
            item.species = selected.name(lang());
        }
        item.weightG = number(R.id.weight);
        item.lengthCm = number(R.id.length);
        ChipGroup baits = findViewById(R.id.baits);
        View bait = baits.findViewById(baits.getCheckedChipId());
        if (bait == null) {
            item.bait = "";
        } else {
            item.bait = String.valueOf(bait.getTag());
        }
        MaterialSwitch released = findViewById(R.id.released);
        item.released = released.isChecked();
        item.note = text(R.id.note);
        Spinner spinner = findViewById(R.id.spot);
        int position = spinner.getSelectedItemPosition();
        if (position > 0) {
            item.spotId = spots.get(position - 1).id;
            item.spotName = spots.get(position - 1).name;
        } else {
            item.spotId = 0;
            item.spotName = "";
        }
        double[] here = Geo.here(this);
        item.lat = here[0];
        item.lng = here[1];

        Db db = Db.get(this);
        ArrayList<Achievements.Badge> before = Achievements.compute(db.catches(item.userId));
        int previousBest = 0;
        for (Models.CatchItem x : db.catches(item.userId)) {
            if (!x.uuid.equals(item.uuid) && x.species.equalsIgnoreCase(item.species) && x.weightG > previousBest) {
                previousBest = x.weightG;
            }
        }

        db.saveCatch(item);
        SyncManager.push(this, null);

        ArrayList<Achievements.Badge> after = Achievements.compute(db.catches(item.userId));
        ArrayList<String> unlocked = new ArrayList<>();
        for (Achievements.Badge badge : Achievements.newlyUnlocked(before, after)) {
            unlocked.add(badge.id);
        }
        boolean personalBest = item.weightG > 0 && previousBest > 0 && item.weightG > previousBest;

        Intent intent = new Intent(this, CatchSuccessActivity.class);
        intent.putExtra("uuid", item.uuid);
        intent.putExtra("pb", personalBest);
        intent.putStringArrayListExtra("badges", unlocked);
        startActivity(intent);
        finish();
    }
}
