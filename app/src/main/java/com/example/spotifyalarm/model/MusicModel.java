package com.example.spotifyalarm.model;

import java.util.HashMap;
import java.util.Objects;

public class MusicModel {
    private String id;
    private String name;
    private String music_uri;
    private String image_url;
    private String[] owner_name;
    private String type;

    public MusicModel(String id, String name, String playlist_uri, String image_url, String[] owner_name, String type) {
        this.id = id;
        this.name = name;
        this.music_uri = playlist_uri;
        this.image_url = image_url;
        this.owner_name = owner_name;
        this.type = type;
    }

    public String getId() { return this.id; }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMusicUri() {
        return music_uri;
    }

    public void setMusicUri(String music_uri) {
        this.music_uri = music_uri;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String[] getOwnerName() {
        return owner_name;
    }

    public void setOwnerName(String[] owner_name) {
        this.owner_name = owner_name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public HashMap<String, Object> getMusicModelContent(){
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("image",this.image_url);
        map.put("name",this.name);
        String type = this.type.substring(0, 1).toUpperCase() + this.type.substring(1).toLowerCase();
        if(!Objects.equals(this.type, "artist")){
            type = type.concat(" · " + String.join(", ", this.owner_name));
        }
        map.put("type", type);
        map.put("uri", this.music_uri);

        return map;
    }
}
