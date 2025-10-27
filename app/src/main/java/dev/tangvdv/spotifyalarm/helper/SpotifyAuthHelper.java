package dev.tangvdv.spotifyalarm.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import dev.tangvdv.spotifyalarm.R;

public class SpotifyAuthHelper {
    private final String TAG = "SpotifyActivity";
    private final Context context;
    private final SpotifyAuthCallback callback;

    public interface SpotifyAuthCallback{
        void onSpotifyConnected();
        void onSpotifyConnectionError(String error);
    }

    public SpotifyAuthHelper(Context context, SpotifyAuthCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public void startSpotifyActivity(Activity activity){
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(context.getString(R.string.client_id), AuthorizationResponse.Type.CODE, context.getString(R.string.redirect_uri));

        builder.setScopes(context.getResources().getStringArray(R.array.scopes));
        builder.setShowDialog(true);
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(activity, context.getResources().getInteger(R.integer.request_code) ,request);
    }

    public void handlerActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == context.getResources().getInteger(R.integer.request_code)) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
            String code = response.getCode();

            if (response.getType() == AuthorizationResponse.Type.CODE) {
                AlarmSharedPreferences.saveCode(context, code);
                callback.onSpotifyConnected();
            }
            else{
                Log.e(TAG, "Response error : "+response.getError());
                callback.onSpotifyConnectionError(context.getResources().getString(R.string.spotify_activity_error));
            }
        }
    }
}
