package com.example.spotifyalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.spotifyalarm.databinding.ActivityMainBinding;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private String playlistId;
    private SpotifyAPI spotifyAPI;

    private ArrayAdapter<String> spinnerAdapter;
    private List<MusicModel> musicModelList;

    private ActivityMainBinding binding;
    private MaterialTimePicker timePicker;
    private Calendar calendar;
    private Intent alarmServiceIntent;

    private SharedPreferences sharedPreferences;

    private int hour;
    private int minute;

    private AuthorizationResponse response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = this.getSharedPreferences("Settings", Context.MODE_PRIVATE);

        alarmServiceIntent = new Intent(this, AlarmManagerService.class);

        context = this;

        startActivity();

        hour = sharedPreferences.getInt("time_hour", 0);
        minute = sharedPreferences.getInt("time_minute", 0);
        playlistId = sharedPreferences.getString("playlistId", "");

        setCalendar();

        binding.setAlarmSwitch.setChecked(
                (AlarmModel.getInstance().getPendingIntent() != null)
        );

        bindingManager();
    }

    private void bindingManager(){
        binding.btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                MusicModel musicModel = musicModelList.get((int) binding.spinnerPlaylist.getSelectedItemId());
                playlistId = musicModel.getId();
                editor.putString("playlistId", playlistId);
                editor.apply();
                selectPlaylist(musicModel.getMusicUri());
                getSpotifyPlaylist(playlistId);

            }
        });

        binding.btnSetTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTime();
            }
        });

        binding.setAlarmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    startForegroundService(alarmServiceIntent);
                }
                else{
                    AlarmModel.getInstance().setPendingIntent(null);
                    stopService(alarmServiceIntent);
                }
            }
        });

        binding.btnMusicSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, MusicSelectionList.class);
                startActivity(intent);
            }
        });
    }

    private void startActivity(){
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(context.getString(R.string.client_id), AuthorizationResponse.Type.TOKEN, context.getString(R.string.redirect_uri));

        builder.setScopes(new String[]{"streaming", "playlist-read-private"});
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(this, context.getResources().getInteger(R.integer.request_code) ,request);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == context.getResources().getInteger(R.integer.request_code)) {
            response = AuthorizationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthorizationResponse.Type.TOKEN) {
                getSpotifyPlaylist(playlistId);
                showUserPlaylistsInSpinner();
            }
            else{
                Log.e("MainActivity", "response type wrong");
            }
        }
        else{
            Log.e("MainActivity", "request code failed");
        }
    }

    private void showUserPlaylistsInSpinner(){
        spotifyAPI = new SpotifyAPI(this, response.getAccessToken());
        spotifyAPI.getUserPlaylist(new SpotifyAPI.UserPlaylistsCallBack() {
            @Override
            public void onSuccess(List<MusicModel> list) {
                if(list != null && !list.isEmpty()){
                    ArrayList<String> playListName = new ArrayList<String>(list.size());
                    musicModelList = list;
                    list.forEach((p) -> playListName.add(p.getName()));

                    spinnerAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, playListName);
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.spinnerPlaylist.setAdapter(spinnerAdapter);
                }
            }
            @Override
            public void onError(String error) {
                Log.e("MainActivity | SpotifyUserPlaylists", error);
            }
        });
    }

    private void getSpotifyPlaylist(String playlistId){
        Log.i("MainActivity", "PlaylistId : "+playlistId);
        if(!playlistId.equals("")){
            spotifyAPI = new SpotifyAPI(this, response.getAccessToken());
            spotifyAPI.getPlaylist(new SpotifyAPI.PlaylistCallBack() {
                @Override
                public void onSuccess(MusicModel musicModel) {
                    setPlaylistLayout(musicModel);
                }

                @Override
                public void onError(String error) {
                    Log.e("MainActivity | SpotifyPlaylist", error);
                }
            }, playlistId);
        }
    }

    private void setPlaylistLayout(MusicModel playlist){
        binding.textPlaylistName.setText(playlist.getName());
        binding.textPlaylistOwner.setText(playlist.getOwnerName());
        Glide.with(context)
                .load(playlist.getImage_url())
                .apply(new RequestOptions()
                        .transform(new CenterCrop(), new RoundedCorners(10))
                )
                .into(binding.imagePlaylist);
    }

    private void selectPlaylist(String playlistUri){
        AlarmModel.getInstance().setPlaylist_uri(playlistUri);
    }

    private void selectTime(){
        timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Select Alarm Time")
                .build();


        timePicker.show(getSupportFragmentManager(), "UwU");
        timePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                hour = timePicker.getHour();
                minute = timePicker.getMinute();
                editor.putInt("time_hour", timePicker.getHour());
                editor.putInt("time_minute", timePicker.getMinute());
                editor.apply();

                setCalendar();

                if(AlarmModel.getInstance().getPendingIntent() != null){
                    startService(alarmServiceIntent);
                }
            }
        });
    }

    private void setCalendar(){
        calendar = calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if(calendar.getTimeInMillis() < System.currentTimeMillis()){
            calendar.add(Calendar.DATE, 1);
        }

        AlarmModel.getInstance().setCalendar(calendar);

        setAlarmTextDate(calendar);
    }

    private void setAlarmTextDate(Calendar calendar){
        if(calendar != null) {
            binding.btnSetTime.setText(new SimpleDateFormat("HH:mm").format(calendar.getTime()));
            binding.alarmDateText.setText(new SimpleDateFormat("MMMM dd").format(calendar.getTime()));
        }
    }

    @Override
    protected void onDestroy() {
        AuthorizationClient.stopLoginActivity(this, context.getResources().getInteger(R.integer.request_code));
        super.onDestroy();
    }
}