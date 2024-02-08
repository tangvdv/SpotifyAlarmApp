package com.example.spotifyalarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spotifyalarm.databinding.SettingsActivityBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private SettingsActivityBinding binding;
    private SharedPreferences sharedPreferences;

    private boolean repeatSetting = false;
    private boolean shuffleSetting = false;
    private int volumeSetting;
    private int stopAlarmSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = SettingsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sharedPreferences = this.getSharedPreferences("App", Context.MODE_PRIVATE);

        bindingManager();
        setSettingsView();
    }

    private void setSettingsView(){
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        binding.seekBarSoundVolume.setProgress(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2);
        binding.seekBarSoundVolume.setMax(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        binding.alarmStateText.setText(AlarmModel.getInstance().isState() ? "On" : "Off");

        try {
            String data = sharedPreferences.getString("settings", "");
            if(!Objects.equals(data, "")){
                Log.i(TAG, "Settings data loaded");
                JSONObject jsonData = new JSONObject(data);

                shuffleSetting = Boolean.parseBoolean(jsonData.getString("shuffle"));
                repeatSetting = Boolean.parseBoolean(jsonData.getString("repeat"));
                volumeSetting = jsonData.getInt("volume");
                stopAlarmSetting = jsonData.getInt("stopAlarm");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        binding.switchRepeatSettings.setChecked(repeatSetting);
        binding.switchShuffleSettings.setChecked(shuffleSetting);
        binding.seekBarSoundVolume.setProgress(volumeSetting);
        binding.spinnerStopAlarmSettings.setSelection(stopAlarmSetting);
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
                repeatSetting = !repeatSetting;
                binding.switchRepeatSettings.setChecked(repeatSetting);
            }
        });

        binding.switchRepeatSettings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                repeatSetting = b;
            }
        });

        binding.btnShuffleSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shuffleSetting = !shuffleSetting;
                binding.switchShuffleSettings.setChecked(shuffleSetting);
            }
        });

        binding.switchShuffleSettings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                shuffleSetting = b;
            }
        });

        binding.seekBarSoundVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                volumeSetting = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.spinnerStopAlarmSettings.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                stopAlarmSetting = pos;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void applySettings(){
        String data = settingsToJsonString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("settings", data);
        editor.apply();

        finish();
    }

    private String settingsToJsonString(){
        JSONObject jsonSettings = new JSONObject();

        try {
            Log.i(TAG, "Settings data saved");
            jsonSettings.put("shuffle", String.valueOf(shuffleSetting));
            jsonSettings.put("repeat", String.valueOf(repeatSetting));
            jsonSettings.put("volume", volumeSetting);
            jsonSettings.put("stopAlarm", stopAlarmSetting);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return jsonSettings.toString();
    }
}