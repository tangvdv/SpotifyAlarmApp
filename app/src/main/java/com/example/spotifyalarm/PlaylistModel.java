package com.example.spotifyalarm;

public class PlaylistModel {
    private String id;
    private String name;
    private String playlist_uri;
    private String image_url;
    private String owner_name;

    public PlaylistModel(String id, String name, String playlist_uri, String image_url, String owner_name) {
        this.id = id;
        this.name = name;
        this.playlist_uri = playlist_uri;
        this.image_url = image_url;
        this.owner_name = owner_name;
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

    public String getPlaylistUri() {
        return playlist_uri;
    }

    public void setPlaylistUri(String playlist_uri) {
        this.playlist_uri = playlist_uri;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String getOwnerName() {
        return owner_name;
    }

    public void setOwnerName(String owner_name) {
        this.owner_name = owner_name;
    }
}
