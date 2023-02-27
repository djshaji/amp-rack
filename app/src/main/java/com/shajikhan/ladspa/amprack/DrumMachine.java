package com.shajikhan.ladspa.amprack;

import static com.shajikhan.ladspa.amprack.MainActivity.context;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;

import java.io.IOException;
import java.util.ArrayList;

public class DrumMachine extends LinearLayout {
    Activity mainActivity ;
    Context context ;
    static int current = 0 ;
    String TAG = getClass().getSimpleName();
    ArrayList <ExoPlayer> players = new ArrayList<>();
    LinearLayout drumsLayout ;
    LinearLayout canvas ;
    String dir = "drumkits/default/" ;
    ToggleButton playButton ;
    int bpm = 100 ;
    SkinEngine skinEngine ;
    DisplayMetrics displayMetrics ;
    String [] samples ;
    int cols = 8 ;
    ArrayList <ArrayList<String>> playlist = new ArrayList<>();
    ArrayList <ArrayList<ToggleButton>> ticks = new ArrayList<>();

    public void setMainActivity(Activity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public DrumMachine(Context context) {
        super(context);
        this.context = context;
    }

    public void create () {
        for (int i = 0 ; i < cols ; i ++)
            players.add(new ExoPlayer.Builder(context).build());

        drumsLayout = (LinearLayout) mainActivity.getLayoutInflater().inflate(R.layout.drum_machine, null);
        addView(drumsLayout);

        LinearLayout _l = (LinearLayout) drumsLayout.getChildAt(0);
        playButton = (ToggleButton) _l.getChildAt(0);
        playButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                createPlaylist();
                play();
            }
        });

        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 10);
        setLayoutParams(layoutParams);
        drumsLayout.setLayoutParams(layoutParams);
        FrameLayout.LayoutParams layoutParamsF = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ScrollView scrollView = (ScrollView) drumsLayout.getChildAt(drumsLayout.getChildCount() - 1);
        canvas = (LinearLayout) scrollView.getChildAt(0);
        LayoutParams layoutParams1 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        canvas.setLayoutParams(layoutParamsF);

        int cols = 8 ;
        samples = new String[0];
        try {
            samples = mainActivity.getAssets().list(dir);
        } catch (IOException e) {
            MainActivity.alert("Unable to load drumkit", e.getMessage());
            e.printStackTrace();
        }

        int rows = samples.length;
        for (int i = 0 ; i < rows ; i ++) {
            if (samples [i].equals("drumkit.xml"))
                continue;
            ArrayList <ToggleButton> row = new ArrayList<>();
            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(HORIZONTAL);
            linearLayout.setLayoutParams(layoutParams1);
            HorizontalScrollView scrollView1 = new HorizontalScrollView(context);
            scrollView1.addView(linearLayout);

            TextView label = new TextView(context);
            label.setText(samples [i]);
            label.setPadding(10,10,10,10);
            canvas.addView(label);
            canvas.addView(scrollView1);

            for (int j = 0 ; j < cols ; j ++) {
                ToggleButton button = new ToggleButton(context);
                row.add(button);
                button.setTextOn("#");
                button.setTextOff(String.valueOf(j + 1));
                button.setText(String.valueOf(j + 1));
                button.setBackgroundColor(getResources().getColor(com.firebase.ui.auth.R.color.fui_transparent));
                button.setTextColor(getResources().getColor(R.color.white));
                linearLayout.addView(button);
                linearLayout.setBackgroundResource(R.drawable.semi_trans);
            }

            ticks.add(row);
        }
    }

    public void createPlaylist () {
        for (int i = 0 ; i < 8 ; i ++) {
            ExoPlayer player = players.get(i) ;
            player.removeMediaItems(0, player.getMediaItemCount());
            for (ArrayList a: ticks) {
                ToggleButton toggleButton = (ToggleButton) a.get(i);
                if (toggleButton.isChecked()) {
                    String uri = "asset:///" + dir + samples [i];
                    Log.d(TAG, "createPlaylist: adding " + String.format(
                            "%s at %d",
                            uri,
                            i
                    ));
                    player.addMediaItem(MediaItem.fromUri(uri));
                    player.prepare();
                }
            }
        }
    }

    public void play () {
        if (playButton.isChecked() == false)
            return ;
        long delay = (1000 / cols) * (60 / bpm) ;
        delay = 300 ;
        Log.d(TAG, "play: delay = " + delay);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: playing " + current);
                players.get(current).play();
                current ++ ;
                if (current > 7)
                    current = 0 ;
                // Your Code
                play ();
            }
        }, delay);
    }
    public void play1 () {
        long delay = (1000 / cols) * (60 / bpm) ;
        Log.d(getClass().getSimpleName(), "play: delay = " + delay);
        while (playButton.isChecked()) {
            for (int i = 0 ; i < cols ; i ++) {
                for (ExoPlayer player: players) {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            player.play();
                            // Your Code
                        }
                    }, delay);

                }
            }
        }
    }
}
