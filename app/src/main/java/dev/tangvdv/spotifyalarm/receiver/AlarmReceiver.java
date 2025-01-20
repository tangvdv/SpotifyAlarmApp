package dev.tangvdv.spotifyalarm.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import dev.tangvdv.spotifyalarm.R;
import dev.tangvdv.spotifyalarm.helper.AlarmSharedPreferences;
import dev.tangvdv.spotifyalarm.helper.AlarmWakeLock;
import dev.tangvdv.spotifyalarm.activity.AlarmLockScreenActivity;
import dev.tangvdv.spotifyalarm.helper.NotificationHelper;
import dev.tangvdv.spotifyalarm.model.AlarmModel;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmWakeLock.acquireAlarmWakeLock(context);

        NotificationManager notificationManager = NotificationHelper.getNotificationManager(context);
        notificationManager.notify(R.integer.notification_id, NotificationHelper.getNotification(context, "Alarm is setting up", "", null));

        AlarmModel.getInstance().setAlarmModel(AlarmSharedPreferences.loadAlarm(context));
        AlarmModel.getInstance().setAlarmOff();

        Intent lockScreen = new Intent(context, AlarmLockScreenActivity.class);
        lockScreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(lockScreen);
    }
}