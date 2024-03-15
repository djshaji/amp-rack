package com.shajikhan.ladspa.amprack;

import static com.shajikhan.ladspa.amprack.MainActivity.context;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.NoiseSuppressor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AuxEffectInfo;
import com.google.android.exoplayer2.video.VideoSize;
import com.google.android.material.slider.Slider;

import java.io.File;

public class Tracks extends Fragment {
    MainActivity mainActivity;
    TracksAdapter tracksAdapter ;
    RecyclerView recyclerView ;
    ToggleButton playPause ;
    boolean themeInit = false;
    String TAG = getClass().getSimpleName();
    SurfaceView surfaceView;
    String filesDir ;
    ExoPlayer player ;
    static int requestCode = 1001 ;
    LinearLayout playerWindow ;
    boolean isDrums = false ;
    BitmapDrawable play, pause, reset ;

    public Tracks () {
        tracksAdapter = new TracksAdapter();
    }
    public Tracks (boolean _isDrums) {
        tracksAdapter = new TracksAdapter();
        isDrums = _isDrums;
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
        surfaceView = view.findViewById(R.id.tracks_video);
//        load (mainActivity.dir);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                player.setVideoSurface(surfaceView.getHolder().getSurface());

            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });

        LinearLayout shareTip = view.findViewById(R.id.share_tip);
        FrameLayout frameLayout = view.findViewById(R.id.vframe);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 1) {
                    shareTip.setVisibility(View.GONE);
                    recyclerView.setOnScrollListener(null);
                }
            }
        });

        player = new ExoPlayer.Builder(context).build();
        tracksAdapter.player = player;

        playPause = view.findViewById(R.id.tracks_play);
        tracksAdapter.tracks = this;
        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPause.setChecked(false);
            }
        });
        playPause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (getContext() == null) {
                    Log.d("context is null", "onCheckedChanged() called with: compoundButton = [" + compoundButton + "], b = [" + b + "]");
                    return;
                }

                if (!b) {
                    playPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_baseline_play_arrow_24));
                    player.pause();
                    frameLayout.setVisibility(View.GONE);
                    if (mainActivity.useTheme) {
                        if (pause != null)
                            playPause.setCompoundDrawables(pause, null, null, null);
                    }
                } else {
                    playPause.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_baseline_pause_24));
                    player.play();
                    if (player.getVideoFormat() != null) {
                        frameLayout.setVisibility(View.VISIBLE);
                  }

                    if (mainActivity.useTheme) {
                        if (pause != null)
                            playPause.setCompoundDrawables(play, null, null, null);

                    }
                }

//                player.setSkipSilenceEnabled(b);
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
                if (b) {
                    player.setRepeatMode(Player.REPEAT_MODE_ONE);
                }
                else {
                    player.setRepeatMode(Player.REPEAT_MODE_OFF);
                }
            }
        });

        /*

        CheckBox gapless = mainActivity.findViewById(R.id.gapless);
        gapless.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                player.setSkipSilenceEnabled(isChecked);
                player.setPauseAtEndOfMediaItems(!isChecked);
            }
        });

         */

        boolean dynamicsProcessingEnabled = mainActivity.defaultSharedPreferences.getBoolean("tracks_fx", true);
        /* commenting out because causes crashes on some devices

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

            if (player.getAudioSessionId() != C.AUDIO_SESSION_ID_UNSET) {
                DynamicsProcessing dynamicsProcessing = new DynamicsProcessing(0, player.getAudioSessionId(), config);
                player.setAuxEffectInfo(new AuxEffectInfo(dynamicsProcessing.getId(), 1));
            }
        }

         */

        Button loadFile = view.findViewById(R.id.load_file);
        loadFile.setPadding(10,10,10,10);

        ToggleButton skipSilence = view.findViewById(R.id.skip_silence);
        skipSilence.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tracksAdapter.player.setSkipSilenceEnabled(isChecked);
            }
        });

        playerWindow = view.findViewById(R.id.tracks_player);
        if (mainActivity.useTheme) {
            play = mainActivity.skinEngine.bitmapDrawable("icons", "play");
            pause = mainActivity.skinEngine.bitmapDrawable("icons", "pause");
            reset = mainActivity.skinEngine.bitmapDrawable("icons", "reset");
            if (play != null)
                playPause.setCompoundDrawables(play, null, null, null);


            if (reset != null)
                resetBPM.setBackground(reset);

            mainActivity.skinEngine.slider(slider);
            mainActivity.skinEngine.slider(bpm);

            mainActivity.skinEngine.button(loadFile, SkinEngine.Resize.Width, 1);
            mainActivity.skinEngine.card (view.findViewById(R.id.skip_silence_label));

            /*
            LinearLayout loadTrack = view.findViewById(R.id.add_track_ll);
            view.post(new Runnable() {
                @Override
                public void run() {
                    mainActivity.skinEngine.card(loadTrack);
                }
            });

             */
//            mainActivity.skinEngine.toggle(skipSilence, skipSilence.isChecked());
        }

        loadFile.setOnClickListener(new View.OnClickListener() {
            boolean finalD = isDrums;
            @Override
            public void onClick(View v) {
                Intent intent_upload = new Intent();
                intent_upload.setType("audio/*");
                intent_upload.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent_upload.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                intent_upload.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                if (finalD)
//                    getActivity().startActivityForResult(intent_upload,1001);
//                else
                getActivity().startActivityForResult(intent_upload,requestCode);
                requestCode ++ ;
            }
        });
    }

    public void load (String [] files) {
        filesDir = "assets://drums";
        for (int i = 0 ; i < files.length; i ++) {
//            File file = new File("assets:///drums/" + files [i]);
            tracksAdapter.add("asset:///drums/" + files [i]);
        }
    }

    public void load (File dir) {
        Log.d(TAG, "load: loading folder " + dir.getAbsolutePath());
        filesDir = dir.getPath() ;
        File [] files = dir.listFiles();
        Log.d(TAG, "load: " + files.length + " files found");
        for (int i = 0 ; i < files.length; i ++) {
            Log.d(TAG, "load: adding file " + files[i].getAbsolutePath());
            tracksAdapter.add(files [i].getAbsolutePath());
        }
    }

    public void load (Uri filename) {
        tracksAdapter.add(filename.toString());
    }
}