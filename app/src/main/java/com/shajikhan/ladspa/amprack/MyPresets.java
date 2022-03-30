package com.shajikhan.ladspa.amprack;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MyPresets extends Fragment {
    MainActivity mainActivity;
    String TAG = getClass().getSimpleName();
    RecyclerView recyclerView;
    public MyPresetsAdapter myPresetsAdapter ;
    FirestoreDB db ;
    ProgressBar progressBar ;

    MyPresets (ProgressBar _progressBar) {
        progressBar = _progressBar;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.my_presets, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        db = new FirestoreDB (mainActivity);

        LinearLayout layout = (LinearLayout) view ;
        recyclerView = (RecyclerView) ((LinearLayout) view).getChildAt(0);
        recyclerView.setLayoutManager(new LinearLayoutManager(mainActivity));
        myPresetsAdapter = new MyPresetsAdapter();
        myPresetsAdapter.setMainActivity(mainActivity);
        recyclerView.setAdapter(myPresetsAdapter);

        myPresetsAdapter.setProgressBar(progressBar);
        db.loadUserPresets(myPresetsAdapter);
    }
}