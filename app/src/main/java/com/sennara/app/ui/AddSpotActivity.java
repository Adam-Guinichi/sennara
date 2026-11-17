package com.sennara.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.google.android.material.chip.ChipGroup;
import com.sennara.app.R;
import com.sennara.app.data.Api;
import com.sennara.app.util.Geo;
import com.sennara.app.util.Images;

import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

public class AddSpotActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_PHOTO = 1;
    private File photo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupSimple(R.layout.activity_add_spot, getString(R.string.suggest_spot));
        findViewById(R.id.photo_box).setOnClickListener(this);
        findViewById(R.id.btn_location).setOnClickListener(this);
        findViewById(R.id.submit).setOnClickListener(this);
        showLocation();
        refreshLocation();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.photo_box) {
            takePhoto();
        } else if (id == R.id.btn_location) {
            refreshLocation();
        } else if (id == R.id.submit) {
            submit();
        }
    }

    private void refreshLocation() {
        Geo.refresh(this, new Runnable() {
            @Override
            public void run() {
                showLocation();
            }
        });
    }

    private void showLocation() {
        TextView location = findViewById(R.id.location);
        if (Geo.known(this)) {
            double[] here = Geo.here(this);
            location.setText(getString(R.string.spot_location, String.format(Locale.US, "%.5f, %.5f", here[0], here[1])));
        } else {
            location.setText(R.string.location_default);
        }
    }

    // ================================================================ camera (implicit intent + startActivityForResult)

    private void takePhoto() {
        photo = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "spot_" + System.currentTimeMillis() + ".jpg");
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", photo);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_PHOTO);
        } catch (ActivityNotFoundException e) {
            photo = null;
            toast(R.string.error_no_app);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PHOTO) {
            return;
        }
        if (resultCode == RESULT_OK && photo != null) {
            ImageView view = findViewById(R.id.photo);
            view.setImageBitmap(Images.decode(photo, 900));
            view.setVisibility(View.VISIBLE);
            findViewById(R.id.photo_hint).setVisibility(View.GONE);
        } else {
            photo = null;
        }
    }

    // ================================================================ send

    private void submit() {
        String name = text(R.id.name);
        if (name.isEmpty()) {
            toast(R.string.fill_all_fields);
            return;
        }
        ChipGroup types = findViewById(R.id.types);
        View chip = types.findViewById(types.getCheckedChipId());
        String type = "shore";
        if (chip != null) {
            type = String.valueOf(chip.getTag());
        }
        double[] here = Geo.here(this);
        setBusy(true);

        HashMap<String, String> params = new HashMap<>();
        params.put("action", "add");
        params.put("name", name);
        params.put("city", text(R.id.city));
        params.put("description", text(R.id.description));
        params.put("type", type);
        params.put("lat", String.valueOf(here[0]));
        params.put("lng", String.valueOf(here[1]));
        Api.upload(this, "spots.php", params, photo, new Api.Listener() {
            @Override
            public void onResult(JSONObject data, String error) {
                setBusy(false);
                if (error != null) {
                    toast(Api.message(AddSpotActivity.this, error));
                    return;
                }
                if (data.optBoolean("approved")) {
                    toast(R.string.spot_published);
                } else {
                    toast(R.string.spot_sent);
                }
                finish();
            }
        });
    }

    private void setBusy(boolean busy) {
        if (busy) {
            findViewById(R.id.busy).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.busy).setVisibility(View.GONE);
        }
        findViewById(R.id.submit).setEnabled(!busy);
    }
}
