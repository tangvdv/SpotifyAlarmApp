package dev.tangvdv.spotifyalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

import dev.tangvdv.spotifyalarm.model.AlarmModel;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            context.stopService(new Intent(context, AlarmManagerService.class));

            AlarmModel.getInstance().setAlarmModel(AlarmSharedPreferences.loadAlarm(context));
            AlarmModel.getInstance().setAlarmOff();

            AlarmWakeLock.acquireAlarmWakeLock(context);

            Intent serviceIntent = new Intent(context, MusicService.class);
            context.startForegroundService(serviceIntent);
        } catch (Exception e) {
            LogFile logFile = new LogFile(context);
            logFile.writeToFile("AlarmReceiver", Objects.requireNonNull(e.getMessage()));
            Log.e("AlarmReceiver", Objects.requireNonNull(e.getMessage()));
        }
    }
}