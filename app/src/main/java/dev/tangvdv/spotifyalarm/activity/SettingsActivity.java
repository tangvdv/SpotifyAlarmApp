package dev.tangvdv.spotifyalarm.activity;

import android.content.Context;
import android.media.AudioManager;
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        settingsModel = new SettingsModel(AlarmSharedPreferences.loadSettings(context));

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
                finish();
            }
        });

        binding.btnApplySettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                applySettings();
            }
        });

        binding.btnRepeatSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                settingsModel.setRepeat(!settingsModel.isRepeat());
                binding.switchRepeatSettings.setChecked(settingsModel.isRepeat());
            }
        });

        binding.switchRepeatSettings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                settingsModel.setRepeat(b);
            }
        });

        binding.btnShuffleSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsModel.setShuffle(!settingsModel.isShuffle());
                binding.switchShuffleSettings.setChecked(settingsModel.isShuffle());
            }
        });

        binding.btnLoopSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                settingsModel.setLoopMusic(!settingsModel.isLooping());
                binding.switchLoopSettings.setChecked(settingsModel.isLooping());
            }
        });

        binding.switchShuffleSettings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                settingsModel.setShuffle(b);
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
            }
        });

    }

    private void applySettings(){
        AlarmSharedPreferences.saveSettings(context, settingsModel.getSettingsModelContent());
        finish();
    }
}