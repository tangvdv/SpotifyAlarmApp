package dev.tangvdv.spotifyalarm;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
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
import dev.tangvdv.spotifyalarm.databinding.ActivityLibraryBinding;
import dev.tangvdv.spotifyalarm.model.MusicModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MusicLibraryActivity extends AppCompatActivity implements SpotifyAuthHelper.SpotifyAuthCallback {
    private static final String TAG = "MusicLibraryActivity";
    private Context context;

    private ActivityLibraryBinding binding;
    private List<String> filterTypes;
    private List<MusicModel> musicModelList;
    private int fetchedPlaylist, fetchedAlbum, fetchedArtist = -1;
    private boolean errorResponse = true;
    private String token;
    private SpotifyAPI spotifyAPI;
    private SpotifyAuthHelper spotifyAuthHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_library);
        super.onCreate(savedInstanceState);
        context = this;
        binding = ActivityLibraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
    public void onSpotifyConnected(String token) {
        getLibrary(token);
        bindingManager();
    }

    @Override
    public void onSpotifyConnectionError(String error) {
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

        binding.btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.errorLayout.setVisibility(View.GONE);
                fetchedAlbum = -1;
                fetchedPlaylist = -1;
                fetchedArtist = -1;
                getLibrary(token);
            }
        });
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

                while(fetchedPlaylist==-1 || fetchedAlbum==-1 || fetchedArtist==-1) {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        binding.progressBar.setVisibility(View.GONE);
                        if(!errorResponse){
                            applyFilter();
                        }
                        else {
                            binding.errorLayout.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        };
        thread.start();
    }

    private void updateLibrary(List<MusicModel> musicModelList){
        binding.libraryLayout.removeAllViews();
        if(musicModelList.size() > 0){
            musicModelList.forEach((musicModel -> {
                binding.libraryLayout.addView(createMusicButton(musicModel));
            }));
        }
    }

    private void getUserPlaylists(String token){
        spotifyAPI = new SpotifyAPI(this, token);
        spotifyAPI.getUserPlaylists(new SpotifyAPI.SpotifyAPICallback() {
            @Override
            public void onSuccess(List<MusicModel> list) {
                if(list != null && !list.isEmpty()){
                    musicModelList.addAll(list);
                    fetchedPlaylist = 200;
                    errorResponse = false;
                }
            }
            @Override
            public void onError(String error) {
                fetchedPlaylist = 404;
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
                    fetchedAlbum = 200;
                    errorResponse = false;
                }
            }
            @Override
            public void onError(String error) {
                fetchedAlbum = 404;
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
                    fetchedArtist = 200;
                    errorResponse = false;
                }
            }
            @Override
            public void onError(String error) {
                fetchedArtist = 404;
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

    public String ellipsize(String input, float maxWidth, float textSize) {
        if (input == null)
            return null;

        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(textSize);

        CharSequence ellipsized = TextUtils.ellipsize(input, textPaint, maxWidth, TextUtils.TruncateAt.END);
        return ellipsized.toString();
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

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}