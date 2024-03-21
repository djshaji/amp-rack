package com.shajikhan.ladspa.amprack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;

import java.io.File;
import java.util.ArrayList;

public class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.ViewHolder> {
    String TAG = this.getClass().getSimpleName();
    Context context = null ;
    ArrayList<String> filenames = new ArrayList<>();
    MainActivity mainActivity = null;
    ArrayList <ViewHolder> holders = new ArrayList<>();
    public ExoPlayer player ;
    Tracks tracks ;

    @NonNull
    @Override
    public TracksAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) context = parent.getContext();
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.track_file, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TracksAdapter.ViewHolder holder, int position) {
        holders.add(holder);
        LinearLayout linearLayout = holder.linearLayout;

        String name = filenames.get(position) ;
        String basename = name.substring(name.lastIndexOf(File.separator) + 1);
        holder.fileButton.setText(basename);

//        if (basename.endsWith(".mp4"))
//            holder.fileButton.setCompoundDrawables(new BitmapDrawable (BitmapFactory.decodeResource(mainActivity.getResources(), R.drawable.baseline_ondemand_video_24)),
//                    null, null, null);
//        else
//            holder.fileButton.setCompoundDrawables(new BitmapDrawable (BitmapFactory.decodeResource(mainActivity.getResources(), R.drawable.ic_baseline_audio_file_24)),
//                    null, null, null);

        // we want the file to have extension so user doesn't try to send WAVE files by Whatsapp (unknowningly)
//        holder.fileButton.setText(basename.substring(0, basename.indexOf(".")));
        holder.fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaItem mediaItem = MediaItem.fromUri(filenames.get(holder.getAdapterPosition()));
                Log.d(TAG, "onClick: playing " + name);
                player.setMediaItem(mediaItem);
                player.prepare();
                tracks.playPause.setChecked(true);
            }
        });

        if (name.startsWith("asset://")) {
            holder.deleteButton.setVisibility(View.GONE);
            Log.d(TAG, "onBindViewHolder: hiding delete button for track: " + name);
        } else {
            holder.fileButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // brilliant
                    MainActivity.shareFile(new File (name));
                    return true;
                }
            });

            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setMessage("Are you sure you want to delete this file? This action cannot be undone.")
                            .setPositiveButton("Delete file", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Log.d(TAG, "onClick: deleting file " + name);
                                    File file = new File(name);
                                    if (name.startsWith("content://")) {
                                        delete(holder.getAdapterPosition());
                                    } else if (file.delete())
                                        delete(holder.getAdapterPosition());
                                    else {
                                        MainActivity.toast("Unable to delete file");
                                        Log.e(TAG, "onClick: delete track: " + name, null);
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .setTitle("Delete " + basename + " ?");
                    builder.create().show();
                }
            });
        }

        if (mainActivity.useTheme) {
            mainActivity.skinEngine.cardText(holder.fileButton);
            linearLayout.post(new Runnable() {
                @Override
                public void run() {
                    mainActivity.skinEngine.card (linearLayout);

                }
            });

            if (tracks.themeInit == false) {
                Log.d(TAG, "onBindViewHolder: tracks for " + tracks.filesDir);
                tracks.themeInit = true ;
                if (tracks.filesDir == "assets://drums") {
                    Log.i(TAG, "onBindViewHolder: fixing drums player");
                    tracks.playerWindow.setBackground(null);
                }
                mainActivity.skinEngine.card (tracks.playerWindow);
                tracks.playerWindow.post(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.skinEngine.card (tracks.playerWindow);
                    }
                });

            }
        }
    }

    @Override
    public int getItemCount() {
        return filenames.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        Button fileButton, deleteButton ;
        LinearLayout linearLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            linearLayout = (LinearLayout) itemView;
            fileButton = (Button) linearLayout.getChildAt(0);
            deleteButton = (Button) linearLayout.getChildAt(1);
        }
    }

    public void add (String filename) {
        filenames.add(filename);
        notifyItemInserted(filenames.size());
    }

    void delete(int index) {
        if (index > filenames.size()) {
            Log.w(TAG, "delete: index greater than items in list!", null);
            return ;
        }

        filenames.remove(index);
        notifyItemRemoved(index);
    }

}
