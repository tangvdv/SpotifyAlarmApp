package com.example.spotifyalarm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.spotifyalarm.model.AlarmModel;

public class AlarmNotificationService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotification();
        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotification(){
        String NOTIFICATION_CHANNEL_ID = "example.permanence";

        String formattedHour = String.format("%02d", AlarmModel.getInstance().getHour());
        String formattedMinute = String.format("%02d", AlarmModel.getInstance().getMinute());

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder
                .setSmallIcon(R.mipmap.app_logo)
                .setOngoing(true)
                .setContentTitle("Alarm is running")
                .setContentText("Time : "+String.format("%s:%s", formattedHour, formattedMinute))
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_ALARM)
                .build();

        startForeground(42, notification);
    }

    private void cancelNotification(){
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    @Override
    public void onDestroy() {
        cancelNotification();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}