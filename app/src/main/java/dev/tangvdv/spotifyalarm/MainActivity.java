package dev.tangvdv.spotifyalarm;

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
import dev.tangvdv.spotifyalarm.databinding.ActivityMainBinding;
import dev.tangvdv.spotifyalarm.databinding.UserProfileDialogBinding;
import dev.tangvdv.spotifyalarm.model.AlarmModel;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationResponse;

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
    private Intent alarmServiceIntent;
    private SharedPreferences sharedPreferences;
    private int isSpotifyActivityConnected;

    private final ActivityResultLauncher<Intent> musicActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        setPlaylistLayout();
                    }
                    else if(result.getResultCode() == Activity.RESULT_CANCELED){
                        Intent data = result.getData();
                        if(data != null && data.getExtras() != null){
                            if(Objects.equals(data.getStringExtra("data"), "")){
                                errorUserToast("Error");
                            }
                            else{
                                errorUserToast(data.getStringExtra("Data"));
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

        context = this;

        isSpotifyActivityConnected = -1;

        sharedPreferences = this.getSharedPreferences("App", Context.MODE_PRIVATE);

        alarmServiceIntent = new Intent(this, AlarmManagerService.class);

        AlarmModel.getInstance().setAlarmModel(AlarmSharedPreferences.loadAlarm(context));

        setupActivityViews();
    }

    private void setupActivityViews(){
        if (isNetworkConnected() && isSpotifyActivityConnected == -1){
            startSpotifyActivity();
            isSpotifyActivityConnected = 0;
        }

        Thread setupThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.mainLayout.setVisibility(View.GONE);
                        binding.progressBar.setVisibility(View.VISIBLE);
                    }
                });

                while (isSpotifyActivityConnected == 0){
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.mainLayout.setVisibility(View.VISIBLE);
                        binding.progressBar.setVisibility(View.GONE);

                        getUserProfileData();
                        setPlaylistLayout();

                        bindingManager();

                        setCalendar();
                        alarmTextValue();
                        alarmBindingState();
                    }
                });
            }
        });

        setupThread.start();
    }

    private void startAlarmService(){
        if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.OFF){
            startService(alarmServiceIntent);
        }
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
                    startAlarmService();
                }
                else{
                    if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON){
                        AlarmModel.getInstance().setAlarmOff();
                        stopService(alarmServiceIntent);
                    }
                }

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        while(b != (AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON)) {

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

                        saveAlarm();
                    }
                };
                thread.start();
            }
        });

        if(isNetworkConnected() && isSpotifyActivityConnected == 1) {
            binding.btnMusicSelection.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, MusicLibraryActivity.class);
                    musicActivityResult.launch(intent);
                }
            });
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
        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginActivity(this, context.getResources().getInteger(R.integer.request_code) ,request);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == context.getResources().getInteger(R.integer.request_code)) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthorizationResponse.Type.TOKEN) {
                AlarmSharedPreferences.saveToken(context, response.getAccessToken());
                isSpotifyActivityConnected = 1;
            }
            else{
                isSpotifyActivityConnected = -1;
                Log.e(TAG, "Response error : "+response.getError());
                errorUserToast(context.getString(R.string.spotify_activity_error));
            }
        }
    }

    private void getUserProfileData(){
        HashMap<String, Object> user = AlarmSharedPreferences.loadUser(context);

        if(!user.isEmpty()){
            setUserProfileView(user);
        }
        else {
            if(isNetworkConnected() && isSpotifyActivityConnected == 1){
                SpotifyAPI spotifyAPI = new SpotifyAPI(context, AlarmSharedPreferences.loadToken(context));
                spotifyAPI.getUserProfile(new SpotifyAPI.SpotifyAPIUserProfileCallback() {
                    @Override
                    public void onSuccess(String name, String image_url) {
                        HashMap<String, Object> user = new HashMap<String, Object>();
                        user.put("image", image_url);
                        user.put("name", name);
                        AlarmSharedPreferences.saveUser(context, user);

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
                    }
                });
            }
        }
    }

    private void setUserProfileView(HashMap<String, Object> user){
        Glide.with(context)
                .load( (String) user.get("image"))
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

        binding.textUserProfile.setText( (String) user.get("name"));
        Glide.with(context)
                .load( (String) user.get("image"))
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
                    errorUserToast(context.getString(R.string.spotify_app_missing_error));
                }
            }
        });

        binding.btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userProfileDialog.dismiss();

                AuthorizationClient.clearCookies(context);
                AlarmSharedPreferences.clearSharedPreferences(context);
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
                finishAffinity();
            }
        });
    }

    private void setPlaylistLayout(){
        HashMap<String, Object> music = AlarmSharedPreferences.loadMusic(context);
        AlarmModel.getInstance().setPlaylist_uri( (String) music.get("uri") );
        if(!music.isEmpty()){
            binding.textPlaylistName.setText( (String) music.get("name"));
            binding.textPlaylistOwner.setText( (String) music.get("type"));
            Glide.with(context)
                    .load( (String) music.get("image"))
                    .apply(new RequestOptions()
                            .transform(new CenterCrop(), new RoundedCorners(10))
                    )
                    .into(binding.imagePlaylist);
        }
        else{
            binding.textPlaylistName.setText(context.getString(R.string.button_select_music));
        }
    }

    private void selectTime(){
        timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(AlarmModel.getInstance().getHour())
                .setMinute(AlarmModel.getInstance().getMinute())
                .setTitleText("Select Alarm Time")
                .setTheme(R.style.ThemeOverlay_App_MaterialTimePicker)
                .build();


        timePicker.show(getSupportFragmentManager(), "UwU");
        timePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlarmModel.getInstance().setHour(timePicker.getHour());
                AlarmModel.getInstance().setMinute(timePicker.getMinute());

                setCalendar();
                alarmTextValue();
                saveAlarm();

                if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON){
                    AlarmModel.getInstance().setAlarmOff();
                    startAlarmService();
                }
            }
        });
    }

    private void setCalendar(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, AlarmModel.getInstance().getHour());
        calendar.set(Calendar.MINUTE, AlarmModel.getInstance().getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if(calendar.getTimeInMillis() < System.currentTimeMillis()){
            calendar.add(Calendar.DATE, 1);
        }

        AlarmModel.getInstance().setCalendar(calendar);
    }

    private void alarmTextValue(){
        if(AlarmModel.getInstance().getCalendar() != null) {
            String formattedHour = String.format("%02d", AlarmModel.getInstance().getHour());
            String formattedMinute = String.format("%02d", AlarmModel.getInstance().getMinute());
            binding.alarmTimeText.setText(String.format("%s:%s", formattedHour, formattedMinute));
            binding.alarmDateText.setText(new SimpleDateFormat("MMMM dd").format(AlarmModel.getInstance().getCalendar().getTime()));
        }else{
            binding.alarmTimeText.setVisibility(View.GONE);
            binding.alarmDateText.setVisibility(View.GONE);
        }
    }

    private void alarmBindingState(){
        binding.btnMusicSelection.setClickable(isNetworkConnected());
        binding.setAlarmSwitch.setChecked(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON);
        if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON){
            binding.alarmTimeText.setTextColor(getResources().getColor(R.color.white));
            binding.alarmTimeLeftText.setVisibility(View.VISIBLE);
            alarmTimeLeftThread();
        }
        else{
            binding.alarmTimeText.setTextColor(getResources().getColor(R.color.light_grey));
            binding.alarmTimeLeftText.setVisibility(View.GONE);
        }
    }

    private void alarmTimeLeftThread(){
        Thread timeLeftThread = new Thread() {
            @Override
            public void run() {
                while(!MainActivity.this.isDestroyed() && AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON) {
                    try {
                        Log.i(TAG, "Tick");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long diffInMillies = Math.abs(Calendar.getInstance().getTimeInMillis() - AlarmModel.getInstance().getCalendar().getTimeInMillis() );

                                long hours = diffInMillies / (60 * 60 * 1000);
                                long minutes = (diffInMillies % (60 * 60 * 1000)) / (60 * 1000);

                                String timeLeft = String.format("%02d:%02d", hours, minutes);
                                binding.alarmTimeLeftText.setText(context.getString(R.string.alarm_time_left) + " : " + timeLeft);
                            }
                        });

                        sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.OFF){
                            alarmBindingState();
                        }
                    }
                });
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
        saveAlarm();
        super.onDestroy();
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void saveAlarm(){
        AlarmSharedPreferences.saveAlarm(context, AlarmModel.getInstance().getAlarmModelContent());
    }
}