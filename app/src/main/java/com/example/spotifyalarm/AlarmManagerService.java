package com.example.spotifyalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.example.spotifyalarm.model.AlarmModel;

public class AlarmManagerService extends Service {
    private static final String TAG = "AlarmManagerService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if(isNetworkConnected()){
            if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.OFF || AlarmModel.getInstance().getCurrentState() == AlarmModel.State.RINGING){
                setAlarm();
            }
        }
        else{
            onDestroy();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void setAlarm(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms();
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, AlarmModel.getInstance().getCalendar().getTimeInMillis(), pendingIntent);

        startService(new Intent(this, AlarmNotificationService.class));

        AlarmModel.getInstance().setAlarmOn();

        Log.i(TAG, AlarmModel.getInstance().getAlarmModelContent().toString() );
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}