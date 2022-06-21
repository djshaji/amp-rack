package com.shajikhan.ladspa.amprack;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;

public class Tracks extends Fragment {
    MainActivity mainActivity;
    TracksAdapter tracksAdapter ;
    RecyclerView recyclerView ;
    String TAG = getClass().getSimpleName();

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
        tracksAdapter = new TracksAdapter();
        recyclerView.setAdapter(tracksAdapter);
        load (mainActivity.dir);
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