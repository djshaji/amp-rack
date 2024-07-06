package com.shajikhan.ladspa.amprack;

import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

public class About extends AppCompatActivity {

    private ExoPlayer exoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);
        getSupportActionBar().hide();
        Button music = findViewById(R.id.music);

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse("asset:///track.mp3")));
        exoPlayer.prepare();
        exoPlayer.play();

        ToggleButton btn = findViewById (R.id.btn);
        btn.setChecked(true);
        btn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    exoPlayer.play();
                    buttonView.setBackground(getResources().getDrawable(R.drawable.ic_baseline_pause_24));
                } else {
                    exoPlayer.pause();
                    buttonView.setBackground(getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                }
            }
        });

        exoPlayer.setRepeatMode (Player.REPEAT_MODE_ONE);

        music.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://music.shaji.in"));
                startActivity(i);
            }
        });

        Button github = findViewById(R.id.github);
        github.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://github.com/djshaji"));
                startActivity(i);
            }
        });

        Button download = findViewById(R.id.download);
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://music.shaji.in/media/2024/rock%20and%20roll%20will%20never%20die_r4.mp3"));
                startActivity(i);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        exoPlayer.stop();
    }
}