package com.shajikhan.ladspa.amprack;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class Onboard extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboard);
        getSupportActionBar().hide ();

        ExtendedFloatingActionButton prev = findViewById(R.id.onboard_prev);
        ExtendedFloatingActionButton next = findViewById(R.id.onboard_next);

        ImageView screen = findViewById(R.id.screenshot);
        LinearLayout selectTheme = findViewById(R.id.onboard_select_theme);

        Spinner spinner = findViewById(R.id.onboard_theme_selector);

        String [] models = {"Material", "TubeAmp"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, models);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTheme.setVisibility(View.GONE);
                screen.setVisibility(View.VISIBLE);
                v.setVisibility(View.GONE);
                next.setVisibility(View.VISIBLE);
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
                prev.setVisibility(View.VISIBLE);
                selectTheme.setVisibility(View.VISIBLE);
                screen.setVisibility(View.GONE);

            }
        });
    }
}