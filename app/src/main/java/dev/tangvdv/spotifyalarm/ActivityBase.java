package dev.tangvdv.spotifyalarm;

import androidx.appcompat.app.AppCompatActivity;

public class ActivityBase extends AppCompatActivity {
    private boolean isPaused = false;

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
}
