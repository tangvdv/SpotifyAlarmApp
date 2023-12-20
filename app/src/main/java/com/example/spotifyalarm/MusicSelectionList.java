package com.example.spotifyalarm;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.paris.Paris;
import com.bumptech.glide.Glide;
import com.example.spotifyalarm.databinding.ActivityMusicSelectionListBinding;
import com.github.siyamed.shapeimageview.ShapeImageView;
import com.github.siyamed.shapeimageview.mask.PorterShapeImageView;

public class MusicSelectionList extends AppCompatActivity {

    private Context context;
    private ActivityMusicSelectionListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_music_selection_list);
        super.onCreate(savedInstanceState);
        context = this;
        binding = ActivityMusicSelectionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.i("MusicSelectionList", "onCreate");

        bindingManager();

        createLibrary();
    }

    private void bindingManager(){
        binding.btnPlaylist.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            }
        });
    }

    private void createLibrary(){
        for (int i = 0; i < 10; i++) {
            FrameLayout fl = createLibraryButton(i);
            binding.libraryLayout.addView(fl);
        }
    }

    private FrameLayout createLibraryButton(int i){
        FrameLayout fl = new FrameLayout(context);
        Paris.style(fl).apply(R.style.library_frame_layout);
        fl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickLibraryButton(i);
            }
        });

        RelativeLayout rl = new RelativeLayout(context);
        rl.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        PorterShapeImageView imageView = new PorterShapeImageView(this);
        Paris.style(imageView).apply(R.style.library_image);
        Glide.with(context)
                .load("https://picsum.photos/200")
                .into(imageView);

        TextView tv_name = new TextView(context);
        Paris.style(tv_name).apply(R.style.library_text_item_name);
        tv_name.setText("Name " + i);

        TextView tv_type = new TextView(context);
        Paris.style(tv_type).apply(R.style.library_text_item_type);
        tv_type.setText("Type " + i);

        rl.addView(imageView);
        rl.addView(tv_name);
        rl.addView(tv_type);

        fl.addView(rl);

        return fl;
    }

    private void onClickLibraryButton(int id){
        Toast t = Toast.makeText(context, "Id : "+ id, Toast.LENGTH_SHORT);
        t.show();
    }
}