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

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class AlarmManagerService extends Service {
    private static final String TAG = "AlarmManagerService";
    PendingIntent pendingIntent;
    AlarmManager alarmManager;

    LogFile logFile;
    Context context;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        context = this;

        logFile = new LogFile(this);

        Log.i(TAG, "Service Started");
        logFile.writeToFile(TAG, "Service Started");
        if(!AlarmModel.getInstance().isState()){
            Log.i(TAG, "Alarm setter begin");
            logFile.writeToFile(TAG, "Alarm setter begin");
            if(AlarmModel.getInstance().getSpotifyAppRemote() == null || !AlarmModel.getInstance().getSpotifyAppRemote().isConnected())
                setSpotifyAppRemote();
            else
                setAlarm();
        }
        else{
            Log.i(TAG, "Alarm already set");
            logFile.writeToFile(TAG, "Alarm already set");
        }

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

    private void setSpotifyAppRemote(){
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(this.getString(R.string.client_id))
                        .setRedirectUri(this.getString(R.string.redirect_uri))
                        .showAuthView(true)
                        .build();
        Log.i(TAG, "setSpotifyAppRemote");
        logFile.writeToFile(TAG, "setSpotifyAppRemote");
        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                Log.i(TAG, "SpotifyAppRemote on connected");
                logFile.writeToFile(TAG, "SpotifyAppRemote on connected");
                AlarmModel.getInstance().setSpotifyAppRemote(spotifyAppRemote);
                setAlarm();
            }
            @Override
            public void onFailure(Throwable throwable) {
                Log.e(TAG, throwable.getMessage(), throwable);
                logFile.writeToFile(TAG, "SpotifyAppRemote on failure : "+throwable.getMessage());
                if(AlarmModel.getInstance().isState()){
                    setSpotifyAppRemote();
                }
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setAlarm(){
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = AlarmModel.getInstance().getCalendar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms();
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        createNotification();

        Log.i(TAG, "Set alarm : " +
                new SimpleDateFormat("HH:mm:ss").format(calendar.getTime()) + " ; " + new SimpleDateFormat("HH:mm:ss:SSS").format(Calendar.getInstance().getTime().getTime()));

        Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();

        AlarmModel.getInstance().setState(true);

        Log.i(TAG, AlarmModel.getInstance().getAlarmModelContent().toString() );
        logFile.writeToFile(TAG, AlarmModel.getInstance().getAlarmModelContent().toString() );
    }

    public void cancelAlarm(){
        if (alarmManager == null) {
            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        }
        AlarmModel.getInstance().setState(false);
        Toast.makeText(this, "Alarm Canceled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service stopped");
        logFile.writeToFile(TAG, "Service stopped");
        if(AlarmModel.getInstance().isState()){
            cancelAlarm();
        }
        super.onDestroy();
    }
}