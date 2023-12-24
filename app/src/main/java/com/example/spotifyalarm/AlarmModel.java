package com.example.spotifyalarm;

import android.app.PendingIntent;
import android.provider.CalendarContract;

import java.util.Calendar;

public class AlarmModel {
    private static AlarmModel instance;
    private PendingIntent pendingIntent;
    private Calendar calendar;
    private String playlist_uri;
    private boolean state;

    private AlarmModel(){}

    public static synchronized AlarmModel getInstance(){
        if(instance == null){
            instance = new AlarmModel();
        }
        return instance;
    }

    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
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

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }
}
