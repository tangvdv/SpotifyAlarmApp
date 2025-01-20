package dev.tangvdv.spotifyalarm.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import dev.tangvdv.spotifyalarm.helper.AlarmHelper;

public class NotificationShutAlarmOffReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmHelper.getInstance(context).shutAlarmOff();
    }
}