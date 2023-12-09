package com.example.spotifyalarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

public class MusicService extends Service {
    SpotifyAppRemote mySpotifyAppRemote;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        onTaskRemoved(intent);

        Log.i("MusicService", "MusicService Started");
        startActivity();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("MusicService", "MusicService Destroyed");
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
                Log.e("MusicService", throwable.getMessage(), throwable);
            }
        });
    }

    private void play(){
        if(mySpotifyAppRemote != null){
            mySpotifyAppRemote.getPlayerApi().play(AlarmModel.getInstance().getPlaylist_uri());
            Log.i("MusicService", "Play");
        }
        else{
            Log.e("AlarmReceiver", "SpotifyPlayerApi object null");
        }

        AlarmModel.getInstance().setPendingIntent(null);
        stopService(new Intent(this, AlarmManagerService.class));
        this.stopSelf();
    }
}