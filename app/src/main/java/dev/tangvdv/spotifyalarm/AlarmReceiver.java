package dev.tangvdv.spotifyalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmWakeLock.acquireAlarmWakeLock(context);
        context.stopService(new Intent(context, AlarmManagerService.class));
        Intent serviceIntent = new Intent(context, MusicService.class);
        context.startForegroundService(serviceIntent);
    }
}