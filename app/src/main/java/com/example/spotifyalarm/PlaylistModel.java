package com.example.spotifyalarm;

public class PlaylistModel {
    private int id;
    private String name;
    private String spotify_id;
    private String image_url;

    public PlaylistModel(int id, String name, String spotify_id, String image_url) {
        this.id = id;
        this.name = name;
        this.spotify_id = spotify_id;
        this.image_url = image_url;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpotifyId() {
        return spotify_id;
    }

    public void setSpotifyId(String spotify_id) {
        this.spotify_id = spotify_id;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }
}
