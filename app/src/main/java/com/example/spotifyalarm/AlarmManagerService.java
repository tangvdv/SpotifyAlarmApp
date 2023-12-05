package com.example.spotifyalarm;

import static android.content.Intent.getIntent;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AlarmManagerService extends Service {
    PendingIntent pendingIntent;

    Calendar calendar;
    AlarmManager alarmManager;

    Context context;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        context = this;
        Log.i("AlarmManagerService", "Service Started");
        if(calendar == null) calendar = (Calendar) intent.getExtras().getSerializable("calendar");
        setAlarm();
        return START_STICKY;
    }

    private void setNotification(){
        String CHANNEL_ID = "example.permanence";

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Your Foreground Service")
                .setContentText("Service is running in the foreground")
                .build();

        startForeground(1, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setAlarm(){
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        setNotification();

        Log.i("AlarmManagerService", "Set alarm : " +
                new SimpleDateFormat("HH:mm:ss").format(calendar.getTime()) + " ; " + new SimpleDateFormat("HH:mm:ss:SSS").format(Calendar.getInstance().getTime().getTime()));

        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
    }

    public void cancelAlarm(){
        if(pendingIntent != null){
            if(alarmManager == null){
                alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            }
            alarmManager.cancel(pendingIntent);
            Toast.makeText(this, "Alarm Canceled", Toast.LENGTH_SHORT).show();
        }
        super.onDestroy();
    }

    @Override
    public void onDestroy() {
        Log.i("AlarmManagerService", "Service Destroyed");
        cancelAlarm();
    }
}