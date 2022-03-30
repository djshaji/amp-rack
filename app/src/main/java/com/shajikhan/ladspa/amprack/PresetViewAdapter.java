package com.shajikhan.ladspa.amprack;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PresetViewAdapter extends FragmentPagerAdapter {
    MyPresets myPresets ;
    public PresetViewAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    void setMyPresets (MyPresets presets) {
        myPresets = presets ;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return myPresets ;
        }

        return null ;
    }

    @Override
    public int getCount() {
        return 2 ;
    }
}
