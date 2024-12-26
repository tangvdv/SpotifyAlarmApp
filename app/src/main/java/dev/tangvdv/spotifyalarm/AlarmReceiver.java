package dev.tangvdv.spotifyalarm;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import dev.tangvdv.spotifyalarm.model.AlarmModel;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String NOTIFICATION_CHANNEL_ID = "notification.spotifyalarm";
    private static final int NOTIFY_ID = 42;

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmWakeLock.acquireAlarmWakeLock(context);

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFY_ID, buildNotification(context));

        AlarmModel.getInstance().setAlarmModel(AlarmSharedPreferences.loadAlarm(context));
        AlarmModel.getInstance().setAlarmOff();

        Intent lockScreen = new Intent(context, AlarmLockScreenActivity.class);
        lockScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(lockScreen);
    }

    private Notification buildNotification(Context context){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.app_logo)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setOngoing(true)
                .setContentTitle("Alarm is setting up");

        return notificationBuilder.build();
    }
}