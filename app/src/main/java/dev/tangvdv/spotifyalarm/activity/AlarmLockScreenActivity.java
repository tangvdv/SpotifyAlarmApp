package dev.tangvdv.spotifyalarm.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import dev.tangvdv.spotifyalarm.helper.AlarmHelper;
import dev.tangvdv.spotifyalarm.R;
import dev.tangvdv.spotifyalarm.service.MusicService;

public class AlarmLockScreenActivity extends AppCompatActivity implements MusicService.MusicServiceCallback {
    private Handler handler;
    private TextView currentTimeTextView;
    private TextView currentDateTextView;
    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;
    private MusicService musicService;
    private boolean isBound = false;
    private boolean isCompleted = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isBound = true;
            musicService.setCallback(AlarmLockScreenActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            musicService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlarmHelper.getInstance(this).setLockScreenActivity(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);

        ContextCompat.startForegroundService(this, serviceIntent);
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
                PixelFormat.TRANSLUCENT);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        overlayView = inflater.inflate( R.layout.activity_alarm_lock_screen, null);

        currentTimeTextView = overlayView.findViewById(R.id.currentTime);
        currentDateTextView = overlayView.findViewById(R.id.currentDate);

        windowManager.addView(overlayView, params);

        handler = new Handler();

        updateDateTime();

        Button turnOffBtn = overlayView.findViewById(R.id.turnOffBtn);
        turnOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isCompleted){
                    finish();
                }
            }
        });
    }

    public Activity getLockScreenActivity() {
        return this;
    }

    @Override
    public void onCompletion() {
        isCompleted = true;
    }

    private void updateDateTime() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM EEEE", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());

                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                String currentTime = timeFormat.format(new Date());

                currentTimeTextView.setText(currentTime);
                currentDateTextView.setText(currentDate);

                windowManager.updateViewLayout(overlayView, params);

                handler.postDelayed(this, 1000);
            }
        }, 0);
    }

    private void removeAlarm(){
        if(isCompleted) {
            windowManager.removeView(overlayView);
            handler.removeCallbacksAndMessages(null);
            AlarmHelper.getInstance(this).shutAlarmOff();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
        removeAlarm();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}