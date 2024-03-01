package com.example.spotifyalarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.spotifyalarm.model.AlarmModel;

public class AlarmManagerService extends Service {
    private static final String TAG = "AlarmManagerService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(isNetworkConnected()){
            if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.OFF || AlarmModel.getInstance().getCurrentState() == AlarmModel.State.RINGING){
                setAlarm();
            }
        }
        else{
            onDestroy();
        }

        return START_STICKY;
    }

    private void createNotification(){
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "channel";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(chan);

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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setAlarm(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms();
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, AlarmModel.getInstance().getCalendar().getTimeInMillis(), pendingIntent);

        createNotification();

        AlarmModel.getInstance().setAlarmOn();

        Log.i(TAG, AlarmModel.getInstance().getAlarmModelContent().toString() );
    }

    private void errorUserToast(String text){
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new ForegroundColorSpan(Color.RED), 0, spannableString.length(), 0);
        Toast.makeText(getApplicationContext(), spannableString, Toast.LENGTH_LONG).show();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}