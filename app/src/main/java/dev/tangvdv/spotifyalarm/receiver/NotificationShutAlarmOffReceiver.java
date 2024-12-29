package dev.tangvdv.spotifyalarm.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;

import com.spotify.android.appremote.api.SpotifyAppRemote;

import dev.tangvdv.spotifyalarm.model.AlarmModel;

public class NotificationShutAlarmOffReceiver extends BroadcastReceiver {
    private static final int NOTIFY_ID = 42;

    @Override
    public void onReceive(Context context, Intent intent) {
        Ringtone defaultRingtone = AlarmModel.getInstance().getBackupAlarmRingtone();
        SpotifyAppRemote spotifyAppRemote = AlarmModel.getInstance().getSpotifyAppRemote();
        if(defaultRingtone != null) defaultRingtone.stop();
        if(spotifyAppRemote != null) spotifyAppRemote.getPlayerApi().pause();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFY_ID);
    }
}