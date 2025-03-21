package com.shajikhan.ladspa.amprack;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class PluginDialogAdapter extends RecyclerView.Adapter <PluginDialogAdapter.ViewHolder> {
    String TAG = this.getClass().getSimpleName();
    Context context = null;
    ArrayList<Integer> plugins = new ArrayList<>();
    ArrayList<Integer> pluginsIDs = new ArrayList<>();
    ArrayList<String> pluginNames = new ArrayList<>();
    ArrayList<Integer> pluginsAll = new ArrayList<>();
    ArrayList<String> pluginNamesAll = new ArrayList<>();
    ArrayList<String> descriptions = new ArrayList<>();
    ArrayList<String> descriptionsAll = new ArrayList<>();
    MainActivity mainActivity ;
    ArrayList <ViewHolder> holders = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        Log.d(TAG, "create view");
        if (context == null) context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.plugin_dialog_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PluginDialogAdapter.ViewHolder holder, int position) {
//        Log.d(TAG, "bid view");
        LinearLayout layout = holder.linearLayout ;
        holders.add(holder);
        String pluginName = pluginNames.get(position) ;
        String desc = descriptions.get(position);
        holder.description.setText(desc);
        if (mainActivity.isPluginLV2(pluginName))
            holder.pluginName.setText(/*"[LV2] " + */ pluginName);
        else
            holder.pluginName.setText(pluginName);
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mainActivity.dataAdapter.plugins.size() > 1 && MainActivity.proVersion == false) {
                    Log.w(TAG, "onClick: " + String.format("already %d plugins in queue", mainActivity.dataAdapter.plugins.size()));
//                    Intent intent = new Intent(mainActivity, Purchase.class);
//                    context.startActivity(intent);
//                    return;
                }

                final int pluginID = plugins.get(holder.getAdapterPosition());
                Log.d(TAG, "Adding plugin ID: " + pluginID) ;
                mainActivity.addPluginToRack(pluginID);
                if (! mainActivity.defaultSharedPreferences.getBoolean("keep_plugindialog_open", false))
                    mainActivity.pluginDialog.hide();
                mainActivity.recyclerView.scrollToPosition(mainActivity.dataAdapter.getItemCount() - 1);
                if (mainActivity.rack.autoHideMixer && mainActivity.rack.mixer.getVisibility() == View.VISIBLE)
                    mainActivity.hidePanel.performClick();
            }
        });

        holder.toggleButton.setOnCheckedChangeListener(null);
        if (mainActivity.isPluginHearted(pluginNames.get(position))) {
            holder.toggleButton.setChecked(true);
            holder.toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_24);
        } else {
            holder.toggleButton.setChecked(false);
            holder.toggleButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
        }

        holder.toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    compoundButton.setButtonDrawable(R.drawable.ic_baseline_favorite_24);
                    mainActivity.heartPlugin(pluginNames.get(position));
                } else {
                    compoundButton.setButtonDrawable(R.drawable.ic_baseline_favorite_border_24);
                    mainActivity.unheartPlugin(pluginNames.get(position));
                }
            }
        });

        if (mainActivity.useTheme) {
            mainActivity.skinEngine.cardText(holder.pluginName);
            layout.post(new Runnable() {
                @Override
                public void run() {
                    mainActivity.skinEngine.card ((View) layout.getParent());

                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return plugins.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout linearLayout ;
        public TextView pluginName ;
        public MaterialButton button ;
        public TextView description ;
        public ToggleButton toggleButton ;
         public ViewHolder(@NonNull View itemView) {
            super(itemView);
            linearLayout = (LinearLayout) itemView.findViewById(R.id.plugin_layout) ;
            pluginName = (TextView) linearLayout.getChildAt(0);
            button = (MaterialButton) linearLayout.getChildAt(2);
            toggleButton = (ToggleButton) linearLayout.getChildAt(1);
            description = itemView.findViewById(R.id.plugin_desc);
        }
    }


    void addItem(int pluginID, String pluginName) {
        if (pluginNamesAll.contains(pluginName))
            return;

        plugins.add(pluginID);
        pluginNames.add(pluginName);
        pluginsAll.add(pluginID);
        pluginNamesAll.add(pluginName);
        notifyItemInserted(plugins.size());
    }

    void addItem(int pluginID, String pluginName, int uniqueID, String desc) {
        if (pluginNamesAll.contains(pluginName))
            return;

        plugins.add(pluginID);
        pluginNames.add(pluginName);
        pluginsAll.add(pluginID);
        pluginsIDs.add(uniqueID);
        descriptionsAll.add(desc);
        descriptions.add(desc);
        pluginNamesAll.add(pluginName);
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

    void search (String searchTerm) {
        plugins.clear();
        pluginNames.clear();
        descriptions.clear();

        if (searchTerm.length() == 0) {
            for (int i = 0; i < pluginNamesAll.size(); i++) {
                plugins.add(pluginsAll.get(i));
                pluginNames.add(pluginNamesAll.get(i));
                descriptions.add(descriptionsAll.get(i));
            }
        } else {
            for (int i = 0; i < pluginNamesAll.size(); i++) {
                String s = pluginNamesAll.get(i);
                Log.d(TAG, "search: " + String.format("<%s> in %s", searchTerm, s));
                if (s.toLowerCase().contains(searchTerm)) {
                    plugins.add(pluginsAll.get(i));
                    pluginNames.add(pluginNamesAll.get(i));
                    descriptions.add(descriptionsAll.get(i));
                }
            }
        }

        notifyDataSetChanged();
    }

    void showOnlyFavorites (boolean show) {
        plugins.clear();
        pluginNames.clear();
        descriptions.clear();

        Log.d(TAG, "showOnlyFavorites: " + mainActivity.getHeartedPlugins());

        if (show == false) {
            for (int i = 0; i < pluginNamesAll.size(); i++) {
                plugins.add(pluginsAll.get(i));
                descriptions.add(descriptionsAll.get(i));
                pluginNames.add(pluginNamesAll.get(i));
            }
        } else {
            for (int i = 0; i < pluginNamesAll.size(); i++) {
                if (mainActivity.isPluginHearted(pluginNamesAll.get (i))) {
                    plugins.add(pluginsAll.get(i));
                    descriptions.add(descriptionsAll.get(i));
                    pluginNames.add(pluginNamesAll.get(i));
                }
            }
        }

        notifyDataSetChanged();
    }


    void filterByCategory (String category) {
        plugins.clear();
        pluginNames.clear();
        descriptions.clear();

        JSONArray IDs = null;
        try {
            if (mainActivity.pluginDialogSortBy.getSelectedItemPosition() == 0)
                IDs = (JSONArray) MainActivity.pluginCategories.get(category);
            else
                IDs = (JSONArray) MainActivity.pluginCreators.get(category);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        if (IDs.length() == 0) {
            for (int i = 0; i < pluginNamesAll.size(); i++) {
                plugins.add(pluginsAll.get(i));
                descriptions.add(descriptionsAll.get(i));
                pluginNames.add(pluginNamesAll.get(i));
            }
        } else {
            for (int i = 0; i < pluginsIDs.size(); i++) {
                int pluginID = pluginsIDs.get(i);
                for (int j = 0 ; j < IDs.length();j++) {
                    try {
                        int UID  = IDs.getInt(j);
                        if (UID == pluginID) {
                            plugins.add(pluginsAll.get(i));
                            descriptions.add(descriptionsAll.get(i));
                            pluginNames.add(pluginNamesAll.get(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        continue;
                    }

                }
            }
        }

        notifyDataSetChanged();
    }


    void reset () {
        notifyItemRangeRemoved(0, plugins.size());
        plugins.clear();
        pluginNames.clear();
        descriptions.clear();
    }

    @Override
    public long getItemId(int position) {
        return plugins.get(position);
    }

}