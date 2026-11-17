package com.sennara.app;

import android.app.Application;

import com.sennara.app.notify.AlertWorker;
import com.sennara.app.notify.Notifier;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Notifier.createChannels(this);
        AlertWorker.schedule(this);
    }
}
