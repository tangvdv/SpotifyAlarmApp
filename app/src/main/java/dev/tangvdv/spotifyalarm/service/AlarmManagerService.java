package dev.tangvdv.spotifyalarm.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import dev.tangvdv.spotifyalarm.R;
import dev.tangvdv.spotifyalarm.helper.AlarmHelper;
import dev.tangvdv.spotifyalarm.helper.NotificationHelper;
import dev.tangvdv.spotifyalarm.model.AlarmModel;

public class AlarmManagerService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(R.integer.notification_id, NotificationHelper.getForegroundNotification(this, "Alarm is setting up"));


        if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.OFF){
            AlarmHelper alarmHelper = new AlarmHelper(this);
            alarmHelper.setAlarm();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}