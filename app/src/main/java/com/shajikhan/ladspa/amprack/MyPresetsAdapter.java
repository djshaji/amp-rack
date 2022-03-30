package com.shajikhan.ladspa.amprack;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Map;

public class MyPresetsAdapter extends RecyclerView.Adapter<MyPresetsAdapter.ViewHolder> {
    String TAG = this.getClass().getSimpleName();
    Context context = null ;
    ArrayList<Map> presets = new ArrayList<>();
    MainActivity mainActivity = null;
    FirestoreDB db ;
    ProgressBar progressBar ;
    MyPresetsAdapter myPresetsAdapter ;

    void addPreset (Map preset) {
        presets.add(preset);
        notifyDataSetChanged();
    }

    void setMainActivity (MainActivity _mainActivity) {
        mainActivity = _mainActivity;
    }

    void setProgressBar (ProgressBar progressBar1) {
        progressBar = progressBar1 ;
    }

    @NonNull
    @Override
    public MyPresetsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) context = parent.getContext();
        db = new FirestoreDB(parent.getContext());
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.my_preset, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyPresetsAdapter.ViewHolder holder, int position) {
        LinearLayout linearLayout = holder.getLinearLayout();
        myPresetsAdapter = this ;
        if (linearLayout == null) {
            Log.wtf(TAG, "linear layout for plugin!") ;
            return ;
        }

        LinearLayout linearLayout1 = (LinearLayout) linearLayout.getChildAt(0);
        Map <String, Object> preset = presets.get(position);

        LinearLayout linearLayout2 = (LinearLayout)linearLayout1.getChildAt(0);
        TextView name = (TextView) linearLayout2.getChildAt(0);
        TextView desc = (TextView) linearLayout2.getChildAt(1);

        name.setText(preset.get("name").toString());
        desc.setText(preset.get("desc").toString());

        MaterialButton materialButton = (MaterialButton) linearLayout1.getChildAt(1);
        materialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.loadPreset(preset);
                MainActivity.toast("Loaded preset " + preset.get ("name").toString());
            }
        });

        MaterialButton deletePreset = (MaterialButton) linearLayout.getChildAt(1);
        deletePreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                db.deletePreset(preset, presets, myPresetsAdapter, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return presets.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout ;
        public ViewHolder(View view) {
            super(view);
            linearLayout = (LinearLayout) view ;
        }

        public LinearLayout getLinearLayout() {
            return linearLayout ;
        }
    }
}
