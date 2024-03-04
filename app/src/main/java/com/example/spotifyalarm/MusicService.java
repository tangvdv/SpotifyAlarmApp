package com.example.spotifyalarm;

import static java.lang.Thread.sleep;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.example.spotifyalarm.model.AlarmModel;
import com.example.spotifyalarm.model.SettingsModel;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.PlayerState;

import java.util.Calendar;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private SpotifyAppRemote mySpotifyAppRemote;

    private LogFile logFile;

    private boolean hasSpotifyRemoteResponded;
    private boolean isBackupAlarmPlayed;
    private int remoteCheckSecondsLeft;

    private MediaPlayer mediaPlayer;

    private SettingsModel settingsModel;
    //private int stopAlarm;
    private boolean isPaused = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        onTaskRemoved(intent);
        logFile = new LogFile(this);
        hasSpotifyRemoteResponded = false;
        isBackupAlarmPlayed = false;
        AlarmModel.getInstance().setAlarmModel(AlarmSharedPreferences.loadAlarm(this));
        if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON) {
            setSpotifyAppRemote();
            spotifyRemoteCheckThread();
        }

        return START_STICKY;
    }

    private void setSpotifyAppRemote(){
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(this.getString(R.string.client_id))
                        .setRedirectUri(this.getString(R.string.redirect_uri))
                        .build();
        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                if(!isBackupAlarmPlayed){
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
                if(!isBackupAlarmPlayed){
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
        String uri = AlarmModel.getInstance().getPlaylist_uri();
        if(!uri.equals("")){
            applySettings();
            mySpotifyAppRemote.getPlayerApi().setShuffle(settingsModel.isShuffle());

            mySpotifyAppRemote.getConnectApi().connectSwitchToLocalDevice();
            mySpotifyAppRemote.getPlayerApi().play(uri, PlayerApi.StreamType.ALARM);
            Log.v(TAG, "SpotifyAlarmPlay");
            logFile.writeToFile(TAG, "SpotifyAlarmPlay");
            AlarmModel.getInstance().setAlarmRing();
            isPaused = false;

            /*
            if(stopAlarm != 0){
                stopAlarmTimeLeftThread();
            }
             */
        }
        else{
            Log.e(TAG, "SpotifyPlayerApi object null");
            playBackupAlarm();
        }

        alarmEnding();
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

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_ALARM, settingsModel.getVolume(), 0);

        /*
        int[] stopAlarmValues = getResources().getIntArray(R.array.stop_alarm_values);
        stopAlarm = stopAlarmValues[settingsModel.getStopAlarm()];
         */
    }

    /*
    private void stopAlarmTimeLeftThread(){
        Thread stopAlarmThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (stopAlarm > 0 && !isPaused){
                    try {
                        Log.i(TAG, "Alarm stop in : "+stopAlarm+" minute(s)");
                        sleep(60000);
                        stopAlarm -= 1;
                        mySpotifyAppRemote.getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
                            @Override
                            public void onResult(PlayerState playerState) {
                                isPaused = playerState.isPaused;
                            }
                        });
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                if(!isPaused){
                    mySpotifyAppRemote.getPlayerApi().pause();
                }
            }
        });

        stopAlarmThread.start();
    }
     */

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
                    isBackupAlarmPlayed = true;
                    playBackupAlarm();
                }
            }
        });
        spotifyRemoteCheck.start();
    }

    private void playBackupAlarm(){
        applySettings();

        mediaPlayer = MediaPlayer.create(this, R.raw.flowers_to_the_moon);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        Log.v(TAG, mediaPlayer.toString());
        mediaPlayer.setAudioAttributes(audioAttributes);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        });

        Log.v(TAG, "BackupAlarmPlay");
        logFile.writeToFile(TAG, "BackupAlarmPlay");
        alarmEnding();
    }

    private void alarmEnding(){
        stopService(new Intent(this, AlarmNotificationService.class));

        if(settingsModel.isRepeat()) setNextAlarm();
        else AlarmModel.getInstance().setAlarmOff();

        AlarmSharedPreferences.saveAlarm(this, AlarmModel.getInstance().getAlarmModelContent());
    }

    @Override
    public void onDestroy() {
        AlarmWakeLock.releaseAlarmWakeLock();
        super.onDestroy();
    }
}