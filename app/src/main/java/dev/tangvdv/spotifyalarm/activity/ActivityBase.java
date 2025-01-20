package dev.tangvdv.spotifyalarm.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import dev.tangvdv.spotifyalarm.helper.AlarmSharedPreferences;
import dev.tangvdv.spotifyalarm.helper.SpotifyRemoteHelper;

public class ActivityBase extends AppCompatActivity {
    private boolean isPaused = false;
    private final Context context = this;
    public interface PermissionResultCallback {
        void onResult(boolean hasAllPermissions);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void firstPermissionAndAuth(PermissionResultCallback callback){
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
            SpotifyRemoteHelper.spotifyAppRemoteConnection(context, null);
            hasAllPermissions = false;
        }

        callback.onResult(hasAllPermissions);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
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

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}
