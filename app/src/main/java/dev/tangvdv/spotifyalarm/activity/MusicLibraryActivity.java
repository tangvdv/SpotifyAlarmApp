package dev.tangvdv.spotifyalarm.activity;

import androidx.annotation.Nullable;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.airbnb.paris.Paris;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import dev.tangvdv.spotifyalarm.helper.AlarmSharedPreferences;
import dev.tangvdv.spotifyalarm.R;
import dev.tangvdv.spotifyalarm.helper.LogFile;
import dev.tangvdv.spotifyalarm.helper.SpotifyAPI;
import dev.tangvdv.spotifyalarm.helper.SpotifyAuthHelper;
import dev.tangvdv.spotifyalarm.databinding.ActivityLibraryBinding;
import dev.tangvdv.spotifyalarm.model.MusicModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MusicLibraryActivity extends ActivityBase implements SpotifyAuthHelper.SpotifyAuthCallback {
    private static final String TAG = "MusicLibraryActivity";
    private Context context;
    private ActivityLibraryBinding binding;
    private List<String> filterTypes;
    private List<MusicModel> musicModelList;
    private State playlistRequest, albumRequest, artistRequest;
    private SpotifyAPI spotifyAPI;
    private SpotifyAuthHelper spotifyAuthHelper;
    private LogFile logFile;
    private enum State {
        SUCCEED,
        FAILED,
        IN_PROGRESS
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_library);
        super.onCreate(savedInstanceState);
        context = this;
        binding = ActivityLibraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        logFile = new LogFile(this);

        if(!isNetworkConnected()){
            setResultActivity(Activity.RESULT_CANCELED, context.getString(R.string.network_error));
        }
        else{
            filterTypes = new ArrayList<>(3);
            musicModelList = new ArrayList<>();

            spotifyAuthHelper = new SpotifyAuthHelper(this, this);
            spotifyAuthHelper.startSpotifyActivity(this);
        }
    }

    @Override
    public void onSpotifyConnected() {
        spotifyAPI = new SpotifyAPI(this, AlarmSharedPreferences.loadCode(context));
        spotifyAPI.getUserToken(new SpotifyAPI.SpotifyAPIAuthCallback() {
            @Override
            public void onSuccess(String token) {
                getLibrary();
            }

            @Override
            public void onError(String error) {
                setResultActivity(Activity.RESULT_CANCELED, context.getString(R.string.token_error));
            }
        });
        bindingManager();
    }

    @Override
    public void onSpotifyConnectionError(String error) {
        logFile.writeToFile(TAG, error);
        setResultActivity(Activity.RESULT_CANCELED, error);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        spotifyAuthHelper.handlerActivityResult(requestCode, resultCode, data);
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

        binding.btnRefreshError.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.errorLayout.setVisibility(View.GONE);
                getLibrary();
            }
        });

        binding.btnRefreshEmpty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.emptyLayout.setVisibility(View.GONE);
                getLibrary();
            }
        });
    }

    private void getLibrary(){
        Handler handler = new Handler();

        getUserPlaylists();
        getUserAlbums();
        getUserArtists();

        binding.progressBar.setVisibility(View.VISIBLE);

        handler.post(new Runnable() {
            @Override
            public void run() {
                if(
                        playlistRequest == State.IN_PROGRESS ||
                        albumRequest == State.IN_PROGRESS ||
                        artistRequest == State.IN_PROGRESS
                ){
                    handler.postDelayed(this, 1000);
                }
                else{
                    binding.progressBar.setVisibility(View.GONE);
                    if(
                            playlistRequest == State.SUCCEED ||
                            albumRequest == State.SUCCEED ||
                            artistRequest == State.SUCCEED
                    ){
                        if(musicModelList.isEmpty()){
                            binding.emptyLayout.setVisibility(View.VISIBLE);
                        }
                        else{
                            applyFilter();
                        }
                    }
                    else {
                        binding.errorLayout.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    private void updateLibrary(List<MusicModel> musicModelList){
        binding.libraryLayout.removeAllViews();
        if(musicModelList.size() > 0){
            musicModelList.forEach((musicModel -> {
                binding.libraryLayout.addView(createMusicButton(musicModel));
            }));
        }
    }

    private void getUserPlaylists(){
        playlistRequest = State.IN_PROGRESS;
        spotifyAPI.getUserPlaylists(new SpotifyAPI.SpotifyAPICallback() {
            @Override
            public void onSuccess(List<MusicModel> list) {
                if(list != null && !list.isEmpty()){
                    musicModelList.addAll(list);
                }
                playlistRequest = State.SUCCEED;
            }
            @Override
            public void onError(String error) {
                playlistRequest = State.FAILED;
                logFile.writeToFile(TAG, error);
            }
        });
    }

    private void getUserAlbums(){
        albumRequest = State.IN_PROGRESS;
        spotifyAPI.getUserAlbums(new SpotifyAPI.SpotifyAPICallback() {
            @Override
            public void onSuccess(List<MusicModel> list) {
                if(list != null && !list.isEmpty()){
                    musicModelList.addAll(list);
                }
                albumRequest = State.SUCCEED;
            }
            @Override
            public void onError(String error) {
                albumRequest = State.FAILED;
                logFile.writeToFile(TAG, error);
            }
        });
    }

    private void getUserArtists(){
        artistRequest = State.IN_PROGRESS;
        spotifyAPI.getUserArtists(new SpotifyAPI.SpotifyAPICallback() {
            @Override
            public void onSuccess(List<MusicModel> list) {
                if(list != null && !list.isEmpty()){
                    musicModelList.addAll(list);
                }
                artistRequest = State.SUCCEED;
            }
            @Override
            public void onError(String error) {
                artistRequest = State.FAILED;
                logFile.writeToFile(TAG, error);
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

    private FrameLayout createMusicButton(MusicModel musicModel){
        FrameLayout fl = new FrameLayout(context);
        Paris.style(fl).apply(R.style.library_frame_layout);
        fl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickLibraryButton(musicModel);
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

        rl.addView(tv_name);

        TextView tv_type = new TextView(context);
        Paris.style(tv_type).apply(R.style.library_text_item_type);

        rl.addView(tv_type);

        fl.addView(rl);

        tv_name.post(new Runnable() {
            @Override
            public void run() {
                String musicName = ellipsize(musicModel.getName(), tv_name.getWidth()-50, tv_name.getTextSize());

                tv_name.setText(musicName);
            }
        });

        tv_type.post(new Runnable() {
            @Override
            public void run() {
                String textType = musicModel.getType().substring(0, 1).toUpperCase() + musicModel.getType().substring(1).toLowerCase();
                if(!Objects.equals(musicModel.getType(), "artist")){
                    textType = textType.concat(" · " + String.join(", ", musicModel.getOwnerName()));
                }

                String musicType = ellipsize(textType, tv_type.getWidth(), tv_type.getTextSize());

                tv_type.setText(musicType);
            }
        });

        return fl;
    }

    private void onClickLibraryButton(MusicModel musicModel){
        AlarmSharedPreferences.saveMusic(context, musicModel.getMusicModelContent());
        setResultActivity(Activity.RESULT_OK, "");
    }

    private void setResultActivity(int type, String message){
        Intent resultIntent = new Intent();
        resultIntent.putExtra("Data", message);
        setResult(type, resultIntent);

        finish();
    }
}