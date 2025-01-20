package dev.tangvdv.spotifyalarm.activity;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import dev.tangvdv.spotifyalarm.helper.AlarmSharedPreferences;
import dev.tangvdv.spotifyalarm.R;
import dev.tangvdv.spotifyalarm.databinding.SettingsActivityBinding;
import dev.tangvdv.spotifyalarm.model.AlarmModel;
import dev.tangvdv.spotifyalarm.model.SettingsModel;

public class SettingsActivity extends AppCompatActivity {
    private Context context;
    private SettingsActivityBinding binding;
    private SettingsModel settingsModel;
    private Ringtone defaultRingtone;
    private AudioManager am;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        settingsModel = new SettingsModel(AlarmSharedPreferences.loadSettings(context));
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        bindingManager();
        setSettingsView();
    }

    private void setSettingsView(){
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        binding.seekBarSoundVolume.setProgress(settingsModel.getVolume());
        binding.seekBarSoundVolume.setMax(am.getStreamMaxVolume(AudioManager.STREAM_ALARM));
        binding.alarmStateText.setText(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON ? context.getString(R.string.alarm_on) : context.getString(R.string.alarm_off));

        binding.switchRepeatSettings.setChecked(settingsModel.isRepeat());
        binding.switchShuffleSettings.setChecked(settingsModel.isShuffle());
        binding.seekBarSoundVolume.setProgress(settingsModel.getVolume());
        binding.switchLoopSettings.setChecked(settingsModel.isLooping());
    }

    private void bindingManager(){
        binding.btnCancelSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRingtone();
                finish();
            }
        });

        binding.btnApplySettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRingtone();
                applySettings();
            }
        });

        binding.btnRepeatSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                settingsModel.setRepeat(!settingsModel.isRepeat());
                binding.switchRepeatSettings.setChecked(settingsModel.isRepeat());
                stopRingtone();
            }
        });

        binding.switchRepeatSettings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                settingsModel.setRepeat(b);
                stopRingtone();
            }
        });

        binding.btnShuffleSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsModel.setShuffle(!settingsModel.isShuffle());
                binding.switchShuffleSettings.setChecked(settingsModel.isShuffle());
                stopRingtone();
            }
        });

        binding.btnLoopSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsModel.setLoopMusic(!settingsModel.isLooping());
                binding.switchLoopSettings.setChecked(settingsModel.isLooping());
                stopRingtone();
            }
        });

        binding.switchShuffleSettings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                settingsModel.setShuffle(b);
                stopRingtone();
            }
        });

        int min = 1;
        binding.seekBarSoundVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(i < min) {
                    i = min;
                    binding.seekBarSoundVolume.setProgress(min);
                }
                settingsModel.setVolume(i);
                playRingtone(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.switchLoopSettings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                settingsModel.setLoopMusic(b);
                stopRingtone();
            }
        });

    }

    private void applySettings(){
        AlarmSharedPreferences.saveSettings(context, settingsModel.getSettingsModelContent());
        finish();
    }

    private void getRingtone(){
        Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_ALARM);
        defaultRingtone = RingtoneManager.getRingtone(this, defaultRingtoneUri);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        defaultRingtone.setAudioAttributes(audioAttributes);
    }

    private void playRingtone(int i){
        if(defaultRingtone == null) getRingtone();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            float volume = (float) i / am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
            defaultRingtone.setVolume(volume);
        }

        if(!defaultRingtone.isPlaying()) defaultRingtone.play();
    }

    private void stopRingtone(){
        if(defaultRingtone == null) getRingtone();

        if(defaultRingtone.isPlaying()) defaultRingtone.stop();
    }
}