package dev.tangvdv.spotifyalarm.helper;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.os.Build;
import android.os.Handler;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.PlayerState;

import java.util.Objects;

import dev.tangvdv.spotifyalarm.R;
import dev.tangvdv.spotifyalarm.model.AlarmModel;
import dev.tangvdv.spotifyalarm.receiver.AlarmReceiver;

public class AlarmHelper {
    public interface AlarmStateCallback{
        void onCompletion(boolean isPlaying);
    }
    public enum State {
        PLAY,
        PAUSE
    }
    private Context context;
    private Handler handler;
    private static AlarmHelper instance;
    private Activity alarmLockScreenActivity;
    public AlarmHelper(Context context){
        this.context = context;
    }

    public static synchronized AlarmHelper getInstance(Context context){
        if(instance == null){
            instance = new AlarmHelper(context);
        }
        return instance;
    }

    public void setLockScreenActivity(Activity activity){
        this.alarmLockScreenActivity = activity;
    }

    public void getAlarmState(State state, AlarmHelper.AlarmStateCallback callback){
        if(AlarmModel.getInstance().getCurrentType() != null){
            handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    final Runnable r = this;
                    if(AlarmModel.getInstance().getIsRinging()) {
                        if (AlarmModel.getInstance().getCurrentType() == AlarmModel.Type.BACKUP) {
                            boolean isPlaying = AlarmModel.getInstance().getBackupAlarmRingtone().isPlaying();
                            if (state == State.PLAY) {
                                if (isPlaying) {
                                    callback.onCompletion(true);
                                } else {
                                    handler.postDelayed(r, 1000);
                                }
                            } else if (state == State.PAUSE) {
                                if (!isPlaying) {
                                    callback.onCompletion(false);
                                } else {
                                    handler.postDelayed(r, 1000);
                                }
                            }
                        } else if (AlarmModel.getInstance().getCurrentType() == AlarmModel.Type.SPOTIFY) {
                            AlarmModel.getInstance().getSpotifyAppRemote().getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
                                @Override
                                public void onResult(PlayerState playerState) {
                                    if (state == State.PLAY) {
                                        if (!playerState.isPaused) {
                                            callback.onCompletion(true);
                                        } else {
                                            handler.postDelayed(r, 1000);
                                        }
                                    } else if (state == State.PAUSE) {
                                        if (playerState.isPaused) {
                                            callback.onCompletion(false);
                                        } else {
                                            handler.postDelayed(r, 1000);
                                        }
                                    }
                                }
                            });
                        }
                    }
                    else{
                        callback.onCompletion(false);
                    }
                }
            });
        }
    }

    public PendingIntent getAlarmPendingIntent(){
        Intent intent = new Intent(context, AlarmReceiver.class);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
    }

    public void alarmCurrentStateHandler(){
        PendingIntent pendingIntent = getAlarmPendingIntent();
        if (pendingIntent != null) {
            if(Objects.equals(pendingIntent.getCreatorPackage(), context.getPackageName()) && AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON){
                setAlarm();
            }
            else{
                pendingIntent.cancel();
                AlarmModel.getInstance().setAlarmOff();
            }
        } else {
            AlarmModel.getInstance().setAlarmOff();
        }
    }

    public void setAlarm(){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms();
        }

        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(AlarmModel.getInstance().getCalendar().getTimeInMillis(), pendingIntent), pendingIntent);

        AlarmModel.getInstance().setAlarmOn();
        AlarmSharedPreferences.saveAlarm(context, AlarmModel.getInstance().getAlarmModelContent());

        String formattedHour = String.format("%02d", AlarmModel.getInstance().getHour());
        String formattedMinute = String.format("%02d", AlarmModel.getInstance().getMinute());

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent appPendingIntent = null;
        if(launchIntent != null){
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            appPendingIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationManager notificationManager = NotificationHelper.getNotificationManager(context);
        notificationManager.notify(R.integer.notification_id, NotificationHelper.getNotification(context, "Alarm is running !", "Time : "+String.format("%s:%s", formattedHour, formattedMinute), appPendingIntent));

        setResult();
    }

    private void setResult(){
        Intent intent = new Intent("intentAlarmKey");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public void shutAlarmOff(){
        AlarmHelper.getInstance(context).getAlarmState(State.PLAY, new AlarmStateCallback() {
            @Override
            public void onCompletion(boolean isPlaying) {
                if(isPlaying){
                    Ringtone defaultRingtone = AlarmModel.getInstance().getBackupAlarmRingtone();
                    SpotifyAppRemote spotifyAppRemote = AlarmModel.getInstance().getSpotifyAppRemote();
                    if(defaultRingtone != null) defaultRingtone.stop();
                    if(spotifyAppRemote != null) spotifyAppRemote.getPlayerApi().pause();

                    AlarmModel.getInstance().setIsRinging(false);

                    NotificationHelper.cancelNotification(context);

                    if(alarmLockScreenActivity != null) {
                        alarmLockScreenActivity.finish();
                    }
                    handler.removeCallbacksAndMessages(null);
                }
            }
        });
    }

    public void shutAlarmOffHandler(){
        AlarmHelper.getInstance(context).getAlarmState(AlarmHelper.State.PAUSE, new AlarmHelper.AlarmStateCallback() {
            @Override
            public void onCompletion(boolean isPlaying) {
                if(!isPlaying){
                    shutAlarmOff();
                }
            }
        });
    }

}
