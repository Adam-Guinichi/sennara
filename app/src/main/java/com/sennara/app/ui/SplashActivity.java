package com.sennara.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sennara.app.R;
import com.sennara.app.data.Session;
import com.sennara.app.data.SyncManager;
import com.sennara.app.data.WeatherRepo;
import com.sennara.app.util.Ui;

import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends BaseActivity {
    private Timer timer = new Timer();
    private ProgressBar bar;
    private TextView status;
    private int percent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        edgeToEdge(findViewById(R.id.root), findViewById(R.id.splash_top), findViewById(R.id.splash_bottom), true);
        bar = findViewById(R.id.progress);
        status = findViewById(R.id.status);
        Ui.rise(findViewById(R.id.splash_title), 100);
        Ui.rise(findViewById(R.id.splash_tagline), 200);

        if (Session.loggedIn(this)) {
            startSync();
        } else {
            startTimer();
        }
    }

    private void startTimer() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        percent = percent + 4;
                        bar.setProgress(percent);
                        if (percent >= 100) {
                            timer.cancel();
                            go(LoginActivity.class);
                        }
                    }
                });
            }
        }, 300, 40);
    }

    private void startSync() {
        SyncManager.sync(this, new SyncManager.Listener() {
            @Override
            public void onProgress(int value, String text) {
                bar.setProgress(value, true);
                status.setText(text);
            }

            @Override
            public void onDone(boolean ok, String error) {
                if ("not_logged_in".equals(error)) {
                    Session.logout(SplashActivity.this);
                    go(LoginActivity.class);
                    return;
                }
                if (!ok) {
                    toast(R.string.offline_mode);
                }
                status.setText(R.string.sync_step_weather);
                WeatherRepo.refresh(SplashActivity.this, false, new WeatherRepo.Listener() {
                    @Override
                    public void onDone(boolean weatherOk) {
                        if (Session.onboarded(SplashActivity.this)) {
                            go(HomeActivity.class);
                        } else {
                            go(OnboardingActivity.class);
                        }
                    }
                });
            }
        });
    }

    private void go(Class<?> next) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Intent intent = new Intent(this, next);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        timer.cancel();
        super.onDestroy();
    }
}
