package dev.tangvdv.spotifyalarm;

import android.os.Handler;
import android.util.Log;

import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.PlayerState;

import dev.tangvdv.spotifyalarm.model.AlarmModel;

public class AlarmState {
    public interface AlarmStateCallback{
        void onCompletion(boolean isPlaying);
    }

    public enum State {
        PLAY,
        PAUSE
    }

    private Handler handler;

    public void getAlarmState(State state, dev.tangvdv.spotifyalarm.AlarmState.AlarmStateCallback callback){
        if(AlarmModel.getInstance().getCurrentType() != null){
            handler = new Handler();
            handler.postDelayed(new Runnable() {
                final Runnable r = this;
                @Override
                public void run() {
                    Log.i("AlarmState", "Checking Alarm State.");
                    if(AlarmModel.getInstance().getCurrentType() == AlarmModel.Type.BACKUP){
                        boolean isPlaying = AlarmModel.getInstance().getBackupAlarmRingtone().isPlaying();
                        if(state == State.PLAY){
                            if(isPlaying){
                                callback.onCompletion(true);
                            }
                            else{
                                handler.postDelayed(r, 1000);
                            }
                        }
                        else if (state == State.PAUSE){
                            if(!isPlaying){
                                callback.onCompletion(false);
                            }
                            else{
                                handler.postDelayed(r, 1000);
                            }
                        }
                    }
                    else if (AlarmModel.getInstance().getCurrentType() == AlarmModel.Type.SPOTIFY){
                        AlarmModel.getInstance().getSpotifyAppRemote().getPlayerApi().getPlayerState().setResultCallback(new CallResult.ResultCallback<PlayerState>() {
                            @Override
                            public void onResult(PlayerState playerState) {
                                if(state == State.PLAY){
                                    if(!playerState.isPaused){
                                        callback.onCompletion(true);
                                    }
                                    else{
                                        handler.postDelayed(r, 1000);
                                    }
                                }
                                else if (state == State.PAUSE){
                                    if(playerState.isPaused){
                                        callback.onCompletion(false);
                                    }
                                    else{
                                        handler.postDelayed(r, 1000);
                                    }
                                }
                            }
                        });
                    }

                }
            }, 0);
        }
    }


}
