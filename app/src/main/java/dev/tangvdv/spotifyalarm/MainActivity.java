package dev.tangvdv.spotifyalarm;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
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
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.sdk.android.auth.AuthorizationClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends ActivityBase implements SpotifyAuthHelper.SpotifyAuthCallback {
    private static final String TAG = "MainActivity";

    private Context context;
    private ActivityMainBinding binding;
    private Dialog userProfileDialog;
    private MaterialTimePicker timePicker;
    private Intent alarmServiceIntent;
    private boolean isSpotifyActivityConnected;
    private AlarmManager alarmManager;
    private PendingIntent alarmPendingIntent;
    private SpotifyAuthHelper spotifyAuthHelper;
    private interface PermissionResultCallback {
        void onResult(boolean hasAllPermissions);
    }

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
        SpotifyAuthHelper.SpotifyAuthCallback spotifyAuthCallback = this;
        Activity activity = this;

        isSpotifyActivityConnected = false;

        try{
            LocalBroadcastManager.getInstance(this).registerReceiver(alarmServiceReceiver, new IntentFilter("intentAlarmKey"));

            alarmServiceIntent = new Intent(this, AlarmManagerService.class);

            AlarmModel.getInstance().setAlarmModel(AlarmSharedPreferences.loadAlarm(context));

            alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            // CHECK CURRENT ALARM
            alarmPendingIntent = getAlarmPendingIntent();
            if (alarmPendingIntent != null) {
                if(Objects.equals(alarmPendingIntent.getCreatorPackage(), context.getPackageName())){
                    AlarmModel.getInstance().setAlarmOn();
                    Log.v(TAG, "An alarm is active. ");
                }
            } else {
                AlarmModel.getInstance().setAlarmOff();
                Log.e(TAG, "No active alarm found.");
            }

            if (isNetworkConnected()){
                binding.mainLayout.setVisibility(View.GONE);
                binding.progressBar.setVisibility(View.VISIBLE);

                firstPermissionAndAuth(new PermissionResultCallback() {
                    @Override
                    public void onResult(boolean hasAllPermissions) {
                        spotifyAuthHelper = new SpotifyAuthHelper(context, spotifyAuthCallback);

                        spotifyAuthHelper.startSpotifyActivity(activity);
                    }
                });
            }
            else{
                setupActivityViews();
            }
        }
        catch (Exception e){
            LogFile logFile = new LogFile(context);
            logFile.writeToFile("AlarmReceiver", Objects.requireNonNull(e.getMessage()));
            Log.e("AlarmReceiver", Objects.requireNonNull(e.getMessage()));
        }
    }

    @Override
    public void onSpotifyConnected(String token) {
        isSpotifyActivityConnected = true;
        setupActivityViews();
    }

    @Override
    public void onSpotifyConnectionError(String error) {
        setupActivityViews();
        errorUserToast(error);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        spotifyAuthHelper.handlerActivityResult(requestCode, resultCode, data);
    }

    private void setupActivityViews(){
        binding.mainLayout.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);

        getUserProfileData();
        setPlaylistLayout();

        bindingManager();

        alarmTextValue();
        alarmBindingState();
    }

    private void startAlarmService(){
        if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.OFF){
            startForegroundService(alarmServiceIntent);
        }
    }

    private final BroadcastReceiver alarmServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            alarmBindingState();
            alarmTextValue();
        }
    };

    private PendingIntent getAlarmPendingIntent(){
        Intent intent = new Intent(context, AlarmReceiver.class);
        alarmPendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        return alarmPendingIntent;
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
                firstPermissionAndAuth(new PermissionResultCallback() {
                    @Override
                    public void onResult(boolean hasAllPermissions) {
                        if(hasAllPermissions){
                            if(b){
                                startAlarmService();
                            }
                            else{
                                if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON){
                                    AlarmModel.getInstance().setAlarmOff();
                                    alarmPendingIntent = getAlarmPendingIntent();
                                    if(alarmPendingIntent != null){
                                        alarmManager.cancel(alarmPendingIntent);
                                    }
                                    stopService(alarmServiceIntent);
                                }
                            }
                        }
                        else{
                            alarmBindingState();
                        }
                    }
                });
            }
        });

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

        if(isSpotifyActivityConnected){
            binding.btnMusicSelection.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(context, MusicLibraryActivity.class);
                    musicActivityResult.launch(intent);
                }
            });
        }
    }

    private void getUserProfileData(){
        HashMap<String, Object> user = AlarmSharedPreferences.loadUser(context);

        if(!user.isEmpty()){
            setUserProfileView(user);
        }
        else {
            if(isSpotifyActivityConnected){
                SpotifyAPI spotifyAPI = new SpotifyAPI(context, AlarmSharedPreferences.loadToken(context));
                spotifyAPI.getUserProfile(new SpotifyAPI.SpotifyAPIUserProfileCallback() {
                    @Override
                    public void onSuccess(String name, String image_url) {
                        HashMap<String, Object> user = new HashMap<String, Object>();
                        user.put("image", image_url);
                        user.put("name", name);
                        AlarmSharedPreferences.saveUser(context, user);

                        setUserProfileView(user);
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
        if(user.get("image") == null){
            Glide.with(context)
                    .load(R.drawable.default_profile_picture)
                    .apply(new RequestOptions()
                            .transform(new CenterCrop(), new CircleCrop())
                    )
                    .into(binding.imageUserProfile);

            binding.imageUserProfile.setColorFilter(getColor(R.color.light_grey), PorterDuff.Mode.SRC_IN);
        }
        else{
            Glide.with(context)
                    .load( (String) user.get("image"))
                    .apply(new RequestOptions()
                            .transform(new CenterCrop(), new CircleCrop())
                    )
                    .into(binding.imageUserProfile);
        }

        userProfileDialog = new Dialog(context);
        userProfileDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        userProfileDialog.setCancelable(true);
        userProfileDialog.setContentView(R.layout.user_profile_dialog);

        UserProfileDialogBinding binding = UserProfileDialogBinding.inflate(LayoutInflater.from(context));
        userProfileDialog.setContentView(binding.getRoot());

        binding.textUserProfile.setText( (String) user.get("name"));

        if(user.get("image") == null){
            Glide.with(context)
                    .load(R.drawable.default_profile_picture)
                    .apply(new RequestOptions()
                            .transform(new CenterCrop(), new CircleCrop())
                    )
                    .into(binding.imageUserProfile);

            binding.imageUserProfile.setColorFilter(getColor(R.color.light_grey), PorterDuff.Mode.SRC_IN);
        }
        else{
            Glide.with(context)
                    .load( (String) user.get("image"))
                    .apply(new RequestOptions()
                            .transform(new CenterCrop(), new CircleCrop())
                    )
                    .into(binding.imageUserProfile);
        }

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
            binding.textPlaylistName.post(new Runnable() {
                @Override
                public void run() {
                    String playlistName = ellipsize((String) music.get("name"), binding.textPlaylistName.getWidth(), binding.textPlaylistName.getTextSize());

                    binding.textPlaylistName.setText(playlistName);
                }
            });

            binding.textPlaylistOwner.post(new Runnable() {
                @Override
                public void run() {
                    String playlistOwner = ellipsize((String) music.get("type"), binding.textPlaylistOwner.getWidth(), binding.textPlaylistOwner.getTextSize());

                    binding.textPlaylistOwner.setText(playlistOwner);
                }
            });

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

                if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON){
                    AlarmModel.getInstance().setAlarmOff();
                    startAlarmService();
                }

                alarmTextValue();
            }
        });
    }

    private void alarmTextValue(){
        Date date = AlarmModel.getInstance().getCalendar().getTime();
        String formattedHour = String.format("%02d", AlarmModel.getInstance().getHour());
        String formattedMinute = String.format("%02d", AlarmModel.getInstance().getMinute());
        binding.alarmTimeText.setText(String.format("%s:%s", formattedHour, formattedMinute));
        binding.alarmDateText.setText(new SimpleDateFormat("MMMM dd").format(date));
    }

    private void alarmBindingState(){
        binding.btnMusicSelection.setClickable(isNetworkConnected());
        binding.setAlarmSwitch.setChecked(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON);
        if(AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON){
            binding.alarmTimeText.setTextColor(getResources().getColor(R.color.white));
            binding.alarmTimeLeftText.setVisibility(View.VISIBLE);
            alarmTimeLeftHandler();
        }
        else{
            binding.alarmTimeText.setTextColor(getResources().getColor(R.color.light_grey));
            binding.alarmTimeLeftText.setVisibility(View.GONE);
        }
    }

    private void alarmTimeLeftHandler(){
        Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                if(!isPaused() && AlarmModel.getInstance().getCurrentState() == AlarmModel.State.ON) {
                    long diffInMillies = Math.abs(Calendar.getInstance().getTimeInMillis() - AlarmModel.getInstance().getCalendar().getTimeInMillis() );

                    long hours = diffInMillies / (60 * 60 * 1000);
                    long minutes = (diffInMillies % (60 * 60 * 1000)) / (60 * 1000);

                    String timeLeft = String.format("%02d:%02d", hours, minutes);
                    binding.alarmTimeLeftText.setText(context.getString(R.string.alarm_time_left) + " : " + timeLeft);

                    handler.postDelayed(this, 1000);
                }
            }
        });
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

    private void firstPermissionAndAuth(PermissionResultCallback callback){
        boolean hasAllPermissions = true;
        if(!AlarmSharedPreferences.isOverlayPermissionGranted(context)){
            requestOverlayPermission();
            hasAllPermissions = false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(!AlarmSharedPreferences.isNotificationPermissionGranted(context)){
                requestNotificationPermission();
                hasAllPermissions = false;
            }
        }

        if(!AlarmSharedPreferences.isAuthSpotify(context)){
            spotifyAppRemoteConnection();
            hasAllPermissions = false;
        }

        callback.onResult(hasAllPermissions);
    }

    private void spotifyAppRemoteConnection(){
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(this.getString(R.string.client_id))
                        .setRedirectUri(this.getString(R.string.redirect_uri))
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                AlarmSharedPreferences.saveAuthSpotify(context, true);
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e(TAG, throwable.getMessage(), throwable);
            }
        });
    }

    private void requestNotificationPermission() {
       if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
        else if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED){
            AlarmSharedPreferences.saveNotificationPermission(context, true);
        }
    }

    private void requestOverlayPermission() {
        if (!Settings.canDrawOverlays(context)) {
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
            overlayPermissionLauncher.launch(overlayIntent);
        }
        else{
            AlarmSharedPreferences.saveOverlayPermission(context, true);
        }
    }

    private final ActivityResultLauncher<Intent> overlayPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Settings.canDrawOverlays(context)) {
                    AlarmSharedPreferences.saveOverlayPermission(context, true);
                } else {
                    AlarmSharedPreferences.saveOverlayPermission(context, false);
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (!isGranted) {
                    AlarmSharedPreferences.saveNotificationPermission(context, isGranted);
                }
            }
    );

    public String ellipsize(String input, float maxWidth, float textSize) {
        if (input == null)
            return null;

        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(textSize);

        CharSequence ellipsized = TextUtils.ellipsize(input, textPaint, maxWidth, TextUtils.TruncateAt.END);
        return ellipsized.toString();
    }
}