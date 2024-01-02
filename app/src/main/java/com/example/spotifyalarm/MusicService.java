package com.example.spotifyalarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class MusicService extends Service {
    private static final String TAG = "MusicService";

    SpotifyAppRemote mySpotifyAppRemote;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        onTaskRemoved(intent);

        startActivity();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startActivity(){
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(this.getString(R.string.client_id))
                        .setRedirectUri(this.getString(R.string.redirect_uri))
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                mySpotifyAppRemote = spotifyAppRemote;
                play();
            }
            @Override
            public void onFailure(Throwable throwable) {
                Log.e(TAG, throwable.getMessage(), throwable);
            }
        });
    }

    private void play(){
        setSettings();
        String uri = AlarmModel.getInstance().getPlaylist_uri();
        if(mySpotifyAppRemote != null && !uri.equals("")){
            mySpotifyAppRemote.getPlayerApi().play(uri, PlayerApi.StreamType.ALARM);
            Log.i(TAG, "Play");
        }
        else{
            Log.e(TAG, "SpotifyPlayerApi object null");
        }

        AlarmModel.getInstance().setPendingIntent(null);
        stopService(new Intent(this, AlarmManagerService.class));
        this.stopSelf();
    }

    private void setSettings(){
        try {
            SharedPreferences sharedPreferences = this.getSharedPreferences("App", Context.MODE_PRIVATE);
            String data = sharedPreferences.getString("settings", "");
            if(!Objects.equals(data, "")){
                JSONObject jsonData = new JSONObject(data);

                mySpotifyAppRemote.getPlayerApi().setShuffle(Boolean.parseBoolean(jsonData.getString("shuffle")));

                if(Boolean.parseBoolean(jsonData.getString("repeat"))){
                    Intent alarmServiceIntent = new Intent(this, AlarmManagerService.class);
                    startForegroundService(alarmServiceIntent);
                };

                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.setStreamVolume(AudioManager.STREAM_MUSIC, jsonData.getInt("volume"), 0);

                mySpotifyAppRemote.getConnectApi().connectSwitchToLocalDevice();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}