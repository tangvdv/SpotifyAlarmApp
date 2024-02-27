package com.example.spotifyalarm;

import static java.lang.Thread.sleep;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import com.example.spotifyalarm.model.AlarmModel;
import com.example.spotifyalarm.model.SettingsModel;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.PlayerState;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Objects;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private SpotifyAppRemote mySpotifyAppRemote;

    private SettingsModel settingsModel;
    private boolean nextAlarm = false;
    private int stopAlarm;
    private boolean isPaused = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        onTaskRemoved(intent);

        AlarmModel.getInstance().setAlarmModel(AlarmSharedPreferences.loadAlarm(this));

        mySpotifyAppRemote = AlarmModel.getInstance().getSpotifyAppRemote();
        if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON) {
            play();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void play(){
        applySettings();
        String uri = AlarmModel.getInstance().getPlaylist_uri();
        if(mySpotifyAppRemote != null && !uri.equals("")){
            mySpotifyAppRemote.getConnectApi().connectSwitchToLocalDevice();
            mySpotifyAppRemote.getPlayerApi().play(uri, PlayerApi.StreamType.ALARM);
            Log.i(TAG, "Play");
            AlarmModel.getInstance().setAlarmRing();
            isPaused = false;

            if(stopAlarm != 0){
                stopAlarmTimeLeftThread();
            }
        }
        else{
            Log.e(TAG, "SpotifyPlayerApi object null");
        }

        if(settingsModel.isRepeat()) setNextAlarm();
        else{
            AlarmModel.getInstance().setAlarmOff();
            stopService(new Intent(this, AlarmManagerService.class));
        }

        this.stopSelf();
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
        Log.i(TAG, String.valueOf(settingsModel.getSettingsModelContent()));

        mySpotifyAppRemote.getPlayerApi().setShuffle(settingsModel.isShuffle());

        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(AudioManager.STREAM_ALARM, settingsModel.getVolume(), 0);

        int[] stopAlarmValues = getResources().getIntArray(R.array.stop_alarm_values);
        stopAlarm = stopAlarmValues[settingsModel.getStopAlarm()];
    }

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

    @Override
    public void onDestroy() {
        AlarmWakeLock.releaseAlarmWakeLock();
        super.onDestroy();
    }
}