package com.shajikhan.ladspa.amprack;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RotateDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.checkerframework.checker.units.qual.K;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class SkinEngine {
    MainActivity mainActivity ;
    String TAG = getClass().getSimpleName() ;
    String theme = "default" ;
    int nativeTheme = R.style.Theme_AmpRack;
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

    void fab (ExtendedFloatingActionButton _button, Resize resize, float factor) {
        _button.hide();
        Bitmap bitmap = skinner.getBitmapFromAssets(-1, 0, themeDir + config.get("icons").get("add"));
        BitmapDrawable drawable = new BitmapDrawable(bitmap);
        _button.setIcon(drawable);
//        _button.setBackgroundDrawable(drawable);
//        _button.setCompoundDrawables(drawable, null, null, null);
        button (_button, resize, factor);
        _button.show();
    }

    void button (Button button, Resize resize, float factor) {
        view (button, "button", "bg", Resize.Width, 1);
        button.setTextColor(Color.parseColor(config.get("button").get("text-color")));
    }

    void view (View _view, String category, String name, Resize resize, float factor) {
        _view.setBackground(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                int w = _view.getWidth(), h = _view.getHeight() ;
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

    BitmapDrawable bitmapDrawable (String category, String name) {
        Log.d(TAG, "bitmapDrawable() called with: category = [" + category + "], name = [" + name + "]");
        return new BitmapDrawable(skinner.getBitmapFromAssets(0 , 0, themeDir + config.get(category).get(name)));
    }

    void drawableLeft (Button _view, String category, String name, Resize resize, float factor) {
        _view.setCompoundDrawables(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                int w = _view.getWidth(), h = _view.getHeight() ;
                Bitmap b ;
                if (resize == Resize.Width)
                    b = skinner.getBitmapFromAssets((int) ((float) w * factor), -1, themeDir + config.get(category).get(name));
                else if (resize == Resize.Height)
                    b = skinner.getBitmapFromAssets(-1 , (int) ((float) h * factor), themeDir + config.get(category).get(name));
                else
                    b = skinner.getBitmapFromAssets(0 , 0, themeDir + config.get(category).get(name));
                setBounds(0, 0, w, h);
                if (resize == Resize.None) {
                    setBounds(0, 0, b.getWidth(), b.getHeight());
                    canvas.drawBitmap(b, 0, 0, paint);
//                    canvas.drawColor(Color.parseColor("#ffffff"));
                }
                else
                    canvas.drawBitmap(b, (w - b.getWidth()) / 2, 0, paint);
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
        }, null, null, null);
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
        Bitmap b = skinner.getBitmapFromAssets(0, 0, themeDir + config.get("header").get("logo"));
        if (b != null)
            imageView.setImageBitmap(b);
    }

    void slider (Slider slider) {
        ColorStateList active = ColorStateList.valueOf(Color.parseColor(config.get("slider").get("active")));
        ColorStateList base = ColorStateList.valueOf(Color.parseColor(config.get("slider").get("base")));
        ///| TODO: Maybe use custom colors here
        slider.setTrackInactiveTintList(base);
        slider.setTrackActiveTintList(active);

        /* is this required, or is color enough for the track?
        slider.setBackground(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                int sliderWidth = slider.getWidth(),
                        sliderHeight = slider.getHeight();
                setBounds(0, 0, sliderWidth, sliderHeight);
                Bitmap bg = skinner.getBitmapFromAssets(-1, 0, themeDir + config.get ("slider").get ("bg"));
                if (bg == null)
                        return ;
                int w = bg.getWidth();

                int top = (sliderHeight - bg.getHeight()) / 2 ;
                for (int i = 0 ; i + w < slider.getWidth(); i = i + w) {
                    canvas.drawBitmap(bg, i, top, paint);
                }
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

         */

        if (slider != null) {
            Bitmap bg = skinner.getBitmapFromAssets(-1, 0, themeDir + config.get ("slider").get ("thumb"));
            if (bg == null)
                return ;
            int w = bg.getWidth();
            // this is a great way to get a drawable from a bitmap into a view
            BitmapDrawable bitmapDrawable = new BitmapDrawable(bg);
            slider.setCustomThumbDrawable(bitmapDrawable);
        }

    }

    void setNativeTheme () {
        mainActivity.setTheme(nativeTheme);
    }

    void card (View layout) {
        int w = layout.getWidth(), h = layout.getHeight() ;
        Log.d(TAG, "card() called with: layout = [" + layout + "]" +
                String.format("\n%d x %d", w, h));
        Bitmap topLeft = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("top-left"));
        Bitmap topRight = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("top-right"));
        Bitmap bottomLeft = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("bottom-left"));
        Bitmap bottomRight = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("bottom-right"));
        Bitmap top = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("top"));
        Bitmap bottom = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("bottom"));
        Bitmap left = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("left"));
        Bitmap right = skinner.getBitmapFromAssets(0, -1, themeDir + config.get("card").get("right"));

        String bg = config.get("card").get("bg") ;
        int alpha = Integer.valueOf(config.get("card").get("alpha"));
        Bitmap bgBitmap = null ;
        if (bg.startsWith("#") == false) {
            bgBitmap = skinner.getBitmapFromAssets(0, -1, themeDir + bg);
        }

        BitmapDrawable bitmapDrawable = new BitmapDrawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                setBounds(0, 0, w, h);
                if (bg.startsWith("#"))
                    canvas.drawColor(Color.parseColor(bg));

                Log.d(TAG, "draw: " + String.format("topright %d x %d", topRight.getWidth(), topRight.getHeight()));

                int painted = 0 ;
                int tWidth = top.getWidth();
                for (painted = 0 ; painted + tWidth < w + tWidth ; painted = painted + tWidth) {
                    canvas.drawBitmap(top, painted, 0, paint);
                }

                tWidth = bottom.getWidth();
                int bottomHeight = h - bottom.getHeight();
                for (painted = 0 ; painted + tWidth < w + tWidth ; painted = painted + tWidth) {
                    canvas.drawBitmap(bottom, painted, bottomHeight, paint);
                }

                int tHeight = left.getHeight() ;
                for (painted = 0 ; painted + tHeight < h + tHeight; painted = painted + tHeight) {
                    canvas.drawBitmap(left, 0, painted, paint);
                }

                tHeight = right.getHeight() ;
                bottomHeight = w - right.getWidth() ;
                for (painted = 0 ; painted + tHeight < h + tHeight; painted = painted + tHeight) {
                    canvas.drawBitmap(right, bottomHeight, painted, paint);
                }

                canvas.drawBitmap(topLeft, 0, 0, paint);
                canvas.drawBitmap(topRight, layout.getWidth() - topRight.getWidth() , 0, paint);
                canvas.drawBitmap(bottomLeft, 0, h - bottomLeft.getHeight(), paint);
                canvas.drawBitmap(bottomRight, w - bottomRight.getWidth(), h - bottomRight.getHeight(), paint);
            }

            @Override
            public void setColorFilter(@Nullable ColorFilter colorFilter) {

            }

            @Override
            public int getOpacity() {
                return 0;
            }
        } ;

        bitmapDrawable.setAlpha(alpha);
        layout.setBackground(bitmapDrawable);
        layout.getBackground().setAlpha(alpha);
    }

    boolean hasKnob () {
        return config.get ("knob") == null ? true : false ;
    }

    void knob (SeekBar seekBar, int knobSize, int min, int max, int value) {
        BitmapDrawable drawable = bitmapDrawable("knobs", String.valueOf(knobSize));
        RotateDrawable rotateDrawable = new RotateDrawable();
        rotateDrawable.setDrawable(drawable);
        rotateDrawable.setFromDegrees(min);
        rotateDrawable.setToDegrees(max);
        rotateDrawable.setLevel(value);
        seekBar.setBackground(drawable);
    }

    void rotate (View view, int knobSize, float angle) {
        Bitmap bitmap = skinner.getBitmapFromAssets(0, 0,
                themeDir + config.get("knobs").get(String.valueOf(knobSize)));
        view.setBackground(new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                canvas.drawBitmap(bitmap, 0, 0, paint);
                canvas.rotate(angle);
                canvas.restore();
            }

            @Override
            public void setAlpha(int alpha) {

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

    void rotary (RotarySeekbar seekBar, int knobSize, float min, float max, float value) {
        Bitmap bitmap = skinner.getBitmapFromAssets (0, 0, themeDir + config.get("knobs").get(String.valueOf(knobSize)));
        RotateDrawable rotateDrawable ;
        seekBar.setBackground(new BitmapDrawable(bitmap));
        seekBar.setMaxValue(max);
        seekBar.setMinValue(min);
        seekBar.setValue(value);
        seekBar.setRotation((float) value/max);
        Log.d(TAG, "rotary: " + String.format(
                "[%f %f]: %f (%f)", min, max, value, seekBar.valueToRotation()
        ));
    }
}
