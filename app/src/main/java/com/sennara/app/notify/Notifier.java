package com.sennara.app.notify;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.sennara.app.R;
import com.sennara.app.data.Session;
import com.sennara.app.ui.SplashActivity;

public class Notifier {
    public static final String CHANNEL_REMINDERS = "reminders";
    public static final String CHANNEL_ALERTS = "alerts";
    private static final int GOAL_REQUEST = 9001;

    public static void createChannels(Context c) {
        NotificationManager manager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel reminders = new NotificationChannel(CHANNEL_REMINDERS,
                c.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH);
        NotificationChannel alerts = new NotificationChannel(CHANNEL_ALERTS,
                c.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(reminders);
        manager.createNotificationChannel(alerts);
    }

    public static boolean allowed(Context c) {
        if (Build.VERSION.SDK_INT < 33) {
            return true;
        }
        int permission = ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS);
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    public static void show(Context c, String channel, int id, String title, String text, Intent open) {
        if (!Session.notifications(c) || !allowed(c)) {
            return;
        }
        if (open == null) {
            open = new Intent(c, SplashActivity.class);
        }
        PendingIntent pending = PendingIntent.getActivity(c, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(c, channel);
        builder.setSmallIcon(R.drawable.ic_hook);
        builder.setColor(ContextCompat.getColor(c, R.color.primary));
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setAutoCancel(true);
        builder.setContentIntent(pending);

        NotificationManager manager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id, builder.build());
    }

    public static void scheduleReminder(Context c, int spotId, String spotName, long windowStart, long showAt) {
        Intent intent = new Intent(c, ReminderReceiver.class);
        intent.putExtra("kind", "window");
        intent.putExtra("spot", spotName);
        intent.putExtra("spot_id", spotId);
        intent.putExtra("start", windowStart);
        schedule(c, 1000 + spotId, intent, showAt);
    }

    public static void scheduleGoal(Context c, long showAt) {
        Intent intent = new Intent(c, ReminderReceiver.class);
        intent.putExtra("kind", "goal");
        schedule(c, GOAL_REQUEST, intent, showAt);
    }

    public static void cancelGoal(Context c) {
        Intent intent = new Intent(c, ReminderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(c, GOAL_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarms = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        alarms.cancel(pending);
    }

    private static void schedule(Context c, int requestCode, Intent intent, long time) {
        PendingIntent pending = PendingIntent.getBroadcast(c, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarms = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending);
    }
}
