package dev.tangvdv.spotifyalarm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

public class SpotifyAuthHelper {

    private final String TAG = "SpotifyActivity";

    private final Context context;
    private final SpotifyAuthCallback callback;
    private String token;

    public interface SpotifyAuthCallback{
        void onSpotifyConnected(String token);
        void onSpotifyConnectionError(String error);
    }

    public SpotifyAuthHelper(Context context, SpotifyAuthCallback callback) {
        this.context = context;
        this.callback = callback;
    }

    public boolean isTokenValid(String token){
        return (token != null && !token.equals("") && System.currentTimeMillis() >= AlarmSharedPreferences.loadExpirationTimeToken(context));
    }

    public void startSpotifyActivity(Activity activity){
        token = AlarmSharedPreferences.loadToken(context);

        if(!isTokenValid(token)){
            AuthorizationRequest.Builder builder =
                    new AuthorizationRequest.Builder(context.getString(R.string.client_id), AuthorizationResponse.Type.TOKEN, context.getString(R.string.redirect_uri));

            builder.setScopes(context.getResources().getStringArray(R.array.scopes));
            builder.setShowDialog(true);
            AuthorizationRequest request = builder.build();

            AuthorizationClient.openLoginActivity(activity, context.getResources().getInteger(R.integer.request_code) ,request);
        }
        else{
            callback.onSpotifyConnected(token);
        }
    }

    public void handlerActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == context.getResources().getInteger(R.integer.request_code)) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthorizationResponse.Type.TOKEN) {
                token = response.getAccessToken();
                AlarmSharedPreferences.saveToken(context, token);
                Long expirationTime = System.currentTimeMillis() + (response.getExpiresIn() * 1000L);
                AlarmSharedPreferences.saveExpirationTimeToken(context, expirationTime);

                callback.onSpotifyConnected(token);

            }
            else{
                Log.e(TAG, "Response error : "+response.getError());
                callback.onSpotifyConnectionError(context.getResources().getString(R.string.spotify_activity_error));
            }
        }
    }
}
