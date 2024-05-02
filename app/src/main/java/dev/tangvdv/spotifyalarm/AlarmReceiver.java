package dev.tangvdv.spotifyalarm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Objects;

import dev.tangvdv.spotifyalarm.model.AlarmModel;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String NOTIFICATION_CHANNEL_ID = "notification.spotifyalarm";
    private static final int NOTIFY_ID = 42;

    @Override
    public void onReceive(Context context, Intent intent) {
        context.stopService(new Intent(context, AlarmManagerService.class));

        AlarmModel.getInstance().setAlarmModel(AlarmSharedPreferences.loadAlarm(context));
        AlarmModel.getInstance().setAlarmOff();

        createMusicNotification(context);

        AlarmWakeLock.acquireAlarmWakeLock(context);
        Intent serviceIntent = new Intent(context, MusicService.class);
        Intent screenOnIntent = new Intent(context, AlarmLockScreenActivity.class);
        screenOnIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try{
            context.startForegroundService(serviceIntent);
            context.startActivity(screenOnIntent);
        }
        catch (IllegalStateException illegalStateException){
            Log.e("AlarmReceiver", Objects.requireNonNull(illegalStateException.getMessage()));
        }
    }

    private void createMusicNotification(Context context){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if(notificationManager != null){
            Intent intent = new Intent(context, NotificationShutAlarmOffReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.app_logo)
                    .setOngoing(true)
                    .setContentTitle("Alarm is ringing ! Click to shut alarm off")
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
                    .setContentIntent(pendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .addAction(R.mipmap.app_logo, "Stop", pendingIntent);


            notificationManager.notify(NOTIFY_ID, notificationBuilder.build());
        }
    }
}