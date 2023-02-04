package com.shajikhan.ladspa.amprack;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class SkinEngine {
    MainActivity mainActivity ;
    String TAG = getClass().getSimpleName() ;
    String theme = "default" ;
    String themeDir = "themes/default/";
    HashMap <String, HashMap <String, String>> config = new HashMap<>();
    JSONObject jsonConfig ;
    Skinner skinner ;
    Paint paint ;

    void setTheme (String _theme) {
        theme = _theme ;
        themeDir = String.format("themes/%s/", theme);
        jsonConfig = ConnectGuitar.loadJSONFromAssetFile(mainActivity, themeDir + "theme.json");
        Log.d(TAG, "setTheme: " + jsonConfig.toString());
        load ();
    }

    SkinEngine (MainActivity _mainActivity) {
        mainActivity = _mainActivity ;
        skinner = new Skinner(mainActivity);
        skinner.init();
        paint = new Paint();
        setTheme("default"); // sane default
    }

    void load () {
        JSONObject hConfig ;
        Iterator<String> keys = jsonConfig.keys();

        while(keys.hasNext()) {
            String key = keys.next();
            try {
                if (jsonConfig.get(key) instanceof JSONObject) {
                    JSONObject object = jsonConfig.getJSONObject(key);
                    Iterator<String> keys1 = object.keys();
                    HashMap <String, String> hashMap = new HashMap();
                    while (keys1.hasNext()) {
                        String key2 = keys1.next();
                        hashMap.put(key2, object.getString(key2));
                    }

                    config.put(key, hashMap);
                }
            } catch (JSONException e) {
                Log.e(TAG, "load: " + key, e);
            }
        }

        Log.d(TAG, config.toString());
    }

    void wallpaper (ImageView wall) {
        String wallpaper = config.get("wallpaper").get("bg") ;
        if (wallpaper.startsWith("#")) {
            wall.setBackgroundColor(Color.parseColor(wallpaper));
        } else {
            Bitmap bg = skinner.getBitmapFromAssets( skinner.displayMetrics.widthPixels, -1, themeDir + wallpaper);
            wall.setImageBitmap(bg);
        }
    }

    void header (LinearLayout layout) {
        String hBg = config.get("header").get("bg");
        Bitmap headerBg = skinner.getBitmapFromAssets(skinner.displayMetrics.widthPixels, -1, themeDir + hBg);
        layout.setBackground(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                setBounds(0, 0, headerBg.getWidth(), headerBg.getHeight());
                canvas.drawBitmap(headerBg, 0, 0, paint);
            }

            @Override
            public void setAlpha(int i) {

            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return 0;
            }
        });
    }

    enum Resize {
        Width,
        Height,
        None
    }

    void view (View view, String category, String name, Resize resize, float factor) {
        view.setBackground(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                int w = view.getWidth(), h = view.getHeight() ;
                Bitmap b ;
                if (resize == Resize.Width)
                    b = skinner.getBitmapFromAssets((int) ((float) w * factor), -1, themeDir + config.get(category).get(name));
                else if (resize == Resize.Height)
                    b = skinner.getBitmapFromAssets(-1 , (int) ((float) h * factor), themeDir + config.get(category).get(name));
                else
                    b = skinner.getBitmapFromAssets(w , h, themeDir + config.get(category).get(name));
                setBounds(0, 0, w, h);
                canvas.drawBitmap(b, (w - b.getWidth()) / 2, (h - b.getHeight()) / 2, paint);
            }

            @Override
            public void setAlpha(int i) {

            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return 0;
            }
        });
    }

    void toggle (ToggleButton toggleButton, boolean state) {
        String on = config.get("toggle").get("on") ;
        if (! state) {
            on = config.get("toggle").get("off");
        }

        String finalOn = on;
        toggleButton.setButtonDrawable(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                int w = toggleButton.getWidth(), h = toggleButton.getHeight() ;
                Bitmap b = skinner.getBitmapFromAssets(w , -1, themeDir + finalOn);
                setBounds(0, 0, w, h);
                canvas.drawBitmap(b, (w - b.getWidth()) / 2, (h - b.getHeight()) / 2, paint);
            }

            @Override
            public void setAlpha(int i) {

            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return 0;
            }
        });


    }

    void setLogo (ImageView imageView) {
        Bitmap b = skinner.getBitmapFromAssets(imageView.getWidth(), -1, themeDir + config.get("header").get("logo"));
        if (b != null)
            imageView.setImageBitmap(b);
    }
}
