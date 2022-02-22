package com.shajikhan.ladspa.amprack;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.shajikhan.ladspa.amprack.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    Context context;
    Switch onOff ;
    MaterialButton record ;

    // Used to load the 'amprack' library on application startup.
    static {
        System.loadLibrary("amprack");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().hide();

    }

    /**
     * A native method that is implemented by the 'amprack' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}