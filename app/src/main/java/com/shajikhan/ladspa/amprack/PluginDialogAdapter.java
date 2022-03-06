package com.shajikhan.ladspa.amprack;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class PluginDialogAdapter extends RecyclerView.Adapter <PluginDialogAdapter.ViewHolder> {
    String TAG = this.getClass().getSimpleName();
    Context context = null;
    ArrayList<Integer> plugins = new ArrayList<>();
    ArrayList<String> pluginNames = new ArrayList<>();
    MainActivity mainActivity ;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "create view");
        if (context == null) context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.plugin_dialog_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PluginDialogAdapter.ViewHolder holder, int position) {
        Log.d(TAG, "bid view");
        LinearLayout layout = holder.linearLayout ;
        holder.pluginName.setText(pluginNames.get(position));
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final int pluginID = plugins.get(position);
                Log.d(TAG, "Adding plugin ID: " + pluginID) ;
                mainActivity.addPluginToRack(pluginID);
            }
        });
    }

    @Override
    public int getItemCount() {
        return plugins.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout linearLayout ;
        public TextView pluginName ;
        public MaterialButton button ;
         public ViewHolder(@NonNull View itemView) {
            super(itemView);
            linearLayout = (LinearLayout) itemView ;
            pluginName = (TextView) linearLayout.getChildAt(0);
            button = (MaterialButton) linearLayout.getChildAt(1);
        }
    }


    void addItem(int pluginID, String pluginName) {
        plugins.add(pluginID);
        pluginNames.add(pluginName);
        notifyItemInserted(plugins.size());
    }

    void deleteItem(int index) {
        plugins.remove(index);
        notifyItemInserted(plugins.size());
    }

    void setMainActivity (Context _context, MainActivity _mainActivity) {
        mainActivity = _mainActivity ;
        context = _context ;
    }
}