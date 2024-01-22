package com.example.spotifyalarm;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpotifyAPI {
    private static final String TAG = "SpotifyAPI";
    private final String TOKEN;
    Context context;

    public interface SpotifyAPICallback{
        void onSuccess(List<MusicModel> musicModelList);
        void onError(String error);
    }

    public SpotifyAPI(Context context, String token){
        this.context = context;
        this.TOKEN = token;
    }

    public void getUserPlaylists(SpotifyAPICallback callback){
        RequestQueue reqQueue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.GET, context.getString(R.string.spotify_api_uri)+ "/me/playlists?limit=50", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try{
                    JSONObject obj = new JSONObject(response);
                    JSONArray array = new JSONArray(obj.getString("items"));
                    List<MusicModel> musicModelList = new ArrayList<>(array.length());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject playlist = array.getJSONObject(i);
                        JSONObject owner = playlist.getJSONObject("owner");
                        JSONObject images = new JSONArray(playlist.getString("images")).getJSONObject(0);
                        MusicModel musicModel = new MusicModel(
                                playlist.getString("id"),
                                playlist.getString("name"),
                                playlist.getString("uri"),
                                images.getString("url"),
                                new String[] {owner.getString("display_name")},
                                "playlist"
                        );
                        musicModelList.add(musicModel);
                    }

                    callback.onSuccess(musicModelList);

                    Log.v(TAG, "onResponseValid : " + array);
                } catch(JSONException e){
                    e.printStackTrace();
                    callback.onError(e.toString());
                    Log.e(TAG, "onResponseError : " + e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onError(error.toString());
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

    public void getUserAlbums(SpotifyAPICallback callback){
        RequestQueue reqQueue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.GET, context.getString(R.string.spotify_api_uri)+ "/me/albums?limit=50", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try{
                    JSONObject obj = new JSONObject(response);
                    JSONArray array = new JSONArray(obj.getString("items"));
                    List<MusicModel> musicModelList = new ArrayList<>(array.length());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject album = array.getJSONObject(i).getJSONObject("album");
                        JSONArray artist = album.getJSONArray("artists");
                        String[] artists_name = new String[artist.length()];
                        for(int j = 0; j < artist.length(); j++){
                            artists_name[j] = artist.getJSONObject(j).getString("name");
                        }

                        JSONObject images = new JSONArray(album.getString("images")).getJSONObject(0);
                        MusicModel musicModel = new MusicModel(
                                album.getString("id"),
                                album.getString("name"),
                                album.getString("uri"),
                                images.getString("url"),
                                artists_name,
                                "album"
                        );
                        musicModelList.add(musicModel);
                    }

                    callback.onSuccess(musicModelList);

                    Log.v(TAG, "onResponseValid : " + array);
                } catch(JSONException e){
                    e.printStackTrace();
                    callback.onError(e.toString());
                    Log.e(TAG, "onResponseError : " + e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onError(error.toString());
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

    public void getUserArtists(SpotifyAPICallback callback){
        RequestQueue reqQueue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.GET, context.getString(R.string.spotify_api_uri)+ "/me/following?type=artist&limit=50", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try{
                    JSONObject obj = new JSONObject(response);
                    JSONArray array = new JSONArray(new JSONObject(obj.getString("artists")).getString("items"));
                    List<MusicModel> musicModelList = new ArrayList<>(array.length());
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject artist = array.getJSONObject(i);

                        JSONObject images = new JSONArray(artist.getString("images")).getJSONObject(0);
                        MusicModel musicModel = new MusicModel(
                                artist.getString("id"),
                                artist.getString("name"),
                                artist.getString("uri"),
                                images.getString("url"),
                                new String[0],
                                "artist"
                        );
                        musicModelList.add(musicModel);
                    }

                    callback.onSuccess(musicModelList);

                    Log.v(TAG, "onResponseValid : " + array);
                } catch(JSONException e){
                    e.printStackTrace();
                    callback.onError(e.toString());
                    Log.e(TAG, "onResponseError : " + e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callback.onError(error.toString());
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
