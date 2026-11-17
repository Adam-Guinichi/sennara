package com.sennara.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.sennara.app.R;
import com.sennara.app.data.Api;
import com.sennara.app.data.Session;
import com.sennara.app.util.Ui;

import org.json.JSONObject;

import java.util.HashMap;

public class LoginActivity extends BaseActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        edgeToEdge(findViewById(R.id.root), findViewById(R.id.login_top), findViewById(R.id.login_sheet), true);

        findViewById(R.id.login).setOnClickListener(this);
        findViewById(R.id.register).setOnClickListener(this);

        bindLanguageToggle();
        Ui.rise(findViewById(R.id.login_sheet), 80);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.login) {
            login();
        } else if (id == R.id.register) {
            open(RegisterActivity.class);
        }
    }

    private void login() {
        String email = text(R.id.email);
        String password = text(R.id.password);
        if (email.isEmpty() || password.isEmpty()) {
            toast(R.string.fill_all_fields);
            return;
        }
        setBusy(true);

        HashMap<String, String> params = new HashMap<>();
        params.put("action", "login");
        params.put("email", email);
        params.put("password", password);
        Api.call(this, "auth.php", params, new Api.Listener() {
            @Override
            public void onResult(JSONObject data, String error) {
                setBusy(false);
                if (error != null) {
                    toast(Api.message(LoginActivity.this, error));
                    return;
                }
                Session.save(LoginActivity.this, data.optJSONObject("user"), data.optString("token"));
                Intent intent = new Intent(LoginActivity.this, SplashActivity.class);
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
        findViewById(R.id.login).setEnabled(!busy);
    }
}
