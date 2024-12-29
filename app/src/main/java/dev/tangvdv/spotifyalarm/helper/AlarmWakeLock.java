package dev.tangvdv.spotifyalarm.helper;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import dev.tangvdv.spotifyalarm.helper.LogFile;

public class AlarmWakeLock {
    private static final String TAG = "AlarmWakeLock";
    private static PowerManager.WakeLock wakeLock;

    public static void acquireAlarmWakeLock(Context context) {
        if (wakeLock != null) {
            return;
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "SpotifyAlarm::AlarmWakeLock");
        wakeLock.acquire();
        Log.v(TAG, "Acquiring cpu wake lock");
        LogFile logFile = new LogFile(context);
        logFile.separator();
        logFile.writeToFile(TAG, "Acquiring cpu wake lock");
    }

    public static void releaseAlarmWakeLock(Context context) {
        Log.v(TAG, "Releasing cpu wake lock");
        LogFile logFile = new LogFile(context);
        logFile.writeToFile(TAG, "Releasing cpu wake lock");
        logFile.separator();
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
