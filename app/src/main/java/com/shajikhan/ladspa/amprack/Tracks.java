package com.shajikhan.ladspa.amprack;

import static com.shajikhan.ladspa.amprack.MainActivity.context;

import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AuxEffectInfo;
import com.google.android.material.slider.Slider;

import java.io.File;

public class Tracks extends Fragment {
    MainActivity mainActivity;
    TracksAdapter tracksAdapter ;
    RecyclerView recyclerView ;
    ToggleButton playPause ;
    String TAG = getClass().getSimpleName();
    ExoPlayer player ;

    Tracks () {
        tracksAdapter = new TracksAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tracks, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        recyclerView = (RecyclerView) view.findViewById(R.id.tracks_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));
        recyclerView.setAdapter(tracksAdapter);
//        load (mainActivity.dir);

        player = new ExoPlayer.Builder(context).build();
        tracksAdapter.player = player;

        playPause = view.findViewById(R.id.tracks_play);
        tracksAdapter.tracks = this;
        playPause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!b) {
                    playPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                    player.pause();
                } else {
                    playPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_baseline_pause_24));
                    player.play();
                }

                player.setSkipSilenceEnabled(b);
            }
        });


        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                Log.d(TAG, "onPlayerStateChanged: " + playbackState);
                if (playbackState == Player.EVENT_PLAYBACK_STATE_CHANGED)
                    playPause.setChecked(false);

            }
        });

        Slider slider = view.findViewById(R.id.tracks_volume);
        slider.setValue(player.getVolume());
        slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                if (fromUser) {
                    player.setVolume(value);
                }
            }
        });

        Slider bpm = view.findViewById(R.id.tracks_bpm);
        bpm.setValueFrom(0.5f);
        bpm.setValueTo(1.5f);
        bpm.setValue(1.0f);
        bpm.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                Log.d(TAG, "onValueChange: " + value);
                PlaybackParameters param = new PlaybackParameters(value);
                player.setPlaybackParameters(param);
            }
        });

        Button resetBPM = view.findViewById(R.id.tracks_reset_bpm);
        resetBPM.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bpm.setValue(1.0f);
            }
        });

        CheckBox loop = view.findViewById(R.id.loop);
        loop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b)
                    player.setRepeatMode(Player.REPEAT_MODE_ONE);
                else
                    player.setRepeatMode(Player.REPEAT_MODE_OFF);
            }
        });

        boolean dynamicsProcessingEnabled = mainActivity.defaultSharedPreferences.getBoolean("tracks_fx", true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P && dynamicsProcessingEnabled) {
            // Cue CSI Miami music
            DynamicsProcessing.Config.Builder builder = new DynamicsProcessing.Config.Builder(
                    DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                    2,
                    false,
                    0,
                    true,
                    1,
                    false,
                    0,
                    true
            );

            DynamicsProcessing.Config config = null;
            config = builder.build();
            for (int i = 0 ; i < 2; i ++) {
                DynamicsProcessing.Mbc mbc = config.getMbcByChannelIndex(i);
                DynamicsProcessing.MbcBand mbcBand = mbc.getBand(0);
                mbcBand.setAttackTime(50);
                mbcBand.setReleaseTime(100);
                mbcBand.setRatio(4.0f);
                mbcBand.setThreshold(-50.0f);
                mbcBand.setKneeWidth(6);
                mbcBand.setNoiseGateThreshold(-70f);
                mbcBand.setExpanderRatio(6);
                mbcBand.setPreGain(0);
                mbcBand.setPostGain(0);
                DynamicsProcessing.Limiter limiter = config.getLimiterByChannelIndex(i);
                limiter.setAttackTime(25);
                limiter.setRatio(2);
                limiter.setThreshold(0);
                limiter.setReleaseTime(50);
            }

            DynamicsProcessing dynamicsProcessing = new DynamicsProcessing(0, player.getAudioSessionId(), config);
            player.setAuxEffectInfo(new AuxEffectInfo(dynamicsProcessing.getId(),1));
        }
    }

    public void load (String [] files) {
        for (int i = 0 ; i < files.length; i ++) {
//            File file = new File("assets:///drums/" + files [i]);
            tracksAdapter.add("asset:///drums/" + files [i]);
        }
    }

    public void load (File dir) {
        Log.d(TAG, "load: loading folder " + dir.getAbsolutePath());
        File [] files = dir.listFiles();
        Log.d(TAG, "load: " + files.length + " files found");
        for (int i = 0 ; i < files.length; i ++) {
            Log.d(TAG, "load: adding file " + files[i].getAbsolutePath());
            tracksAdapter.add(files [i].getAbsolutePath());
        }
    }
}