package dev.tangvdv.spotifyalarm;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class AlarmWakeLock {
    private static final String TAG = "AlarmWakeLock";
    private static PowerManager.WakeLock wakeLock;

    private static LogFile logFile;

    static void acquireAlarmWakeLock(Context context) {
        Log.v(TAG, "Acquiring cpu wake lock");
        LogFile logFile = new LogFile(context);
        logFile.writeToFile(TAG, "Acquiring cpu wake lock");
        if (wakeLock != null) {
            return;
        }

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "SpotifyAlarm::AlarmWakeLock");
        wakeLock.acquire();
    }

    static void releaseAlarmWakeLock() {
        Log.v(TAG, "Releasing cpu wake lock");
        if(logFile != null) logFile.writeToFile(TAG, "Releasing cpu wake lock");
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
