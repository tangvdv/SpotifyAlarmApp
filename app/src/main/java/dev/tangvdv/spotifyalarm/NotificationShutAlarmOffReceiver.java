package dev.tangvdv.spotifyalarm;

import static androidx.core.app.ServiceCompat.stopForeground;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationShutAlarmOffReceiver extends BroadcastReceiver {
    private static final int NOTIFY_ID = 500;

    @Override
    public void onReceive(Context context, Intent intent) {
        context.stopService(new Intent(context, MusicService.class));
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFY_ID);
    }
}