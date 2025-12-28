package dev.tangvdv.spotifyalarm.helper;

import android.content.Context;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import dev.tangvdv.spotifyalarm.R;
import dev.tangvdv.spotifyalarm.SpotifyConstants;

public class SpotifyRemoteHelper {
    private static final String CLIENT_ID = SpotifyConstants.CLIENT_ID;
    private static final String REDIRECT_URI = SpotifyConstants.REDIRECT_URI;
    public interface SpotifyRemoteCallback{
        void onRemoteConnected(SpotifyAppRemote spotifyAppRemote);
        void onRemoteConnectionError(Throwable throwable);
    }

    public static void spotifyAppRemoteConnection(Context context, SpotifyRemoteCallback callback) {
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(context, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                AlarmSharedPreferences.saveAuthSpotify(context, true);
                if(callback != null) callback.onRemoteConnected(spotifyAppRemote);
            }

            @Override
            public void onFailure(Throwable throwable) {
                if (callback != null) callback.onRemoteConnectionError(throwable);
            }
        });
    }
}
