package com.shajikhan.ladspa.amprack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class TracksAdapter extends RecyclerView.Adapter<TracksAdapter.ViewHolder> {
    String TAG = this.getClass().getSimpleName();
    Context context = null ;
    ArrayList<String> filenames = new ArrayList<>();
    MainActivity mainActivity = null;
    ArrayList <ViewHolder> holders = new ArrayList<>();

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
        holder.fileButton.setText(basename.substring(0, basename.indexOf(".")));

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File file = new File(name);
                if (file.delete())
                    delete(position);
                else
                    MainActivity.toast("Unable to delete file");
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
