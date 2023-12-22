package com.example.spotifyalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.paris.Paris;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.spotifyalarm.databinding.ActivityMusicSelectionListBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MusicSelectionList extends AppCompatActivity {
    private static final String TAG = "MusicSelectionList";

    private Context context;
    private ActivityMusicSelectionListBinding binding;
    private List<String> filterTypes;
    private List<MusicModel> musicModelList;
    private boolean fetchedPlaylist, fetchedAlbum, fetchedArtist = false;
    private SpotifyAPI spotifyAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_music_selection_list);
        super.onCreate(savedInstanceState);
        context = this;
        binding = ActivityMusicSelectionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.i(TAG, "onCreate");

        filterTypes = new ArrayList<>(3);
        musicModelList = new ArrayList<>();

        SharedPreferences sharedPreferences = this.getSharedPreferences("Settings", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("TOKEN", null);
        if(token != null){
           getLibrary(token);
        }
        else{
            Log.e(TAG, "TOKEN is null");
        }

        bindingManager();
    }

    private void getLibrary(String token){
        Thread thread = new Thread() {
            @Override
            public void run() {
                getUserPlaylists(token);
                getUserAlbums(token);
                getUserArtists(token);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.progressBar.setVisibility(View.VISIBLE);
                    }
                });

                while(!fetchedPlaylist || !fetchedAlbum || !fetchedArtist) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                Log.i(TAG, "Data fetched");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.progressBar.setVisibility(View.GONE);
                        applyFilter();
                    }
                });
            }
        };
        thread.start();
    }

    private void getUserPlaylists(String token){
        spotifyAPI = new SpotifyAPI(this, token);
        spotifyAPI.getUserPlaylists(new SpotifyAPI.SpotifyAPICallback() {
            @Override
            public void onSuccess(List<MusicModel> list) {
                if(list != null && !list.isEmpty()){
                    musicModelList.addAll(list);
                    fetchedPlaylist = true;
                }
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "GetUserPlaylist : " + error);
            }
        });
    }

    private void getUserAlbums(String token){
        spotifyAPI = new SpotifyAPI(this, token);
        spotifyAPI.getUserAlbums(new SpotifyAPI.SpotifyAPICallback() {
            @Override
            public void onSuccess(List<MusicModel> list) {
                if(list != null && !list.isEmpty()){
                    musicModelList.addAll(list);
                    fetchedAlbum = true;
                }
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "GetUserAlbums : " + error);
            }
        });
    }

    private void getUserArtists(String token){
        spotifyAPI = new SpotifyAPI(this, token);
        spotifyAPI.getUserArtists(new SpotifyAPI.SpotifyAPICallback() {
            @Override
            public void onSuccess(List<MusicModel> list) {
                if(list != null && !list.isEmpty()){
                    musicModelList.addAll(list);
                    fetchedArtist = true;
                }
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "GetUserArtists : " + error);
            }
        });
    }

    private void bindingManager(){
        binding.btnPlaylist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                updateFilterList("playlist", isChecked);
            }
        });
        binding.btnAlbum.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                updateFilterList("album", isChecked);
            }
        });
        binding.btnArtist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                updateFilterList("artist", isChecked);
            }
        });
    }

    private void updateFilterList(String type, boolean state){
        if(state){
            if(!filterTypes.contains(type)){
                filterTypes.add(type);
            }
        }
        else{
            if(filterTypes.contains(type)){
                filterTypes.remove(type);
            }
        }

        applyFilter();
    }

    private void applyFilter(){
        if(musicModelList != null){
            if(filterTypes.size() > 0) {
                List<MusicModel> list = new ArrayList<>();

                musicModelList.forEach((musicModel -> {
                    if(filterTypes.contains(musicModel.getType())){
                        list.add(musicModel);
                    }
                }));

                updateLibrary(list);
            }
            else{
                updateLibrary(musicModelList);
            }
        }
    }

    private void updateLibrary(List<MusicModel> musicModelList){
        binding.libraryLayout.removeAllViews();
        if(musicModelList.size() > 0){
            musicModelList.forEach((musicModel -> {
                binding.libraryLayout.addView(createMusicButton(musicModel));
            }));
        }
    }

    private FrameLayout createMusicButton(MusicModel musicModel){
        FrameLayout fl = new FrameLayout(context);
        Paris.style(fl).apply(R.style.library_frame_layout);
        fl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickLibraryButton(musicModel.getMusicUri());
            }
        });

        RelativeLayout rl = new RelativeLayout(context);
        rl.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView imageView = new ImageView(this);
        Paris.style(imageView).apply(R.style.library_image);

        Glide.with(context)
                .load(musicModel.getImage_url())
                .apply(new RequestOptions()
                        .transform(new CenterCrop(), new RoundedCorners(10))
                )
                .into(imageView);
        rl.addView(imageView);

        TextView tv_name = new TextView(context);
        Paris.style(tv_name).apply(R.style.library_text_item_name);
        tv_name.setText(musicModel.getName());
        rl.addView(tv_name);

        TextView tv_type = new TextView(context);
        Paris.style(tv_type).apply(R.style.library_text_item_type);

        String textType = musicModel.getType().substring(0, 1).toUpperCase() + musicModel.getType().substring(1).toLowerCase();
        if(!Objects.equals(musicModel.getType(), "artist")){
            textType = textType.concat(" · " + String.join(", ", musicModel.getOwnerName()));
        }
        tv_type.setText(textType);

        rl.addView(tv_type);

        fl.addView(rl);

        return fl;
    }

    private void onClickLibraryButton(String uri){
        Toast t = Toast.makeText(context, "uri : "+ uri, Toast.LENGTH_SHORT);
        t.show();
    }
}