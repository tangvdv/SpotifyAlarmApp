package com.example.spotifyalarm.model;

import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.Calendar;
import java.util.HashMap;

public class AlarmModel {
    private static AlarmModel instance;
    private Calendar calendar;
    private int hour;
    private int minute;
    private String playlist_uri;
    public enum State {
        OFF,
        ON,
        RINGING
    }

    private State currentState;

    private SpotifyAppRemote spotifyAppRemote;

    private AlarmModel(){
        this.calendar = Calendar.getInstance();
        this.currentState = State.OFF;
    }

    public static synchronized AlarmModel getInstance(){
        if(instance == null){
            instance = new AlarmModel();
        }
        return instance;
    }

    public void setAlarmModel(HashMap<String, Object> alarm){
        hour = alarm.containsKey("hour") ? Integer.parseInt((String) alarm.get("hour")) : 10;
        minute = alarm.containsKey("minute") ? Integer.parseInt((String) alarm.get("minute")) : 0;
        playlist_uri = alarm.containsKey("playlistUri") ? (String) alarm.get("playlistUri") : "";
        currentState = alarm.containsKey("state") ? State.valueOf((String) alarm.get("state")) : State.OFF;
    }

    public Calendar getCalendar() { return calendar; }

    public void setCalendar(Calendar calendar) { this.calendar = calendar; }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public String getPlaylist_uri() {
        return playlist_uri;
    }

    public void setPlaylist_uri(String playlist_uri) {
        this.playlist_uri = playlist_uri;
    }

    public SpotifyAppRemote getSpotifyAppRemote() {
        return spotifyAppRemote;
    }

    public void setSpotifyAppRemote(SpotifyAppRemote spotifyAppRemote) {
        this.spotifyAppRemote = spotifyAppRemote;
    }

    public State getCurrentState() {
        return currentState;
    }

    public void setAlarmOff() {
        currentState = State.OFF;
    }

    public void setAlarmOn() {
        currentState = State.ON;
    }

    public void setAlarmRing() {
        currentState = State.RINGING;
    }

    public HashMap<String, Object> getAlarmModelContent(){
        HashMap<String, Object> AlarmMap = new HashMap<String, Object>();
        AlarmMap.put("hour",String.valueOf(this.hour));
        AlarmMap.put("minute",String.valueOf(this.minute));
        AlarmMap.put("playlistUri", this.playlist_uri );
        AlarmMap.put("state", String.valueOf(this.currentState));

        return AlarmMap;
    }
}
