package com.example.spotifyalarm;

import static java.lang.Thread.sleep;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.spotifyalarm.databinding.ActivityMainBinding;
import com.example.spotifyalarm.databinding.UserProfileDialogBinding;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Context context;
    private ActivityMainBinding binding;

    private Dialog userProfileDialog;

    private MaterialTimePicker timePicker;
    private Calendar calendar;
    private Intent alarmServiceIntent;
    private SharedPreferences sharedPreferences;
    private int hour;
    private int minute;
    private Boolean isSpotifyActivityConnected = false;

    private SpotifyAPI spotifyAPI;

    private final ActivityResultLauncher<Intent> musicActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if(data != null && data.getExtras() != null){
                            String music =  data.getStringExtra("Data");
                            if(!Objects.equals(music, "")){
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("music", music);
                                editor.apply();
                            }

                            getMusicData();
                        }
                    }
                    else if(result.getResultCode() == Activity.RESULT_CANCELED){
                        Intent data = result.getData();
                        if(data != null && data.getExtras() != null){
                            if(Objects.equals(data.getStringExtra("data"), "")){
                                errorUserToast("Error");
                            }
                            else{
                                errorUserToast("Error : "+ data.getStringExtra("Data"));
                            }
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferences = this.getSharedPreferences("App", Context.MODE_PRIVATE);

        alarmServiceIntent = new Intent(this, AlarmManagerService.class);

        hour = sharedPreferences.getInt("time_hour", 0);
        minute = sharedPreferences.getInt("time_minute", 0);

        context = this;

        setupActivityViews();
    }

    private void setupActivityViews(){
        if (isNetworkConnected() && !isSpotifyActivityConnected){
            startSpotifyActivity();
        }

        getUserProfileData();

        bindingManager();

        getMusicData();
        setCalendar();
        alarmBindingState();

        binding.setAlarmSwitch.setChecked( AlarmModel.getInstance().isState() );
    }

    private void bindingManager(){
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
                    if(isNetworkConnected()){
                        if(isSpotifyActivityConnected){
                            startService(alarmServiceIntent);
                        }
                        else{
                            errorUserToast("You need to be connected to Spotify to set an alarm");
                            binding.setAlarmSwitch.setChecked(false);
                        }
                    }
                    else{
                        errorUserToast("You need to be connected to internet to set an alarm");
                        binding.setAlarmSwitch.setChecked(false);
                    }
                }
                else{
                    stopService(alarmServiceIntent);
                }

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        while(b != AlarmModel.getInstance().isState()) {
                            try {
                                sleep(100);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                alarmBindingState();
                            }
                        });
                    }
                };
                thread.start();
            }
        });

        if(isNetworkConnected()) {
            Thread musicButtonThread = new Thread() {
                @Override
                public void run() {
                    while (!isSpotifyActivityConnected) {
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    binding.btnMusicSelection.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(context, MusicLibraryActivity.class);
                            musicActivityResult.launch(intent);
                        }
                    });
                }
            };

            musicButtonThread.start();
        }

        binding.btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, SettingsActivity.class);
                startActivity(intent);
            }
        });

        binding.btnUserProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(userProfileDialog != null){
                    userProfileDialog.show();
                }
            }
        });
    }

    private void startSpotifyActivity(){
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(context.getString(R.string.client_id), AuthorizationResponse.Type.TOKEN, context.getString(R.string.redirect_uri));

        builder.setScopes(getResources().getStringArray(R.array.scopes));
        builder.setShowDialog(true);
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(this, context.getResources().getInteger(R.integer.request_code) ,request);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == context.getResources().getInteger(R.integer.request_code)) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthorizationResponse.Type.TOKEN) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("TOKEN", response.getAccessToken());
                editor.apply();
                isSpotifyActivityConnected = true;
            }
            else{
                Log.e(TAG, "Response error : "+response.getError());
                errorUserToast("Error : failed to open spotify activity.");
            }
        }
    }

    private void getUserProfileData(){
        String data = sharedPreferences.getString("user", "");
        HashMap<String, String> user = new HashMap<>();

        if(!Objects.equals(data, "")){
            Log.i(TAG, "User profile data with shared preferences");
            try {
                JSONObject jsonData = new JSONObject(data);

                user.put("image", jsonData.getString("image"));
                user.put("name", jsonData.getString("name"));

                setUserProfileView(user);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            if(isNetworkConnected()){
                Log.i(TAG, "User Profile data with API");
                Thread userProfileDataThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(!isSpotifyActivityConnected){
                            try {
                                sleep(100);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        spotifyAPI = new SpotifyAPI(context, sharedPreferences.getString("TOKEN", ""));
                        spotifyAPI.getUserProfile(new SpotifyAPI.SpotifyAPIUserProfileCallback() {
                            @Override
                            public void onSuccess(String name, String image_url) {
                                user.put("image", image_url);
                                user.put("name", name);

                                JSONObject jsonUser = new JSONObject(user);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("user", jsonUser.toString());
                                editor.apply();

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        setUserProfileView(user);
                                    }
                                });
                            }

                            @Override
                            public void onError(String error) {
                                Log.e(TAG, "GetUserProfile : " + error);
                                errorUserToast("GetUserProfile : " + error);
                            }
                        });
                    }
                });

                userProfileDataThread.start();
            }
        }
    }

    private void setUserProfileView(HashMap<String, String> user){
        Glide.with(context)
                .load(user.get("image"))
                .apply(new RequestOptions()
                        .transform(new CenterCrop(), new CircleCrop())
                )
                .into(binding.imageUserProfile);

        userProfileDialog = new Dialog(context);
        userProfileDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        userProfileDialog.setCancelable(true);
        userProfileDialog.setContentView(R.layout.user_profile_dialog);

        UserProfileDialogBinding binding = UserProfileDialogBinding.inflate(LayoutInflater.from(context));
        userProfileDialog.setContentView(binding.getRoot());

        binding.textUserProfile.setText(user.get("name"));
        Glide.with(context)
                .load(user.get("image"))
                .apply(new RequestOptions()
                        .transform(new CenterCrop(), new CircleCrop())
                )
                .into(binding.imageUserProfile);

        setUserProfileBinding(binding);
    }

    private void setUserProfileBinding(UserProfileDialogBinding binding){
        binding.btnOpenSpotifyApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.spotify.music");
                if (launchIntent != null) {
                    startActivity(launchIntent);
                }
                else{
                    errorUserToast("Couldn't find spotify application");
                }
            }
        });

        binding.btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userProfileDialog.dismiss();

                AuthorizationClient.clearCookies(context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();

                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
                finishAffinity();
            }
        });
    }

    private void getMusicData(){
        HashMap<String, String> music = new HashMap<>();
        try {
            String data = sharedPreferences.getString("music", "");
            if(!Objects.equals(data, "")){
                JSONObject jsonData = new JSONObject(data);

                music.put("image", jsonData.getString("image"));
                music.put("name", jsonData.getString("name"));
                music.put("type", jsonData.getString("type"));
                AlarmModel.getInstance().setPlaylist_uri(jsonData.getString("uri"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        setPlaylistLayout(music);
    }

    private void setPlaylistLayout(HashMap<String, String> data){
        if(data != null && !data.isEmpty()){
            binding.textPlaylistName.setText(data.get("name"));
            binding.textPlaylistOwner.setText(data.get("type"));
            Glide.with(context)
                    .load(data.get("image"))
                    .apply(new RequestOptions()
                            .transform(new CenterCrop(), new RoundedCorners(10))
                    )
                    .into(binding.imagePlaylist);
        }
        else{
            binding.textPlaylistName.setText("Select music");
        }
    }

    private void selectTime(){
        timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Select Alarm Time")
                .setTheme(R.style.ThemeOverlay_App_MaterialTimePicker)
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

                if(AlarmModel.getInstance().isState()){
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

        alarmTextValue();
    }

    private void alarmTextValue(){
        if(calendar != null) {
            binding.alarmTimeText.setText(new SimpleDateFormat("HH:mm").format(calendar.getTime()));
            binding.alarmDateText.setText(new SimpleDateFormat("MMMM dd").format(calendar.getTime()));
        }else{
            binding.alarmTimeText.setVisibility(View.GONE);
            binding.alarmDateText.setVisibility(View.GONE);
        }
    }

    private void alarmBindingState(){
        binding.btnMusicSelection.setClickable(isNetworkConnected());
        binding.setAlarmSwitch.setChecked(AlarmModel.getInstance().isState());
        if(AlarmModel.getInstance().isState()){
            binding.alarmTimeText.setTextColor(getResources().getColor(R.color.white));
            binding.alarmDateText.setTextColor(getResources().getColor(R.color.white));
            binding.alarmTimeLeftText.setVisibility(View.VISIBLE);
            alarmTimeLeftThread();
        }
        else{
            binding.alarmTimeText.setTextColor(getResources().getColor(R.color.light_grey));
            binding.alarmDateText.setTextColor(getResources().getColor(R.color.light_grey));
            binding.alarmTimeLeftText.setVisibility(View.GONE);
        }
    }

    private void alarmTimeLeftThread(){
        Thread timeLeftThread = new Thread() {
            @Override
            public void run() {
                while(!MainActivity.this.isDestroyed() && AlarmModel.getInstance().isState()) {
                    try {
                        Log.i(TAG, "Tick");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long diffInMillies = Math.abs(Calendar.getInstance().getTimeInMillis() - calendar.getTimeInMillis());

                                long hours = diffInMillies / (60 * 60 * 1000);
                                long minutes = (diffInMillies % (60 * 60 * 1000)) / (60 * 1000);

                                String timeLeft = String.format("%02d:%02d", hours, minutes);
                                binding.alarmTimeLeftText.setText("Alarm in : " + timeLeft);
                            }
                        });

                        sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        timeLeftThread.start();
    }

    private void errorUserToast(String text){
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new ForegroundColorSpan(Color.RED), 0, spannableString.length(), 0);
        Toast.makeText(context, spannableString, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        AuthorizationClient.stopLoginActivity(this, context.getResources().getInteger(R.integer.request_code));
        Log.i(TAG, "Closed !");
        super.onDestroy();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}