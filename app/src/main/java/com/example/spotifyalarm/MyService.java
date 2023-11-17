package com.example.spotifyalarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

public class MyService extends Service {
    SpotifyAppRemote mySpotifyAppRemote;
    String playlist_uri;

    public MyService() {
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        onTaskRemoved(intent);

        //Toast.makeText(getApplicationContext(),"Alarm triggered", Toast.LENGTH_SHORT).show();

        SharedPreferences sharedPreferences = getSharedPreferences(
               "Settings", Context.MODE_PRIVATE
        );
        playlist_uri = sharedPreferences.getString("playlist_uri", "");

        startActivity();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
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
                Log.e("AlarmReceiver", throwable.getMessage(), throwable);
            }
        });
    }

    private void play(){
        if(mySpotifyAppRemote != null){
            mySpotifyAppRemote.getPlayerApi().play(playlist_uri);
        }
        else{
            Log.e("AlarmReceiver", "SpotifyPlayerApi object null");
        }

        this.stopSelf();
    }
}