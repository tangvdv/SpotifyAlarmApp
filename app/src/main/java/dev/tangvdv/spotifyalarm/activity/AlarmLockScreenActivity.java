package dev.tangvdv.spotifyalarm.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
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
import dev.tangvdv.spotifyalarm.model.AlarmModel;
import dev.tangvdv.spotifyalarm.service.MusicService;

public class AlarmLockScreenActivity extends AppCompatActivity {
    private Handler handler;
    private TextView currentTimeTextView;
    private TextView currentDateTextView;
    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AlarmHelper.getInstance(this).setLockScreenActivity(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);

        ContextCompat.startForegroundService(this, serviceIntent);

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
                if(AlarmModel.getInstance().getIsRinging()){
                    finish();
                }
            }
        });
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
        windowManager.removeView(overlayView);
        handler.removeCallbacksAndMessages(null);
        AlarmHelper.getInstance(this).shutAlarmOff();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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