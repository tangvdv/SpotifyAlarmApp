package com.example.spotifyalarm;

import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class AlarmModel {
    private static AlarmModel instance;
    private Calendar calendar;
    private String playlist_uri;
    public enum State {
        OFF,
        ON,
        RINGING
    }

    private State currentState;

    private SpotifyAppRemote spotifyAppRemote;

    private AlarmModel(){
        this.currentState = State.OFF;
    }

    public static synchronized AlarmModel getInstance(){
        if(instance == null){
            instance = new AlarmModel();
        }
        return instance;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public void setCalendar(Calendar calendar) {
        this.calendar = calendar;
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

    public HashMap<String, String> getAlarmModelContent(){
        HashMap<String, String> AlarmMap = new HashMap<String, String>();
        AlarmMap.put("Calendar", new SimpleDateFormat("HH:mm:ss").format(this.calendar.getTime()));
        AlarmMap.put("PlaylistUri", this.playlist_uri );
        AlarmMap.put("State", String.valueOf(this.currentState));
        if(this.spotifyAppRemote != null)
            AlarmMap.put("SpotifyAppRemote", String.valueOf(this.spotifyAppRemote.getConnectApi()) );

        return AlarmMap;
    }
}
