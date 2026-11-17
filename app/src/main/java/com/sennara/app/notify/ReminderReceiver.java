package com.sennara.app.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sennara.app.R;
import com.sennara.app.ui.SpotDetailActivity;
import com.sennara.app.util.Fmt;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        String kind = intent.getStringExtra("kind");

        if ("goal".equals(kind)) {
            String title = c.getString(R.string.notif_goal_title);
            String text = c.getString(R.string.notif_goal_text);
            Notifier.show(c, Notifier.CHANNEL_REMINDERS, 2, title, text, null);
            return;
        }

        String spot = intent.getStringExtra("spot");
        String time = Fmt.hm(intent.getLongExtra("start", 0));
        Intent open = new Intent(c, SpotDetailActivity.class);
        open.putExtra("id", intent.getIntExtra("spot_id", 0));
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String title = c.getString(R.string.notif_window_title);
        String text = c.getString(R.string.notif_window_text, spot, time);
        Notifier.show(c, Notifier.CHANNEL_REMINDERS, 1, title, text, open);
    }
}
