package dev.tangvdv.spotifyalarm;

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

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import dev.tangvdv.spotifyalarm.model.AlarmModel;

public class AlarmManagerService extends Service {
    private static final String TAG = "AlarmManagerService";
    private static final String NOTIFICATION_CHANNEL_ID = "notification.spotifyalarm";
    private static final int NOTIFY_ID = 42;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        NotificationChannel serviceChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification SpotifyAlarm",NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(serviceChannel);

        startForeground(NOTIFY_ID, buildForegroundNotification());

        if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.OFF){
            setAlarm();
        }

        return START_STICKY;
    }

    private Notification createAlarmNotification(){
        String formattedHour = String.format("%02d", AlarmModel.getInstance().getHour());
        String formattedMinute = String.format("%02d", AlarmModel.getInstance().getMinute());

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.app_logo)
                .setOngoing(true)
                .setContentTitle("Alarm is running")
                .setContentText("Time : "+String.format("%s:%s", formattedHour, formattedMinute))
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setCategory(Notification.CATEGORY_ALARM);

        return notificationBuilder.build();
    }



    private Notification buildForegroundNotification(){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.app_logo)
                .setOngoing(true)
                .setContentTitle("Alarm is setting up");

        return notificationBuilder.build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setAlarm(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
         PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms();
        }

        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(AlarmModel.getInstance().getCalendar().getTimeInMillis(), pendingIntent), pendingIntent);

        AlarmModel.getInstance().setAlarmOn();
        AlarmSharedPreferences.saveAlarm(this, AlarmModel.getInstance().getAlarmModelContent());

        startForeground(NOTIFY_ID, createAlarmNotification());

        setResult();

        Log.i(TAG, AlarmModel.getInstance().getAlarmModelContent().toString() );
        LogFile logFile = new LogFile(this);
        logFile.writeToFile(TAG, AlarmModel.getInstance().getAlarmModelContent().toString());
    }

    private void setResult(){
        Intent intent = new Intent("intentAlarmKey");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}