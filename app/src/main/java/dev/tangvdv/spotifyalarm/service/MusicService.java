package dev.tangvdv.spotifyalarm.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import dev.tangvdv.spotifyalarm.R;
import dev.tangvdv.spotifyalarm.helper.AlarmSharedPreferences;
import dev.tangvdv.spotifyalarm.helper.AlarmHelper;
import dev.tangvdv.spotifyalarm.helper.AlarmWakeLock;
import dev.tangvdv.spotifyalarm.helper.LogFile;
import dev.tangvdv.spotifyalarm.helper.NotificationHelper;
import dev.tangvdv.spotifyalarm.helper.SpotifyRemoteHelper;
import dev.tangvdv.spotifyalarm.receiver.NotificationShutAlarmOffReceiver;
import dev.tangvdv.spotifyalarm.model.AlarmModel;
import dev.tangvdv.spotifyalarm.model.SettingsModel;

import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.Objects;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private SpotifyAppRemote mSpotifyAppRemote;
    private LogFile logFile;
    private SettingsModel settingsModel;
    private Context context;
    private final IBinder binder = new LocalBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        context = this;

        startForeground(R.integer.notification_id, NotificationHelper.getForegroundNotification(context, "Alarm is setting up"));

        try{
            settingsModel = new SettingsModel(AlarmSharedPreferences.loadSettings(this));

            logFile = new LogFile(this);
            AlarmModel.getInstance().setIsRinging(true);
            if(isNetworkConnected()){
                SpotifyRemoteHelper.spotifyAppRemoteConnection(context, new SpotifyRemoteHelper.SpotifyRemoteCallback() {
                    @Override
                    public void onRemoteConnected(SpotifyAppRemote spotifyAppRemote) {
                        if (AlarmModel.getInstance().getIsRinging()) {
                            logFile.writeToFile(TAG, "SpotifyAppRemote on connected");
                            mSpotifyAppRemote = spotifyAppRemote;
                            AlarmModel.getInstance().setSpotifyAppRemote(spotifyAppRemote);
                            playSpotifyAlarm();
                        }
                    }

                    @Override
                    public void onRemoteConnectionError(Throwable throwable) {
                        if (AlarmModel.getInstance().getIsRinging()) {
                            logFile.writeToFile(TAG, throwable.getMessage());
                            Log.e(TAG, throwable.getMessage(), throwable);
                            playBackupAlarm();
                        }
                    }
                });
            }
            else{
                playBackupAlarm();
            }

            setNextAlarm();
        }
        catch(Exception e){
            logFile = new LogFile(context);
            logFile.writeToFile(TAG, Objects.requireNonNull(e.getMessage()));
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void playSpotifyAlarm() {
        String uri = AlarmModel.getInstance().getPlaylist_uri();
        if (!uri.equals("")) {
            // APPLY SETTINGS
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setStreamVolume(AudioManager.STREAM_ALARM, settingsModel.getVolume(), 0);
            mSpotifyAppRemote.getPlayerApi().setShuffle(settingsModel.isShuffle());
            mSpotifyAppRemote.getConnectApi().connectSwitchToLocalDevice();
            mSpotifyAppRemote.getPlayerApi().play(uri, PlayerApi.StreamType.ALARM);

            AlarmModel.getInstance().setAlarmSpotifyType();

            AlarmHelper.getInstance(context).getAlarmState(AlarmHelper.State.PLAY, new AlarmHelper.AlarmStateCallback() {
                @Override
                public void onCompletion(boolean isPlaying) {
                    if(isPlaying){
                        alarmIsRinging();
                    }
                    stopForeground(STOP_FOREGROUND_DETACH);
                }
            });

        } else {
            Log.e(TAG, "Playlist uri not found");
            logFile.writeToFile(TAG, "Playlist uri not found");
            playBackupAlarm();
        }
    }

    private void playBackupAlarm(){
        Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_ALARM);
        Ringtone defaultRingtone = RingtoneManager.getRingtone(this, defaultRingtoneUri);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        defaultRingtone.setAudioAttributes(audioAttributes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            defaultRingtone.setLooping(settingsModel.isLooping());

            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            float volume = (float) settingsModel.getVolume() / am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            defaultRingtone.setVolume(volume);
        }
        defaultRingtone.play();
        AlarmModel.getInstance().setBackupAlarmRingtone(defaultRingtone);
        AlarmModel.getInstance().setAlarmBackupType();
        AlarmHelper.getInstance(context).getAlarmState(AlarmHelper.State.PLAY, new AlarmHelper.AlarmStateCallback() {
            @Override
            public void onCompletion(boolean isPlaying) {
                if(isPlaying){
                    alarmIsRinging();
                }
                stopForeground(STOP_FOREGROUND_DETACH);
            }
        });
    }

    private void alarmIsRinging(){
        AlarmHelper.getInstance(context).shutAlarmOffHandler();
        stopForeground(STOP_FOREGROUND_DETACH);

        Intent intent = new Intent(context, NotificationShutAlarmOffReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        NotificationManager notificationManager = NotificationHelper.getNotificationManager(context);
        notificationManager.notify(R.integer.notification_id,NotificationHelper.getNotification(context, "Alarm is ringing !","Click to shut alarm off", pendingIntent));

        AlarmSharedPreferences.saveAlarm(context, AlarmModel.getInstance().getAlarmModelContent());
    }

    private void setNextAlarm(){
        if(settingsModel.isRepeat()) AlarmHelper.getInstance(context).setAlarm();
        AlarmSharedPreferences.saveAlarm(this, AlarmModel.getInstance().getAlarmModelContent());
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    @Override
    public void onDestroy() {
        AlarmWakeLock.releaseAlarmWakeLock(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
}