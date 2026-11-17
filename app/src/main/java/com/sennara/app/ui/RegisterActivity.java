package com.sennara.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.card.MaterialCardView;
import com.sennara.app.R;
import com.sennara.app.data.Api;
import com.sennara.app.data.Session;

import org.json.JSONObject;

import java.util.HashMap;

public class RegisterActivity extends BaseActivity implements View.OnClickListener {
    private String role = "fisher";
    private MaterialCardView fisherCard;
    private MaterialCardView sellerCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupSimple(R.layout.activity_register, getString(R.string.create_account));
        fisherCard = findViewById(R.id.role_fisher);
        sellerCard = findViewById(R.id.role_seller);
        fisherCard.setChecked(true);

        fisherCard.setOnClickListener(this);
        sellerCard.setOnClickListener(this);
        findViewById(R.id.submit).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.role_fisher) {
            role = "fisher";
            fisherCard.setChecked(true);
            sellerCard.setChecked(false);
        } else if (id == R.id.role_seller) {
            role = "seller";
            fisherCard.setChecked(false);
            sellerCard.setChecked(true);
        } else if (id == R.id.submit) {
            register();
        }
    }

    private void register() {
        String name = text(R.id.name);
        String email = text(R.id.email);
        String password = text(R.id.password);
        if (name.isEmpty() || email.isEmpty()) {
            toast(R.string.fill_all_fields);
            return;
        }
        if (password.length() < 6) {
            toast(R.string.password_short);
            return;
        }
        setBusy(true);

        HashMap<String, String> params = new HashMap<>();
        params.put("action", "register");
        params.put("name", name);
        params.put("email", email);
        params.put("password", password);
        params.put("phone", text(R.id.phone));
        params.put("city", text(R.id.city));
        params.put("role", role);
        Api.call(this, "auth.php", params, new Api.Listener() {
            @Override
            public void onResult(JSONObject data, String error) {
                setBusy(false);
                if (error != null) {
                    toast(Api.message(RegisterActivity.this, error));
                    return;
                }
                Session.save(RegisterActivity.this, data.optJSONObject("user"), data.optString("token"));
                Intent intent = new Intent(RegisterActivity.this, SplashActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
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
