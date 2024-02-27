package com.example.spotifyalarm;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

public class AlarmSharedPreferences {
    private static final String TAG = "AlarmSharedPreferences";
    private static SharedPreferences sharedPreferences;

    private static void loadSharedPreferences(Context context){
        sharedPreferences = context.getSharedPreferences("App", Context.MODE_PRIVATE);
    }

    private static void saveSharedPreferences(String tag, String data){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(tag, data);
        editor.apply();
    }

    public static HashMap<String, Object> loadSettings(Context context){
        if(sharedPreferences == null) loadSharedPreferences(context);
        String data = sharedPreferences.getString("settings", "");
        return JsonToHashMap(data);
    }

    public static void saveSettings(Context context, HashMap<String, Object> data){
        if(sharedPreferences == null) loadSharedPreferences(context);
        saveSharedPreferences("settings", HashToJsonString(data));
    }

    public static HashMap<String, Object> loadAlarm(Context context){
        if(sharedPreferences == null) loadSharedPreferences(context);
        String data = sharedPreferences.getString("alarm", "");
        return JsonToHashMap(data);
    }

    public static void saveAlarm(Context context, HashMap<String, Object> data){
        if(sharedPreferences == null) loadSharedPreferences(context);
        saveSharedPreferences("alarm", HashToJsonString(data));
    }

    public static HashMap<String, Object> loadMusic(Context context){
        if(sharedPreferences == null) loadSharedPreferences(context);
        String data = sharedPreferences.getString("music", "");
        return JsonToHashMap(data);
    }

    public static void saveMusic(Context context, HashMap<String, Object> data){
        if(sharedPreferences == null) loadSharedPreferences(context);
        saveSharedPreferences("music", HashToJsonString(data));
    }

    private static HashMap<String, Object> JsonToHashMap(String json){
        HashMap<String, Object> map = new HashMap<String, Object>();

        if(!Objects.equals(json, "")){
            try{
                JSONObject jsonObject = new JSONObject(json);
                Iterator<String> keys = jsonObject.keys();

                while(keys.hasNext()){
                    String key = keys.next();
                    map.put(key, jsonObject.get(key));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return map;
    }

    private static String HashToJsonString(HashMap<String, Object> map){
        JSONObject json = new JSONObject();
        map.forEach((key, value) -> {
            try {
                json.put(key, value);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });

        return json.toString();
    }
}
