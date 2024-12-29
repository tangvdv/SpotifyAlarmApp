package dev.tangvdv.spotifyalarm.helper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import dev.tangvdv.spotifyalarm.R;

public class NotificationHelper {
    private static final String NOTIFICATION_CHANNEL_ID = "notification.spotifyalarm";

    public static NotificationManager getNotificationManager(Context context){
        NotificationChannel serviceChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification SpotifyAlarm", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(serviceChannel);

        return notificationManager;
    }

    public static Notification getNotification(Context context, String title, String content, PendingIntent pendingIntent){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.app_logo)
                .setOngoing(true)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if(pendingIntent != null) notificationBuilder.setContentIntent(pendingIntent);

        return notificationBuilder.build();
    }

    public static Notification getForegroundNotification(Context context, String title){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.app_logo)
                .setOngoing(true)
                .setContentTitle(title);

        return notificationBuilder.build();
    }
}
