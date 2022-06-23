package com.shajikhan.ladspa.amprack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
        // we want the file to have extension so user doesn't try to send WAVE files by Whatsapp (unknowningly)
//        holder.fileButton.setText(basename.substring(0, basename.indexOf(".")));
        holder.fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaItem mediaItem = MediaItem.fromUri(filenames.get(position));
                player.setMediaItem(mediaItem);
                player.prepare();
                tracks.playPause.setChecked(true);
            }
        });

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setMessage("Are you sure you want to delete this file? This action cannot be undone.")
                        .setPositiveButton("Delete file", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                File file = new File(name);
                                if (file.delete())
                                    delete(position);
                                else
                                    MainActivity.toast("Unable to delete file");
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .setTitle("Delete " + basename + " ?");
                builder.create().show();
            }
        });
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
        filenames.remove(index);
        notifyItemRemoved(index);
    }

}
