package com.example.spotifyalarm;

import static java.lang.Thread.sleep;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

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

    private boolean nextAlarm = false;

    private int[] stopAlarmValues;
    private int stopAlarm;

    private boolean isPaused = true;

    private LogFile logFile;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        onTaskRemoved(intent);
        logFile = new LogFile(this);
        mySpotifyAppRemote = AlarmModel.getInstance().getSpotifyAppRemote();
        if(AlarmModel.getInstance().isState()) {
            logFile.writeToFile(TAG, "Service started, alarm is on");
            AlarmModel.getInstance().setState(false);
            play();
        }
        else{
            logFile.writeToFile(TAG, "Service started, alarm is off");
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
            logFile.writeToFile(TAG, "Alarm trigger");
            mySpotifyAppRemote.getConnectApi().connectSwitchToLocalDevice();
            mySpotifyAppRemote.getPlayerApi().play(uri, PlayerApi.StreamType.ALARM);
            Log.i(TAG, "Play");
            isPaused = false;

            if(stopAlarm != 0){
                stopAlarmTimeLeftThread();
            }
        }
        else{
            Log.e(TAG, "SpotifyPlayerApi object null");
            logFile.writeToFile(TAG, "SpotifyPlayerApi object null");
        }

        logFile.writeToFile(TAG, "Service stop");

        if(nextAlarm) setNextAlarm();
        else{
            AlarmModel.getInstance().setState(false);
            stopService(new Intent(this, AlarmManagerService.class));
        }

        this.stopSelf();
    }

    private void setNextAlarm(){
        Log.i(TAG, "Alarm repeat set");
        Intent alarmServiceIntent = new Intent(this, AlarmManagerService.class);

        Calendar calendar = AlarmModel.getInstance().getCalendar();
        calendar.add(Calendar.DATE, 1);
        AlarmModel.getInstance().setCalendar(calendar);

        startForegroundService(alarmServiceIntent);
    }

    private void applySettings(){
        try {
            logFile.writeToFile(TAG, "Apply settings");
            SharedPreferences sharedPreferences = this.getSharedPreferences("App", Context.MODE_PRIVATE);
            String data = sharedPreferences.getString("settings", "");
            if(!Objects.equals(data, "")){
                JSONObject jsonData = new JSONObject(data);
                Log.i(TAG, jsonData.toString());
                logFile.writeToFile(TAG, jsonData.toString());
                mySpotifyAppRemote.getPlayerApi().setShuffle(Boolean.parseBoolean(jsonData.getString("shuffle")));

                if(Boolean.parseBoolean(jsonData.getString("repeat"))){
                    nextAlarm = true;
                }

                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.setStreamVolume(AudioManager.STREAM_ALARM, jsonData.getInt("volume"), 0);

                stopAlarmValues = getResources().getIntArray(R.array.stop_alarm_values);
                stopAlarm = stopAlarmValues[jsonData.getInt("stopAlarm")];
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
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
                                Log.i(TAG, "On Player Api Callback Result, is paused : "+ playerState.isPaused);
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
                Log.i(TAG, "Alarm over");
            }
        });

        stopAlarmThread.start();
    }
}