package com.shajikhan.ladspa.amprack;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.view.ViewGroup;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.shajikhan.ladspa.amprack.databinding.ActivityDrumMachineBinding;

public class DrumMachineActivity extends AppCompatActivity {
    DrumMachine drumMachine ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        drumMachine = new DrumMachine(getApplicationContext());
        drumMachine.setMainActivity(this);
        drumMachine.create();
        setTitle("Drummer");
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        addContentView(drumMachine, layoutParams);


    }

}