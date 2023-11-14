package com.example.spotifyalarm;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpotifyAPI {
    public interface PlaylistCallBack{
        void onSuccess(List<PlaylistModel> playlistModelList);
        void onError(String error);
    }

    private String TOKEN;

    List<PlaylistModel> playlistModelList;

    Context context;

    public SpotifyAPI(Context context, String token){
        this.context = context;
        this.TOKEN = token;
    }

    public void getUserPlaylist(PlaylistCallBack callback){
        RequestQueue reqQueue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.GET, context.getString(R.string.spotify_api_uri)+ "/me/playlists", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try{
                    JSONObject obj = new JSONObject(response);
                    JSONArray array = new JSONArray(obj.getString("items"));
                    playlistModelList = new ArrayList<>(array.length());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject playlist = array.getJSONObject(i);
                        JSONArray images = new JSONArray(playlist.getString("images"));
                        PlaylistModel playlistModel = new PlaylistModel(
                                i,
                                playlist.getString("name"),
                                playlist.getString("uri"),
                                ""
                                //images.getJSONObject(1).getString("url")
                        );
                        playlistModelList.add(playlistModel);
                    }

                    callback.onSuccess(playlistModelList);

                    Log.v("api", "onResponseValid : " + array);
                } catch(JSONException e){
                    e.printStackTrace();
                    Log.e("api", "onResponseError : " + e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onError(error.toString());
                Log.d("API Test", "onErrorResponse: "+ error);
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String auth = "Bearer " + TOKEN;
                headers.put("Accept", "application/json");
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", auth);
                return headers;
            }
        };
        reqQueue.add(request);
    }
}
