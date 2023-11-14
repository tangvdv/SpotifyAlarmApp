package com.example.spotifyalarm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

public class AlarmReceiver extends BroadcastReceiver {
    SpotifyAppRemote mySpotifyAppRemote;
    String playlist_uri;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                "Settings", Context.MODE_PRIVATE
        );
        playlist_uri = sharedPreferences.getString("playlist_uri", "");
        String t = context.getString(R.string.app_name);
        startActivity(context);
    }

    private void startActivity(Context context){
        Log.v("AlarmReceiver", "StartActivity");
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(context.getString(R.string.client_id))
                        .setRedirectUri(context.getString(R.string.redirect_uri))
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(context, connectionParams, new Connector.ConnectionListener() {
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
    }
}
