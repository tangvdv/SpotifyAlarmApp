package dev.tangvdv.spotifyalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationShutAlarmOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.stopService(new Intent(context, MusicService.class));
    }
}