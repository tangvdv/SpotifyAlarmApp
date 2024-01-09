package com.example.spotifyalarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AlarmManagerService extends Service {
    private static final String TAG = "AlarmManagerService";
    PendingIntent pendingIntent;
    AlarmManager alarmManager;
    Context context;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        context = this;

        Log.i(TAG, "Service Started");
        setAlarm();

        return START_STICKY;
    }

    private void createNotification(){
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "channel";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(chan);

        long notificationId = System.currentTimeMillis();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Alarm is running")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        startForeground((int) notificationId, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setAlarm(){
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmModel.getInstance().setPendingIntent(pendingIntent);

        Calendar calendar = AlarmModel.getInstance().getCalendar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms();
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        createNotification();

        Log.i(TAG, "Set alarm : " +
                new SimpleDateFormat("HH:mm:ss").format(calendar.getTime()) + " ; " + new SimpleDateFormat("HH:mm:ss:SSS").format(Calendar.getInstance().getTime().getTime()));

        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();

        AlarmModel.getInstance().setState(true);
    }

    public void cancelAlarm(){
        if(pendingIntent != null) {
            if (alarmManager == null) {
                alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            }
            alarmManager.cancel(pendingIntent);
            AlarmModel.getInstance().setPendingIntent(null);
            AlarmModel.getInstance().setState(false);
            Toast.makeText(this, "Alarm Canceled", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        if(AlarmModel.getInstance().isState()){
            startService(new Intent(this, AlarmManagerService.class));
        }
        else{
            cancelAlarm();
            super.onDestroy();
        }
    }
}