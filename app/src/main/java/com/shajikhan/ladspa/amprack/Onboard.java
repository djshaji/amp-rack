package com.shajikhan.ladspa.amprack;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class Onboard extends AppCompatActivity {

    Context context ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this ;
        setContentView(R.layout.activity_onboard);
        getSupportActionBar().hide ();

        Button prev = findViewById(R.id.onboard_prev);
        Button next = findViewById(R.id.onboard_next);
        Button finish = findViewById(R.id.onboard_finish);

        TextView buildId = findViewById(R.id.onboard_build_number);
        String version = String.format("Build %s Version Code %d",
                getResources().getString(R.string.build_id),
                BuildConfig.VERSION_CODE);
        buildId.setText(version);
//        MainActivity.applyWallpaper(context, getWindow(), getResources(), findViewById(R.id.wallpaper_on), getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight()); //finally

        RadioButton tube = findViewById(R.id.select_tube),
                material = findViewById(R.id.select_material);

        ImageView materialScreen = findViewById(R.id.material_screen),
                tubeScreen = findViewById(R.id.tube_screen);

        materialScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                material.setChecked(true);
                tube.setChecked(false);
            }
        });

        tubeScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                material.setChecked(false);
                tube.setChecked(true);
            }
        });

        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("currentVersion", BuildConfig.VERSION_CODE).apply();

                finishAffinity();
                finish () ;
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("onboard", 1);
                String theme = "Material";
                if (tube.isChecked())
                    theme = "TubeAmp";
                intent.putExtra("theme", theme);
                startActivity(intent);
            }
        });

        ImageView screen = findViewById(R.id.screenshot);
        LinearLayout welcome = findViewById(R.id.welcome_screen),
                page1 = findViewById(R.id.onboard_page1);
        LinearLayout selectTheme = findViewById(R.id.onboard_select_theme);

        Spinner spinner = findViewById(R.id.onboard_theme_selector);

        tube.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (! isChecked) return;
                material.setChecked(false);
                PreferenceManager.getDefaultSharedPreferences (context).edit().putString("theme", "TubeAmp").commit();
            }
        });

        material.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (! isChecked) return;
                tube.setChecked(false);
                PreferenceManager.getDefaultSharedPreferences (context).edit().putString("theme", "Material").commit();
                MainActivity.introShown = true;
            }
        });
        String [] models = {"Material", "TubeAmp"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, models);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        Button skip = findViewById(R.id.onboard_skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("currentVersion", BuildConfig.VERSION_CODE).apply();
                finishAffinity();
                finish () ;
                Intent intent = new Intent(context, MainActivity.class);
                intent.putExtra("onboard", 1);
                startActivity(intent);
            }
        });
        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTheme.setVisibility(View.GONE);
                screen.setVisibility(View.VISIBLE);
                v.setVisibility(View.GONE);
                next.setVisibility(View.VISIBLE);
                finish.setVisibility(View.GONE);
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (welcome.getVisibility() == View.VISIBLE) {
                    welcome.setVisibility(View.GONE);
                    page1.setVisibility(View.VISIBLE);
                    return;
                }

                v.setVisibility(View.GONE);
                prev.setVisibility(View.VISIBLE);
                finish.setVisibility(View.VISIBLE);
                selectTheme.setVisibility(View.VISIBLE);
                screen.setVisibility(View.GONE);
                skip.setVisibility(View.GONE);

            }
        });


    }
}