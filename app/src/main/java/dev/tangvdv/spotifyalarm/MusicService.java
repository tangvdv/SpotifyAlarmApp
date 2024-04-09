package dev.tangvdv.spotifyalarm;

import static java.lang.Thread.sleep;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.Empty;
import com.spotify.protocol.types.PlayerState;

import java.util.Calendar;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private static final String NOTIFICATION_CHANNEL_ID = "notification.spotifyalarm";
    private static final int NOTIFY_ID = 51;
    private SpotifyAppRemote mySpotifyAppRemote;

    private LogFile logFile;

    private boolean hasSpotifyRemoteResponded;
    private boolean isBackupAlarmPlayed;
    private int remoteCheckSecondsLeft;

    private Ringtone defaultRingtone;

    private SettingsModel settingsModel;
    private boolean isPaused = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        onTaskRemoved(intent);

        NotificationChannel serviceChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification SpotifyAlarm",NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(serviceChannel);

        startForeground(NOTIFY_ID, buildForegroundNotification());

        logFile = new LogFile(this);
        hasSpotifyRemoteResponded = false;
        isBackupAlarmPlayed = false;
        if(isNetworkConnected()){
            setSpotifyAppRemote();
            spotifyRemoteCheckThread();
        }
        else{
            playBackupAlarm();
        }

        return START_STICKY;
    }

    private Notification buildForegroundNotification(){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.app_logo)
                .setOngoing(true)
                .setContentTitle("Music is setting up");

        return notificationBuilder.build();
    }

    private void setSpotifyAppRemote(){
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(this.getString(R.string.client_id))
                        .setRedirectUri(this.getString(R.string.redirect_uri))
                        .build();
        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                if(!isBackupAlarmPlayed && !hasSpotifyRemoteResponded){
                    logFile.writeToFile(TAG, "SpotifyAppRemote on connected");
                    mySpotifyAppRemote = spotifyAppRemote;
                    playSpotifyAlarm();
                    hasSpotifyRemoteResponded = true;
                }
            }
            @Override
            public void onFailure(Throwable throwable) {
                logFile.writeToFile(TAG, throwable.getMessage());
                Log.e(TAG, throwable.getMessage(), throwable);
                if(!isBackupAlarmPlayed && !hasSpotifyRemoteResponded){
                    hasSpotifyRemoteResponded = true;
                    playBackupAlarm();
                }
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void playSpotifyAlarm(){
        applySettings();
        String uri = AlarmModel.getInstance().getPlaylist_uri();
        if(!uri.equals("")){
            mySpotifyAppRemote.getPlayerApi().setShuffle(settingsModel.isShuffle());
            mySpotifyAppRemote.getConnectApi().connectSwitchToLocalDevice();
            mySpotifyAppRemote.getPlayerApi().play(uri, PlayerApi.StreamType.ALARM).setResultCallback(new CallResult.ResultCallback<Empty>() {
                @Override
                public void onResult(Empty empty) {
                    mySpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
                        @Override
                        public void onResult(PlayerState playerState) {
                            isPaused = playerState.isPaused;
                            Log.v(TAG, "SpotifyAlarmPlay");
                            logFile.writeToFile(TAG, "SpotifyAlarmPlay");

                            if(isPaused){
                                playBackupAlarm();
                            }
                            else{
                                alarmEnding();
                            }
                        }
                    });
                }
            });
        }
        else {
            Log.e(TAG, "Playlist uri not found");
            logFile.writeToFile(TAG, "Playlist uri not found");
            playBackupAlarm();
        }
    }

    private void setNextAlarm(){
        Calendar calendar = AlarmModel.getInstance().getCalendar();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        AlarmModel.getInstance().setCalendar(calendar);

        Intent alarmServiceIntent = new Intent(this, AlarmManagerService.class);
        startForegroundService(alarmServiceIntent);
    }

    private void applySettings(){
        settingsModel = new SettingsModel(AlarmSharedPreferences.loadSettings(this));
        Log.v(TAG, String.valueOf(settingsModel.getSettingsModelContent()));
        logFile.writeToFile(TAG, String.valueOf(settingsModel.getSettingsModelContent()));

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_ALARM, settingsModel.getVolume(), 0);
    }

    private void spotifyRemoteCheckThread(){
        remoteCheckSecondsLeft = 60;
        Thread spotifyRemoteCheck = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!hasSpotifyRemoteResponded && remoteCheckSecondsLeft > 0){
                    try {
                        sleep(1000);
                        remoteCheckSecondsLeft -= 1;
                        Log.v(TAG, remoteCheckSecondsLeft + "seconds left until alarm backup plays");
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                if(!hasSpotifyRemoteResponded && remoteCheckSecondsLeft <= 0){
                    playBackupAlarm();
                }
            }
        });
        spotifyRemoteCheck.start();
    }

    private void playBackupAlarm(){
        applySettings();

        Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_ALARM);
        defaultRingtone = RingtoneManager.getRingtone(this, defaultRingtoneUri);

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
        isPaused = false;
        isBackupAlarmPlayed = true;

        Log.v(TAG, "BackupAlarmPlay");
        logFile.writeToFile(TAG, "BackupAlarmPlay");
        alarmEnding();
    }

    private void alarmEnding(){
        stopForeground(STOP_FOREGROUND_REMOVE);
        if(settingsModel.isRepeat()) setNextAlarm();
        AlarmSharedPreferences.saveAlarm(this, AlarmModel.getInstance().getAlarmModelContent());
        checkMusicState();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void checkMusicState(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isPaused){
                    try {
                        sleep(1000);
                        if(isBackupAlarmPlayed){
                            if(!defaultRingtone.isPlaying()) isPaused = true;
                        }else{
                            mySpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
                                @Override
                                public void onResult(PlayerState playerState) {
                                    if(playerState.isPaused) isPaused = true;
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                stopSelf();
            }
        });
        thread.start();
    }

    @Override
    public void onDestroy() {
        AlarmWakeLock.releaseAlarmWakeLock(this);
        super.onDestroy();
    }
}