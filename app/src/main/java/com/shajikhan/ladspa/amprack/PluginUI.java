package com.shajikhan.ladspa.amprack;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.List;

class PluginControl {
    int min ;
    int max ;
    int def ;
    String name ;
    int portNumber ;
}

public class PluginUI extends CardView {
    List <PluginControl> pluginControls = new ArrayList<>();

    public PluginUI(@NonNull Context context, int plugin) {
        super(context);

    }
}
