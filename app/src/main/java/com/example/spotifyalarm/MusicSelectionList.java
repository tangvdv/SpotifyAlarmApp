package com.example.spotifyalarm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;
import java.util.Objects;

public class MusicSelectionList extends AppCompatActivity {

    private Context context;
    private ActivityMusicSelectionListBinding binding;
    private List<String> filterTypes;
    private List<MusicModel> musicModelList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_music_selection_list);
        super.onCreate(savedInstanceState);
        context = this;
        binding = ActivityMusicSelectionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.i("MusicSelectionList", "onCreate");

        filterTypes = new ArrayList<>(3);
        musicModelList = new ArrayList<>();

        bindingManager();
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
                onClickLibraryButton(musicModel.getId());
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

        String textType = musicModel.getType();
        if(!Objects.equals(musicModel.getType(), "Artist")){
            textType += " · " + musicModel.getOwnerName();
        }
        tv_type.setText(textType);

        rl.addView(tv_type);

        fl.addView(rl);

        return fl;
    }

    private void onClickLibraryButton(String id){
        Toast t = Toast.makeText(context, "Id : "+ id, Toast.LENGTH_SHORT);
        t.show();
    }
}