package dev.tangvdv.spotifyalarm.model;

import java.util.HashMap;

public class SettingsModel {
    private boolean repeat;
    private boolean shuffle;
    private int volume;
    private boolean loopMusic;

    public SettingsModel(HashMap<String, Object> settings){
        repeat = settings.containsKey("repeat") && (Boolean) settings.get("repeat");
        shuffle = settings.containsKey("shuffle") && (Boolean) settings.get("shuffle");
        volume = settings.containsKey("volume") ? (Integer) settings.get("volume") : 7;
        loopMusic = settings.containsKey("loopMusic") && (Boolean) settings.get("loopMusic");
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public boolean getLoopMusic() { return loopMusic; }

    public void setLoopMusic(boolean loopMusic) { this.loopMusic = loopMusic; }

    public HashMap<String, Object> getSettingsModelContent(){
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("repeat", this.repeat);
        map.put("shuffle", this.shuffle);
        map.put("volume", this.volume);
        map.put("loopMusic", this.loopMusic);

        return map;
    }
}
