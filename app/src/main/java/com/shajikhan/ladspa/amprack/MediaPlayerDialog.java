package com.shajikhan.ladspa.amprack;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.File;
import java.io.IOException;

public class MediaPlayerDialog {
    MainActivity mainActivity ;
    MediaPlayer mediaPlayer ;
    AlertDialog dialog ;
    String TAG = getClass().getSimpleName();

    MediaPlayerDialog (MainActivity activity, MediaPlayer player) {
        mainActivity = activity;
        mediaPlayer = player;
        create(mainActivity);
    }

    public Dialog create (MainActivity context) {
        mainActivity = context ;
        mediaPlayer = mainActivity.mediaPlayer;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = context.getLayoutInflater();

        ConstraintLayout constraintLayout = (ConstraintLayout) inflater.inflate(R.layout.media_player_dialog, null);
        SurfaceView surface = constraintLayout.findViewById(R.id.video_player_dialog);

        surface.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mediaPlayer.setDisplay(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                       int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mediaPlayer.setDisplay(null);
                mediaPlayer.stop();
            }
        });

        ToggleButton toggleButton = constraintLayout.findViewById(R.id.media_play);
        surface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleButton.setChecked(false);
            }
        });
        Button openFolder = constraintLayout.findViewById(R.id.open_folder);
        openFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(mainActivity.dir.toString());
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setDataAndType(uri, "*/*");
                mainActivity.startActivity(intent);

            }
        });
        TextView textView = constraintLayout.findViewById(R.id.media_filename);
        File file = new File(mainActivity.lastRecordedFileName);
        textView.setText(file.getName());

        LinearLayout surfaceLayout = constraintLayout.findViewById(R.id.surface_ll);
        mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                surface.getHolder().setFixedSize(width, height);
            }
        });

        toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mainActivity.tracks.tracksAdapter.add(mainActivity.lastRecordedFileName);
                    Uri uri = Uri.parse(mainActivity.lastRecordedFileName);
                    try {
                        if (mainActivity.lastRecordedFileName.endsWith(".mp4")) {
                            surface.setVisibility(View.VISIBLE);
                            Log.d(TAG, "onCheckedChanged: ends with mp4");
                        } else {
                            surface.setVisibility(View.GONE);
                            Log.d(TAG, "onCheckedChanged: no end");
                        }

                        mediaPlayer.stop();
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(mainActivity.getApplicationContext(), uri);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                        mainActivity.toast("Cannot load media file: " + e.getMessage());
                        return;
                    }

                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_pause_24);
                    mediaPlayer.start();
                } else {
                    mediaPlayer.pause();
                    toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
                    surface.setVisibility(View.GONE);
                }
            }
        });

        SeekBar seekBar = constraintLayout.findViewById(R.id.media_seekbar);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                toggleButton.setButtonDrawable(R.drawable.ic_baseline_play_arrow_24);
                seekBar.setProgress(0);
                surface.setVisibility(View.GONE);
            }
        });

        Button share = constraintLayout.findViewById(R.id.share_file);
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.shareFile(file);
                // this is pretty awesome!
                // update 24-6-2022 doesnt work :(
                /*
                MediaScannerConnection.scanFile(context,
                        new String[] { file.toString() }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                Log.i("ExternalStorage", "-> uri=" + uri);
                                intentShareFile.setType("audio/*");
                                intentShareFile.putExtra(Intent.EXTRA_STREAM, uri);

                                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                                        "Sharing Audio File...");
                                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Sharing Audio File...");

                                intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                startActivity(Intent.createChooser(intentShareFile, "Share Audio File"));
                            }
                        });

                 */
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar _seekBar) {
                mediaPlayer.seekTo(mediaPlayer.getDuration() * (_seekBar.getProgress() / 100));

            }
        });

        // Disabling this because this caused a crash when stopping recording ... I think
        // very hard to debug ... VERY !!!
        /*
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mediaPlayer.isPlaying()) {
                    seekBar.setProgress(100 * mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration());
                    Log.d(TAG, "run: " + mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration());
                }
            }
        }, 0, 1000);

         */

        builder.setView(constraintLayout)
                .setPositiveButton("Close", null);

        dialog = builder.create();

        Button deleteFile = constraintLayout.findViewById(R.id.delete_file);
        deleteFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Are you sure you want to delete this file?")
                        .setTitle("Delete " + mainActivity.lastRecordedFileName)
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface _dialog, int id) {
                                file.delete();
                                if (file.exists()) {
                                    mainActivity.toast("File could not be deleted");
                                } else {
                                    mainActivity.toast("File deleted");
                                    dialog.dismiss();
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null);
                // Create the AlertDialog object and return it
                builder.create().show();
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mediaPlayer.stop();
//                timer.cancel();
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mediaPlayer.stop();
//                timer.cancel();
            }
        });

        return dialog;
    }
}
