package com.shajikhan.ladspa.amprack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MyPresetsAdapter extends RecyclerView.Adapter<MyPresetsAdapter.ViewHolder> {
    String TAG = this.getClass().getSimpleName();
    Context context = null ;
    ArrayList<Map> presets = new ArrayList<>();
    MainActivity mainActivity = null;
    FirestoreDB db ;
    ProgressBar progressBar ;
    MyPresetsAdapter myPresetsAdapter ;
    String sortBy = "timestamp";
    String uid = null;
    Map<String, Object> favoritePresets = null;
    ArrayList <Map> allPresets = new ArrayList<>() ;

    void removeAll () {
        int num = allPresets.size();
        allPresets.clear();
        presets.clear();
        notifyItemRangeRemoved(0, num);
    }

    void addPreset (Map preset) {
        presets.add(preset);
        allPresets.add(preset);
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
        FirebaseAuth auth = FirebaseAuth.getInstance() ;
        if (auth != null)
            uid = auth.getUid();

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

        LinearLayout linearLayout3 = (LinearLayout) linearLayout.getChildAt(1);
        MaterialButton deletePreset = (MaterialButton) linearLayout3.getChildAt(0);
        ToggleButton heart = (ToggleButton) linearLayout3.getChildAt(2);
        heart.setOnCheckedChangeListener(null);
        if (favoritePresets!= null && preset.get("path")!= null && favoritePresets.containsKey(preset.get("path").toString())) {
            heart.setButtonDrawable(R.drawable.ic_baseline_favorite_24);
            heart.setChecked(true);
        } else {
            heart.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
            heart.setChecked(false);
        }

        heart.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    compoundButton.setButtonDrawable(R.drawable.ic_baseline_favorite_24);
//                    db.addPresetToCollection("collections", preset);
//                    db.likePreset(preset);
                    db.addAndLike(preset);
                    if (favoritePresets == null) {
                        favoritePresets = new HashMap<String, Object>();
                    }

                    favoritePresets.put(preset.get("path").toString(), preset.get("name"));
                } else {
                    compoundButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
                    favoritePresets.remove(preset.get("path"));
                    db.removeAndUnlike(preset);
                }
            }
        });
        if (!preset.get("uid").equals(uid))
            deletePreset.setVisibility(View.GONE);
        else
            heart.setVisibility(View.GONE);

        deletePreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        db.deletePreset(preset, presets, myPresetsAdapter, position);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });

                builder.setTitle("Delete this preset?");
                builder.setMessage("Are you sure you want to delete this preset? This action cannot be undone.") ;

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });

        if (mainActivity.useTheme) {
            linearLayout.post(new Runnable() {
                @Override
                public void run() {
                    mainActivity.skinEngine.card (linearLayout);

                }
            });
        }
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

    public void updateList(String string){
        Log.d(TAG, "updateList: " +
            String.format ("<%s> presets: %s\nall presets:%s", string, presets.toString(), allPresets.toString()));

        presets.clear();
        if(string.length() == 0) {
            for (Map m: allPresets)
                presets.add(m);
        } else {
            for (Map m : allPresets) {
                if (m.get("name").toString().toLowerCase().contains(string)) {
                    presets.add(m);
                }
            }
        }

        notifyDataSetChanged();
    }

    public void resetList () {
        presets = allPresets ;
        notifyDataSetChanged();
    }

    public void showOnlyFavorites (boolean show) {
        Log.d(TAG, "showOnlyFavorites: " + show);
        presets.clear();
        if(show == false) {
            Log.d(TAG, "showOnlyFavorites: adding all presets");
            for (Map m: allPresets)
                presets.add(m);
        } else {
            for (Map m : allPresets) {
                if (favoritePresets.containsKey(m.get("path").toString())) {
                    Log.d(TAG, "showOnlyFavorites: adding preset " + m.get("name"));
                    presets.add(m);
                }
            }
        }

        notifyDataSetChanged();
    }
}
