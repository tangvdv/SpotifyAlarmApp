package com.example.spotifyalarm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.spotifyalarm.databinding.ActivityMainBinding;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.spotify.android.appremote.api.ContentApi;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.Capabilities;
import com.spotify.protocol.types.ListItem;
import com.spotify.protocol.types.ListItems;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.json.JSONObject;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private String playlistUri;
    private String playlistId;
    private SpotifyAPI spotifyAPI;

    private ArrayAdapter<String> spinnerAdapter;
    private List<PlaylistModel> playlistModelList;

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
                PlaylistModel playlistModel = playlistModelList.get((int) binding.spinnerPlaylist.getSelectedItemId());
                playlistId = playlistModel.getId();
                editor.putString("playlistId", playlistId);
                editor.apply();
                selectPlaylist(playlistModel.getPlaylistUri());
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
                    startService(alarmServiceIntent);
                }
                else{
                    AlarmModel.getInstance().setPendingIntent(null);
                    stopService(alarmServiceIntent);
                }
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
            getSpotifyPlaylist(playlistId);

            if (response.getType() == AuthorizationResponse.Type.TOKEN) {
                Log.i("MainActivity", "TOKEN : "+response.getAccessToken());
                spotifyAPI = new SpotifyAPI(this, response.getAccessToken());
                spotifyAPI.getUserPlaylist(new SpotifyAPI.UserPlaylistsCallBack() {
                    @Override
                    public void onSuccess(List<PlaylistModel> list) {
                        if(list != null && !list.isEmpty()){
                            ArrayList<String> playListName = new ArrayList<String>(list.size());
                            playlistModelList = list;
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
        }
    }

    private void getSpotifyPlaylist(String playlistId){
        Log.i("MainActivity", "PlaylistId : "+playlistId);
        if(!playlistId.equals("")){
            if(response.getType() == AuthorizationResponse.Type.TOKEN){
                spotifyAPI = new SpotifyAPI(this, response.getAccessToken());
                spotifyAPI.getPlaylist(new SpotifyAPI.PlaylistCallBack() {
                    @Override
                    public void onSuccess(PlaylistModel playlistModel) {
                        setPlaylistLayout(playlistModel);
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("MainActivity | SpotifyPlaylist", error);
                    }
                }, playlistId);
            }
        }
    }

    private void setPlaylistLayout(PlaylistModel playlist){
        binding.textPlaylistName.setText(playlist.getName());
        binding.textPlaylistOwner.setText(playlist.getOwnerName());
        Glide.with(context)
                .load(playlist.getImage_url())
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
}