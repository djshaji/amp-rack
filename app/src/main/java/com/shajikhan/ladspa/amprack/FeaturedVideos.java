package com.shajikhan.ladspa.amprack;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.shajikhan.ladspa.amprack.databinding.ActivityFeaturedVideosBinding;

public class FeaturedVideos extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityFeaturedVideosBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFeaturedVideosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}