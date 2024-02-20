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
import android.os.PowerManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.Calendar;

public class AlarmManagerService extends Service {
    private static final String TAG = "AlarmManagerService";
    private int spotifyConnectionTryAmount = 5;
    PowerManager.WakeLock wakeLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(isNetworkConnected()){
            if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.OFF || AlarmModel.getInstance().getCurrentState() == AlarmModel.State.RINGING){
                if(AlarmModel.getInstance().getSpotifyAppRemote() == null || !AlarmModel.getInstance().getSpotifyAppRemote().isConnected()) {
                    setSpotifyAppRemote();
                }
                else
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
        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                Log.i(TAG, "SpotifyAppRemote on connected");
                AlarmModel.getInstance().setSpotifyAppRemote(spotifyAppRemote);
                spotifyConnectionTryAmount = 5;
                if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.OFF){
                    setAlarm();
                }
            }
            @Override
            public void onFailure(Throwable throwable) {
                Log.e(TAG, throwable.getMessage(), throwable);
                if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON){
                    if(spotifyConnectionTryAmount > 0){
                        setSpotifyAppRemote();
                        spotifyConnectionTryAmount--;
                    }
                    else{
                        errorUserToast("Error : Too many attempts connecting to spotify app remote");
                        onDestroy();
                    }
                }
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setAlarm(){
        long diffInMillies = Math.abs(Calendar.getInstance().getTimeInMillis() - AlarmModel.getInstance().getCalendar().getTimeInMillis());

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpotifyAlarm::AlarmWakeLock");
        wakeLock.acquire(diffInMillies + 60000);
        
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = AlarmModel.getInstance().getCalendar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms();
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

        createNotification();

        AlarmModel.getInstance().setAlarmOn();

        Log.i(TAG, AlarmModel.getInstance().getAlarmModelContent().toString() );
    }

    @Override
    public void onDestroy() {
        if(AlarmModel.getInstance().getCurrentState() != AlarmModel.State.OFF){
            if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON){
                wakeLock.release();
            }
            AlarmModel.getInstance().setAlarmOff();
        }

        super.onDestroy();
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