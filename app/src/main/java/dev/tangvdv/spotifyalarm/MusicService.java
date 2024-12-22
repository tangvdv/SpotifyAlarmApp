package dev.tangvdv.spotifyalarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import dev.tangvdv.spotifyalarm.model.AlarmModel;
import dev.tangvdv.spotifyalarm.model.SettingsModel;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.Objects;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private static final String NOTIFICATION_CHANNEL_ID = "notification.spotifyalarm";
    private static final int NOTIFY_ID = 42;
    private SpotifyAppRemote mySpotifyAppRemote;

    private LogFile logFile;

    private boolean isAlarmRinging;

    private SettingsModel settingsModel;

    private Context context;

    private AlarmState alarmState;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        startForeground(NOTIFY_ID, buildForegroundNotification());

        try{
            context = this;

            alarmState = new AlarmState();

            NotificationChannel serviceChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification SpotifyAlarm",NotificationManager.IMPORTANCE_HIGH);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);


            settingsModel = new SettingsModel(AlarmSharedPreferences.loadSettings(this));

            logFile = new LogFile(this);
            isAlarmRinging = false;
            if(isNetworkConnected()){
                setSpotifyAppRemote();
            }
            else{
                playBackupAlarm();
            }
        }
        catch(Exception e){
            logFile = new LogFile(context);
            logFile.writeToFile(TAG, Objects.requireNonNull(e.getMessage()));
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private Notification buildForegroundNotification(){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.app_logo)
                .setOngoing(true)
                .setContentTitle("Alarm is setting up");

        return notificationBuilder.build();
    }

    private void setSpotifyAppRemote() {
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(this.getString(R.string.client_id))
                        .setRedirectUri(this.getString(R.string.redirect_uri))
                        .build();
        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                if (!isAlarmRinging) {
                    logFile.writeToFile(TAG, "SpotifyAppRemote on connected");
                    mySpotifyAppRemote = spotifyAppRemote;
                    AlarmModel.getInstance().setSpotifyAppRemote(spotifyAppRemote);
                    playSpotifyAlarm();
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                if (!isAlarmRinging) {
                    logFile.writeToFile(TAG, throwable.getMessage());
                    Log.e(TAG, throwable.getMessage(), throwable);
                    playBackupAlarm();
                }
            }
        });
    }

    private void playSpotifyAlarm() {
        String uri = AlarmModel.getInstance().getPlaylist_uri();
        if (!uri.equals("")) {
            // APPLY SETTINGS
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setStreamVolume(AudioManager.STREAM_ALARM, settingsModel.getVolume(), 0);
            mySpotifyAppRemote.getPlayerApi().setShuffle(settingsModel.isShuffle());
            mySpotifyAppRemote.getConnectApi().connectSwitchToLocalDevice();
            mySpotifyAppRemote.getPlayerApi().play(uri, PlayerApi.StreamType.ALARM);

            AlarmModel.getInstance().setAlarmSpotifyType();

            alarmState.getAlarmState(AlarmState.State.PLAY, new AlarmState.AlarmStateCallback() {
                @Override
                public void onCompletion(boolean isPlaying) {
                    if(isPlaying){
                        alarmEnding();
                    }
                }
            });

        } else {
            Log.e(TAG, "Playlist uri not found");
            logFile.writeToFile(TAG, "Playlist uri not found");
            playBackupAlarm();
        }
    }

    private void playBackupAlarm(){
        Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_ALARM);
        Ringtone defaultRingtone = RingtoneManager.getRingtone(this, defaultRingtoneUri);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        defaultRingtone.setAudioAttributes(audioAttributes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            defaultRingtone.setLooping(settingsModel.isLooping());
            defaultRingtone.setVolume(settingsModel.getVolume());
        }
        defaultRingtone.play();
        AlarmModel.getInstance().setBackupAlarmRingtone(defaultRingtone);
        AlarmModel.getInstance().setAlarmBackupType();
        alarmState.getAlarmState(AlarmState.State.PLAY, new AlarmState.AlarmStateCallback() {
            @Override
            public void onCompletion(boolean isPlaying) {
                if(isPlaying){
                    Log.v(TAG, "BackupAlarmPlay");
                    logFile.writeToFile(TAG, "BackupAlarmPlay");
                    alarmEnding();
                }
            }
        });
    }

    private void shutAlarmOffHandler(){
        alarmState.getAlarmState(AlarmState.State.PAUSE, new AlarmState.AlarmStateCallback() {
            @Override
            public void onCompletion(boolean isPlaying) {
                if(!isPlaying){
                    if(AlarmLockScreenActivity.lockScreenActivity != null) {
                        AlarmLockScreenActivity.lockScreenActivity.finish();
                    }
                    else{
                        Intent intent = new Intent(getApplicationContext(), NotificationShutAlarmOffReceiver.class);
                        sendBroadcast(intent);
                    }
                }
            }
        });
    }

    private void alarmEnding(){
        isAlarmRinging = true;
        stopForeground(STOP_FOREGROUND_REMOVE);

        try{
            Intent lockScreen = new Intent(context, AlarmLockScreenActivity.class);
            lockScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(lockScreen);

            createMusicNotification();
            shutAlarmOffHandler();
        }
        catch (IllegalStateException illegalStateException){
            logFile.writeToFile(TAG, Objects.requireNonNull(illegalStateException.getMessage()));
            Log.e(TAG, Objects.requireNonNull(illegalStateException.getMessage()));
        }

        if(settingsModel.isRepeat()) setNextAlarm();
        AlarmSharedPreferences.saveAlarm(this, AlarmModel.getInstance().getAlarmModelContent());
    }

    private void createMusicNotification(){
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

    private void setNextAlarm(){
        Intent alarmServiceIntent = new Intent(this, AlarmManagerService.class);
        startForegroundService(alarmServiceIntent);
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    @Override
    public void onDestroy() {
        AlarmWakeLock.releaseAlarmWakeLock(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}