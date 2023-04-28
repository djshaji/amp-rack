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
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.checkerframework.checker.units.qual.K;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SkinEngine {
    MainActivity mainActivity ;
    String TAG = getClass().getSimpleName() ;
    String theme = "Adwaita" ;
    int nativeTheme = R.style.Theme_AmpRack;
    String themeDir = "themes/Adwaita/";
    HashMap <String, HashMap <String, String>> config = new HashMap<>();
    JSONObject jsonConfig ;
    Skinner skinner ;
    public boolean custom = false ;
    DisplayMetrics displayMetrics = new DisplayMetrics();
    Display display ;
    Uri themeUri ;
    HashMap<String, Uri> themeFiles = new HashMap<>();
    HashMap<String, Bitmap> bitmaps = new HashMap<>();
    int screenWidth = 1800, screenHeight = 2400 ;
    float scaleFactor = 1 ;

    Paint paint ;

    static void setColorScheme (MainActivity mainActivity, String colorScheme) {
        switch (colorScheme) {
            case "Red.Pink":
                mainActivity.setTheme (R.style.Red_Pink);
                break;

            case "Red.Purple":
                mainActivity.setTheme (R.style.Red_Purple);
                break;

            case "Red.DeepPurple":
                mainActivity.setTheme (R.style.Red_DeepPurple);
                break;

            case "Red.Indigo":
                mainActivity.setTheme (R.style.Red_Indigo);
                break;

            case "Red.Blue":
                mainActivity.setTheme (R.style.Red_Blue);
                break;

            case "Red.LightBlue":
                mainActivity.setTheme (R.style.Red_LightBlue);
                break;

            case "Red.Cyan":
                mainActivity.setTheme (R.style.Red_Cyan);
                break;

            case "Red.Teal":
                mainActivity.setTheme (R.style.Red_Teal);
                break;

            case "Red.Green":
                mainActivity.setTheme (R.style.Red_Green);
                break;

            case "Red.LightGreen":
                mainActivity.setTheme (R.style.Red_LightGreen);
                break;

            case "Red.Lime":
                mainActivity.setTheme (R.style.Red_Lime);
                break;

            case "Red.Yellow":
                mainActivity.setTheme (R.style.Red_Yellow);
                break;

            case "Red.Orange":
                mainActivity.setTheme (R.style.Red_Orange);
                break;

            case "Red.DeepOrange":
                mainActivity.setTheme (R.style.Red_DeepOrange);
                break;

            case "Red.Brown":
                mainActivity.setTheme (R.style.Red_Brown);
                break;

            case "Red.Gray":
                mainActivity.setTheme (R.style.Red_Gray);
                break;

            case "Red.BlueGray":
                mainActivity.setTheme (R.style.Red_BlueGray);
                break;

            case "Pink.Red":
                mainActivity.setTheme (R.style.Pink_Red);
                break;

            case "Pink.Purple":
                mainActivity.setTheme (R.style.Pink_Purple);
                break;

            case "Pink.DeepPurple":
                mainActivity.setTheme (R.style.Pink_DeepPurple);
                break;

            case "Pink.Indigo":
                mainActivity.setTheme (R.style.Pink_Indigo);
                break;

            case "Pink.Blue":
                mainActivity.setTheme (R.style.Pink_Blue);
                break;

            case "Pink.LightBlue":
                mainActivity.setTheme (R.style.Pink_LightBlue);
                break;

            case "Pink.Cyan":
                mainActivity.setTheme (R.style.Pink_Cyan);
                break;

            case "Pink.Teal":
                mainActivity.setTheme (R.style.Pink_Teal);
                break;

            case "Pink.Green":
                mainActivity.setTheme (R.style.Pink_Green);
                break;

            case "Pink.LightGreen":
                mainActivity.setTheme (R.style.Pink_LightGreen);
                break;

            case "Pink.Lime":
                mainActivity.setTheme (R.style.Pink_Lime);
                break;

            case "Pink.Yellow":
                mainActivity.setTheme (R.style.Pink_Yellow);
                break;

            case "Pink.Orange":
                mainActivity.setTheme (R.style.Pink_Orange);
                break;

            case "Pink.DeepOrange":
                mainActivity.setTheme (R.style.Pink_DeepOrange);
                break;

            case "Pink.Brown":
                mainActivity.setTheme (R.style.Pink_Brown);
                break;

            case "Pink.Gray":
                mainActivity.setTheme (R.style.Pink_Gray);
                break;

            case "Pink.BlueGray":
                mainActivity.setTheme (R.style.Pink_BlueGray);
                break;

            case "Purple.Red":
                mainActivity.setTheme (R.style.Purple_Red);
                break;

            case "Purple.Pink":
                mainActivity.setTheme (R.style.Purple_Pink);
                break;

            case "Purple.Indigo":
                mainActivity.setTheme (R.style.Purple_Indigo);
                break;

            case "Purple.Blue":
                mainActivity.setTheme (R.style.Purple_Blue);
                break;

            case "Purple.LightBlue":
                mainActivity.setTheme (R.style.Purple_LightBlue);
                break;

            case "Purple.Cyan":
                mainActivity.setTheme (R.style.Purple_Cyan);
                break;

            case "Purple.Teal":
                mainActivity.setTheme (R.style.Purple_Teal);
                break;

            case "Purple.Green":
                mainActivity.setTheme (R.style.Purple_Green);
                break;

            case "Purple.LightGreen":
                mainActivity.setTheme (R.style.Purple_LightGreen);
                break;

            case "Purple.Lime":
                mainActivity.setTheme (R.style.Purple_Lime);
                break;

            case "Purple.Yellow":
                mainActivity.setTheme (R.style.Purple_Yellow);
                break;

            case "Purple.Orange":
                mainActivity.setTheme (R.style.Purple_Orange);
                break;

            case "Purple.DeepOrange":
                mainActivity.setTheme (R.style.Purple_DeepOrange);
                break;

            case "Purple.Brown":
                mainActivity.setTheme (R.style.Purple_Brown);
                break;

            case "Purple.Gray":
                mainActivity.setTheme (R.style.Purple_Gray);
                break;

            case "Purple.BlueGray":
                mainActivity.setTheme (R.style.Purple_BlueGray);
                break;

            case "DeepPurple.Red":
                mainActivity.setTheme (R.style.DeepPurple_Red);
                break;

            case "DeepPurple.Pink":
                mainActivity.setTheme (R.style.DeepPurple_Pink);
                break;

            case "DeepPurple.Indigo":
                mainActivity.setTheme (R.style.DeepPurple_Indigo);
                break;

            case "DeepPurple.Blue":
                mainActivity.setTheme (R.style.DeepPurple_Blue);
                break;

            case "DeepPurple.LightBlue":
                mainActivity.setTheme (R.style.DeepPurple_LightBlue);
                break;

            case "DeepPurple.Cyan":
                mainActivity.setTheme (R.style.DeepPurple_Cyan);
                break;

            case "DeepPurple.Teal":
                mainActivity.setTheme (R.style.DeepPurple_Teal);
                break;

            case "DeepPurple.Green":
                mainActivity.setTheme (R.style.DeepPurple_Green);
                break;

            case "DeepPurple.LightGreen":
                mainActivity.setTheme (R.style.DeepPurple_LightGreen);
                break;

            case "DeepPurple.Lime":
                mainActivity.setTheme (R.style.DeepPurple_Lime);
                break;

            case "DeepPurple.Yellow":
                mainActivity.setTheme (R.style.DeepPurple_Yellow);
                break;

            case "DeepPurple.Orange":
                mainActivity.setTheme (R.style.DeepPurple_Orange);
                break;

            case "DeepPurple.DeepOrange":
                mainActivity.setTheme (R.style.DeepPurple_DeepOrange);
                break;

            case "DeepPurple.Brown":
                mainActivity.setTheme (R.style.DeepPurple_Brown);
                break;

            case "DeepPurple.Gray":
                mainActivity.setTheme (R.style.DeepPurple_Gray);
                break;

            case "DeepPurple.BlueGray":
                mainActivity.setTheme (R.style.DeepPurple_BlueGray);
                break;

            case "Indigo.Red":
                mainActivity.setTheme (R.style.Indigo_Red);
                break;

            case "Indigo.Pink":
                mainActivity.setTheme (R.style.Indigo_Pink);
                break;

            case "Indigo.Purple":
                mainActivity.setTheme (R.style.Indigo_Purple);
                break;

            case "Indigo.DeepPurple":
                mainActivity.setTheme (R.style.Indigo_DeepPurple);
                break;

            case "Indigo.Blue":
                mainActivity.setTheme (R.style.Indigo_Blue);
                break;

            case "Indigo.LightBlue":
                mainActivity.setTheme (R.style.Indigo_LightBlue);
                break;

            case "Indigo.Cyan":
                mainActivity.setTheme (R.style.Indigo_Cyan);
                break;

            case "Indigo.Teal":
                mainActivity.setTheme (R.style.Indigo_Teal);
                break;

            case "Indigo.Green":
                mainActivity.setTheme (R.style.Indigo_Green);
                break;

            case "Indigo.LightGreen":
                mainActivity.setTheme (R.style.Indigo_LightGreen);
                break;

            case "Indigo.Lime":
                mainActivity.setTheme (R.style.Indigo_Lime);
                break;

            case "Indigo.Yellow":
                mainActivity.setTheme (R.style.Indigo_Yellow);
                break;

            case "Indigo.Orange":
                mainActivity.setTheme (R.style.Indigo_Orange);
                break;

            case "Indigo.DeepOrange":
                mainActivity.setTheme (R.style.Indigo_DeepOrange);
                break;

            case "Indigo.Brown":
                mainActivity.setTheme (R.style.Indigo_Brown);
                break;

            case "Indigo.Gray":
                mainActivity.setTheme (R.style.Indigo_Gray);
                break;

            case "Indigo.BlueGray":
                mainActivity.setTheme (R.style.Indigo_BlueGray);
                break;

            case "Blue.Red":
                mainActivity.setTheme (R.style.Blue_Red);
                break;

     case "Blue.Pink":
                mainActivity.setTheme (R.style.Blue_Pink);
                break;

            case "Blue.Purple":
                mainActivity.setTheme (R.style.Blue_Purple);
                break;

            case "Blue.DeepPurple":
                mainActivity.setTheme (R.style.Blue_DeepPurple);
                break;

            case "Blue.Indigo":
                mainActivity.setTheme (R.style.Blue_Indigo);
                break;

            case "Blue.Cyan":
                mainActivity.setTheme (R.style.Blue_Cyan);
                break;

            case "Blue.Teal":
                mainActivity.setTheme (R.style.Blue_Teal);
                break;

            case "Blue.Green":
                mainActivity.setTheme (R.style.Blue_Green);
                break;

            case "Blue.LightGreen":
                mainActivity.setTheme (R.style.Blue_LightGreen);
                break;

            case "Blue.Lime":
                mainActivity.setTheme (R.style.Blue_Lime);
                break;

            case "Blue.Yellow":
                mainActivity.setTheme (R.style.Blue_Yellow);
                break;

            case "Blue.Orange":
                mainActivity.setTheme (R.style.Blue_Orange);
                break;

            case "Blue.DeepOrange":
                mainActivity.setTheme (R.style.Blue_DeepOrange);
                break;

            case "Blue.Brown":
                mainActivity.setTheme (R.style.Blue_Brown);
                break;

            case "Blue.Gray":
                mainActivity.setTheme (R.style.Blue_Gray);
                break;

            case "LightBlue.Red":
                mainActivity.setTheme (R.style.LightBlue_Red);
                break;

            case "LightBlue.Pink":
                mainActivity.setTheme (R.style.LightBlue_Pink);
                break;

            case "LightBlue.Purple":
                mainActivity.setTheme (R.style.LightBlue_Purple);
                break;

            case "LightBlue.DeepPurple":
                mainActivity.setTheme (R.style.LightBlue_DeepPurple);
                break;

            case "LightBlue.Indigo":
                mainActivity.setTheme (R.style.LightBlue_Indigo);
                break;

            case "LightBlue.Cyan":
                mainActivity.setTheme (R.style.LightBlue_Cyan);
                break;

            case "LightBlue.Teal":
                mainActivity.setTheme (R.style.LightBlue_Teal);
                break;

            case "LightBlue.Green":
                mainActivity.setTheme (R.style.LightBlue_Green);
                break;

            case "LightBlue.LightGreen":
                mainActivity.setTheme (R.style.LightBlue_LightGreen);
                break;

            case "LightBlue.Lime":
                mainActivity.setTheme (R.style.LightBlue_Lime);
                break;

            case "LightBlue.Yellow":
                mainActivity.setTheme (R.style.LightBlue_Yellow);
                break;

            case "LightBlue.Orange":
                mainActivity.setTheme (R.style.LightBlue_Orange);
                break;

            case "LightBlue.DeepOrange":
                mainActivity.setTheme (R.style.LightBlue_DeepOrange);
                break;

            case "LightBlue.Brown":
                mainActivity.setTheme (R.style.LightBlue_Brown);
                break;

            case "LightBlue.Gray":
                mainActivity.setTheme (R.style.LightBlue_Gray);
                break;

            case "LightBlue.BlueGray":
                mainActivity.setTheme (R.style.LightBlue_BlueGray);
                break;

            case "Cyan.Red":
                mainActivity.setTheme (R.style.Cyan_Red);
                break;

            case "Cyan.Pink":
                mainActivity.setTheme (R.style.Cyan_Pink);
                break;

            case "Cyan.Purple":
                mainActivity.setTheme (R.style.Cyan_Purple);
                break;

            case "Cyan.DeepPurple":
                mainActivity.setTheme (R.style.Cyan_DeepPurple);
                break;

            case "Cyan.Indigo":
                mainActivity.setTheme (R.style.Cyan_Indigo);
                break;

            case "Cyan.Blue":
                mainActivity.setTheme (R.style.Cyan_Blue);
                break;

            case "Cyan.LightBlue":
                mainActivity.setTheme (R.style.Cyan_LightBlue);
                break;

            case "Cyan.Teal":
                mainActivity.setTheme (R.style.Cyan_Teal);
                break;

            case "Cyan.Green":
                mainActivity.setTheme (R.style.Cyan_Green);
                break;

            case "Cyan.LightGreen":
                mainActivity.setTheme (R.style.Cyan_LightGreen);
                break;

            case "Cyan.Lime":
                mainActivity.setTheme (R.style.Cyan_Lime);
                break;

            case "Cyan.Yellow":
                mainActivity.setTheme (R.style.Cyan_Yellow);
                break;

            case "Cyan.Orange":
                mainActivity.setTheme (R.style.Cyan_Orange);
                break;

            case "Cyan.DeepOrange":
                mainActivity.setTheme (R.style.Cyan_DeepOrange);
                break;

            case "Cyan.Brown":
                mainActivity.setTheme (R.style.Cyan_Brown);
                break;

            case "Cyan.Gray":
                mainActivity.setTheme (R.style.Cyan_Gray);
                break;

            case "Cyan.BlueGray":
                mainActivity.setTheme (R.style.Cyan_BlueGray);
                break;

            case "Teal.Red":
                mainActivity.setTheme (R.style.Teal_Red);
                break;

            case "Teal.Pink":
                mainActivity.setTheme (R.style.Teal_Pink);
                break;

            case "Teal.Purple":
                mainActivity.setTheme (R.style.Teal_Purple);
                break;

            case "Teal.DeepPurple":
                mainActivity.setTheme (R.style.Teal_DeepPurple);
                break;

            case "Teal.Indigo":
                mainActivity.setTheme (R.style.Teal_Indigo);
                break;

            case "Teal.Blue":
                mainActivity.setTheme (R.style.Teal_Blue);
                break;

            case "Teal.LightBlue":
                mainActivity.setTheme (R.style.Teal_LightBlue);
                break;

            case "Teal.Cyan":
                mainActivity.setTheme (R.style.Teal_Cyan);
                break;

            case "Teal.Green":
                mainActivity.setTheme (R.style.Teal_Green);
                break;

            case "Teal.LightGreen":
                mainActivity.setTheme (R.style.Teal_LightGreen);
                break;

            case "Teal.Lime":
                mainActivity.setTheme (R.style.Teal_Lime);
                break;

            case "Teal.Yellow":
                mainActivity.setTheme (R.style.Teal_Yellow);
                break;

            case "Teal.Orange":
                mainActivity.setTheme (R.style.Teal_Orange);
                break;

            case "Teal.DeepOrange":
                mainActivity.setTheme (R.style.Teal_DeepOrange);
                break;

            case "Teal.Brown":
                mainActivity.setTheme (R.style.Teal_Brown);
                break;

            case "Teal.Gray":
                mainActivity.setTheme (R.style.Teal_Gray);
                break;

            case "Teal.BlueGray":
                mainActivity.setTheme (R.style.Teal_BlueGray);
                break;

            case "Green.Red":
                mainActivity.setTheme (R.style.Green_Red);
                break;

            case "Green.Pink":
                mainActivity.setTheme (R.style.Green_Pink);
                break;

            case "Green.Purple":
                mainActivity.setTheme (R.style.Green_Purple);
                break;

            case "Green.DeepPurple":
                mainActivity.setTheme (R.style.Green_DeepPurple);
                break;

            case "Green.Indigo":
                mainActivity.setTheme (R.style.Green_Indigo);
                break;

            case "Green.Blue":
                mainActivity.setTheme (R.style.Green_Blue);
                break;

            case "Green.LightBlue":
                mainActivity.setTheme (R.style.Green_LightBlue);
                break;

            case "Green.Cyan":
                mainActivity.setTheme (R.style.Green_Cyan);
                break;

            case "Green.Teal":
                mainActivity.setTheme (R.style.Green_Teal);
                break;

            case "Green.Lime":
                mainActivity.setTheme (R.style.Green_Lime);
                break;

            case "Green.Yellow":
                mainActivity.setTheme (R.style.Green_Yellow);
                break;

            case "Green.Orange":
                mainActivity.setTheme (R.style.Green_Orange);
                break;

            case "Green.DeepOrange":
                mainActivity.setTheme (R.style.Green_DeepOrange);
                break;

            case "Green.Brown":
                mainActivity.setTheme (R.style.Green_Brown);
                break;

            case "Green.Gray":
                mainActivity.setTheme (R.style.Green_Gray);
                break;

            case "Green.BlueGray":
                mainActivity.setTheme (R.style.Green_BlueGray);
                break;

            case "LightGreen.Red":
                mainActivity.setTheme (R.style.LightGreen_Red);
                break;

            case "LightGreen.Pink":
                mainActivity.setTheme (R.style.LightGreen_Pink);
                break;

            case "LightGreen.Purple":
                mainActivity.setTheme (R.style.LightGreen_Purple);
                break;

            case "LightGreen.DeepPurple":
                mainActivity.setTheme (R.style.LightGreen_DeepPurple);
                break;

            case "LightGreen.Indigo":
                mainActivity.setTheme (R.style.LightGreen_Indigo);
                break;

            case "LightGreen.Blue":
                mainActivity.setTheme (R.style.LightGreen_Blue);
                break;

            case "LightGreen.LightBlue":
                mainActivity.setTheme (R.style.LightGreen_LightBlue);
                break;

            case "LightGreen.Cyan":
                mainActivity.setTheme (R.style.LightGreen_Cyan);
                break;

            case "LightGreen.Teal":
                mainActivity.setTheme (R.style.LightGreen_Teal);
                break;

            case "LightGreen.Lime":
                mainActivity.setTheme (R.style.LightGreen_Lime);
                break;

            case "LightGreen.Yellow":
                mainActivity.setTheme (R.style.LightGreen_Yellow);
                break;

            case "LightGreen.Orange":
                mainActivity.setTheme (R.style.LightGreen_Orange);
                break;

            case "LightGreen.DeepOrange":
                mainActivity.setTheme (R.style.LightGreen_DeepOrange);
                break;

            case "LightGreen.Brown":
                mainActivity.setTheme (R.style.LightGreen_Brown);
                break;

            case "LightGreen.Gray":
                mainActivity.setTheme (R.style.LightGreen_Gray);
                break;

            case "LightGreen.BlueGray":
                mainActivity.setTheme (R.style.LightGreen_BlueGray);
                break;

            case "Lime.Red":
                mainActivity.setTheme (R.style.Lime_Red);
                break;

            case "Lime.Pink":
                mainActivity.setTheme (R.style.Lime_Pink);
                break;

            case "Lime.Purple":
                mainActivity.setTheme (R.style.Lime_Purple);
                break;

            case "Lime.DeepPurple":
                mainActivity.setTheme (R.style.Lime_DeepPurple);
                break;

            case "Lime.Indigo":
                mainActivity.setTheme (R.style.Lime_Indigo);
                break;

            case "Lime.Blue":
                mainActivity.setTheme (R.style.Lime_Blue);
                break;

            case "Lime.LightBlue":
                mainActivity.setTheme (R.style.Lime_LightBlue);
                break;

            case "Lime.Cyan":
                mainActivity.setTheme (R.style.Lime_Cyan);
                break;

            case "Lime.Teal":
                mainActivity.setTheme (R.style.Lime_Teal);
                break;

            case "Lime.Green":
                mainActivity.setTheme (R.style.Lime_Green);
                break;

            case "Lime.LightGreen":
                mainActivity.setTheme (R.style.Lime_LightGreen);
                break;

            case "Lime.Yellow":
                mainActivity.setTheme (R.style.Lime_Yellow);
                break;

            case "Lime.Orange":
                mainActivity.setTheme (R.style.Lime_Orange);
                break;

            case "Lime.DeepOrange":
                mainActivity.setTheme (R.style.Lime_DeepOrange);
                break;

            case "Lime.Brown":
                mainActivity.setTheme (R.style.Lime_Brown);
                break;

            case "Lime.Gray":
                mainActivity.setTheme (R.style.Lime_Gray);
                break;

            case "Lime.BlueGray":
                mainActivity.setTheme (R.style.Lime_BlueGray);
                break;

            case "Yellow.Red":
                mainActivity.setTheme (R.style.Yellow_Red);
                break;

            case "Yellow.Pink":
                mainActivity.setTheme (R.style.Yellow_Pink);
                break;

            case "Yellow.Purple":
                mainActivity.setTheme (R.style.Yellow_Purple);
                break;

            case "Yellow.DeepPurple":
                mainActivity.setTheme (R.style.Yellow_DeepPurple);
                break;

            case "Yellow.Indigo":
                mainActivity.setTheme (R.style.Yellow_Indigo);
                break;

            case "Yellow.Blue":
                mainActivity.setTheme (R.style.Yellow_Blue);
                break;

            case "Yellow.LightBlue":
                mainActivity.setTheme (R.style.Yellow_LightBlue);
                break;

            case "Yellow.Cyan":
                mainActivity.setTheme (R.style.Yellow_Cyan);
                break;

            case "Yellow.Teal":
                mainActivity.setTheme (R.style.Yellow_Teal);
                break;

            case "Yellow.Green":
                mainActivity.setTheme (R.style.Yellow_Green);
                break;

            case "Yellow.LightGreen":
                mainActivity.setTheme (R.style.Yellow_LightGreen);
                break;

            case "Yellow.Lime":
                mainActivity.setTheme (R.style.Yellow_Lime);
                break;

            case "Yellow.Orange":
                mainActivity.setTheme (R.style.Yellow_Orange);
                break;

            case "Yellow.DeepOrange":
                mainActivity.setTheme (R.style.Yellow_DeepOrange);
                break;

            case "Yellow.Brown":
                mainActivity.setTheme (R.style.Yellow_Brown);
                break;

            case "Yellow.Gray":
                mainActivity.setTheme (R.style.Yellow_Gray);
                break;

            case "Yellow.BlueGray":
                mainActivity.setTheme (R.style.Yellow_BlueGray);
                break;

            case "Orange.Red":
                mainActivity.setTheme (R.style.Orange_Red);
                break;

            case "Orange.Pink":
                mainActivity.setTheme (R.style.Orange_Pink);
                break;

            case "Orange.Purple":
                mainActivity.setTheme (R.style.Orange_Purple);
                break;

            case "Orange.DeepPurple":
                mainActivity.setTheme (R.style.Orange_DeepPurple);
                break;

            case "Orange.Indigo":
                mainActivity.setTheme (R.style.Orange_Indigo);
                break;

            case "Orange.Blue":
                mainActivity.setTheme (R.style.Orange_Blue);
                break;

            case "Orange.LightBlue":
                mainActivity.setTheme (R.style.Orange_LightBlue);
                break;

            case "Orange.Cyan":
                mainActivity.setTheme (R.style.Orange_Cyan);
                break;

            case "Orange.Teal":
                mainActivity.setTheme (R.style.Orange_Teal);
                break;

            case "Orange.Green":
                mainActivity.setTheme (R.style.Orange_Green);
                break;

            case "Orange.LightGreen":
                mainActivity.setTheme (R.style.Orange_LightGreen);
                break;

            case "Orange.Lime":
                mainActivity.setTheme (R.style.Orange_Lime);
                break;

            case "Orange.Yellow":
                mainActivity.setTheme (R.style.Orange_Yellow);
                break;

            case "Orange.Brown":
                mainActivity.setTheme (R.style.Orange_Brown);
                break;

            case "Orange.Gray":
                mainActivity.setTheme (R.style.Orange_Gray);
                break;

            case "Orange.BlueGray":
                mainActivity.setTheme (R.style.Orange_BlueGray);
                break;

            case "DeepOrange.Red":
                mainActivity.setTheme (R.style.DeepOrange_Red);
                break;

            case "DeepOrange.Pink":
                mainActivity.setTheme (R.style.DeepOrange_Pink);
                break;

            case "DeepOrange.Purple":
                mainActivity.setTheme (R.style.DeepOrange_Purple);
                break;

            case "DeepOrange.DeepPurple":
                mainActivity.setTheme (R.style.DeepOrange_DeepPurple);
                break;

            case "DeepOrange.Indigo":
                mainActivity.setTheme (R.style.DeepOrange_Indigo);
                break;

            case "DeepOrange.Blue":
                mainActivity.setTheme (R.style.DeepOrange_Blue);
                break;

            case "DeepOrange.LightBlue":
                mainActivity.setTheme (R.style.DeepOrange_LightBlue);
                break;

            case "DeepOrange.Cyan":
                mainActivity.setTheme (R.style.DeepOrange_Cyan);
                break;

            case "DeepOrange.Teal":
                mainActivity.setTheme (R.style.DeepOrange_Teal);
                break;

            case "DeepOrange.Green":
                mainActivity.setTheme (R.style.DeepOrange_Green);
                break;

            case "DeepOrange.LightGreen":
                mainActivity.setTheme (R.style.DeepOrange_LightGreen);
                break;

            case "DeepOrange.Lime":
                mainActivity.setTheme (R.style.DeepOrange_Lime);
                break;

            case "DeepOrange.Yellow":
                mainActivity.setTheme (R.style.DeepOrange_Yellow);
                break;

            case "DeepOrange.Brown":
                mainActivity.setTheme (R.style.DeepOrange_Brown);
                break;

            case "DeepOrange.Gray":
                mainActivity.setTheme (R.style.DeepOrange_Gray);
                break;

            case "DeepOrange.BlueGray":
                mainActivity.setTheme (R.style.DeepOrange_BlueGray);
                break;

            case "Brown.Red":
                mainActivity.setTheme (R.style.Brown_Red);
                break;

            case "Brown.Pink":
                mainActivity.setTheme (R.style.Brown_Pink);
                break;

            case "Brown.Purple":
                mainActivity.setTheme (R.style.Brown_Purple);
                break;

            case "Brown.DeepPurple":
                mainActivity.setTheme (R.style.Brown_DeepPurple);
                break;

            case "Brown.Indigo":
                mainActivity.setTheme (R.style.Brown_Indigo);
                break;

            case "Brown.Blue":
                mainActivity.setTheme (R.style.Brown_Blue);
                break;

            case "Brown.LightBlue":
                mainActivity.setTheme (R.style.Brown_LightBlue);
                break;

            case "Brown.Cyan":
                mainActivity.setTheme (R.style.Brown_Cyan);
                break;

            case "Brown.Teal":
                mainActivity.setTheme (R.style.Brown_Teal);
                break;

            case "Brown.Green":
                mainActivity.setTheme (R.style.Brown_Green);
                break;

            case "Brown.LightGreen":
                mainActivity.setTheme (R.style.Brown_LightGreen);
                break;

            case "Brown.Lime":
                mainActivity.setTheme (R.style.Brown_Lime);
                break;

            case "Brown.Yellow":
                mainActivity.setTheme (R.style.Brown_Yellow);
                break;

            case "Brown.Orange":
                mainActivity.setTheme (R.style.Brown_Orange);
                break;

            case "Brown.DeepOrange":
                mainActivity.setTheme (R.style.Brown_DeepOrange);
                break;

            case "Brown.Gray":
                mainActivity.setTheme (R.style.Brown_Gray);
                break;

            case "Brown.BlueGray":
                mainActivity.setTheme (R.style.Brown_BlueGray);
                break;

            case "Gray.Red":
                mainActivity.setTheme (R.style.Gray_Red);
                break;

            case "Gray.Pink":
                mainActivity.setTheme (R.style.Gray_Pink);
                break;

            case "Gray.Purple":
                mainActivity.setTheme (R.style.Gray_Purple);
                break;

            case "Gray.DeepPurple":
                mainActivity.setTheme (R.style.Gray_DeepPurple);
                break;

            case "Gray.Indigo":
                mainActivity.setTheme (R.style.Gray_Indigo);
                break;

            case "Gray.Blue":
                mainActivity.setTheme (R.style.Gray_Blue);
                break;

            case "Gray.LightBlue":
                mainActivity.setTheme (R.style.Gray_LightBlue);
                break;

            case "Gray.Cyan":
                mainActivity.setTheme (R.style.Gray_Cyan);
                break;

            case "Gray.Teal":
                mainActivity.setTheme (R.style.Gray_Teal);
                break;

            case "Gray.Green":
                mainActivity.setTheme (R.style.Gray_Green);
                break;

            case "Gray.LightGreen":
                mainActivity.setTheme (R.style.Gray_LightGreen);
                break;

            case "Gray.Lime":
                mainActivity.setTheme (R.style.Gray_Lime);
                break;

            case "Gray.Yellow":
                mainActivity.setTheme (R.style.Gray_Yellow);
                break;

            case "Gray.Orange":
                mainActivity.setTheme (R.style.Gray_Orange);
                break;

            case "Gray.DeepOrange":
                mainActivity.setTheme (R.style.Gray_DeepOrange);
                break;

            case "Gray.Brown":
                mainActivity.setTheme (R.style.Gray_Brown);
                break;

            case "BlueGray.Red":
                mainActivity.setTheme (R.style.BlueGray_Red);
                break;

            case "BlueGray.Pink":
                mainActivity.setTheme (R.style.BlueGray_Pink);
                break;

            case "BlueGray.Purple":
                mainActivity.setTheme (R.style.BlueGray_Purple);
                break;

            case "BlueGray.DeepPurple":
                mainActivity.setTheme (R.style.BlueGray_DeepPurple);
                break;

            case "BlueGray.Indigo":
                mainActivity.setTheme (R.style.BlueGray_Indigo);
                break;

            case "BlueGray.LightBlue":
                mainActivity.setTheme (R.style.BlueGray_LightBlue);
                break;

            case "BlueGray.Cyan":
                mainActivity.setTheme (R.style.BlueGray_Cyan);
                break;

            case "BlueGray.Teal":
                mainActivity.setTheme (R.style.BlueGray_Teal);
                break;

            case "BlueGray.Green":
                mainActivity.setTheme (R.style.BlueGray_Green);
                break;

            case "BlueGray.LightGreen":
                mainActivity.setTheme (R.style.BlueGray_LightGreen);
                break;

            case "BlueGray.Lime":
                mainActivity.setTheme (R.style.BlueGray_Lime);
                break;

            case "BlueGray.Yellow":
                mainActivity.setTheme (R.style.BlueGray_Yellow);
                break;

            case "BlueGray.Orange":
                mainActivity.setTheme (R.style.BlueGray_Orange);
                break;

            case "BlueGray.DeepOrange":
                mainActivity.setTheme (R.style.BlueGray_DeepOrange);
                break;

            case "BlueGray.Brown":
                mainActivity.setTheme (R.style.BlueGray_Brown);
                break;
            default:
                mainActivity.setTheme (R.style.Theme_AmpRack);
        }
    }

    void setTheme (String _theme) {
        theme = _theme ;
        if (theme.startsWith("content://"))
            custom = true ;
        Log.d(TAG, "setTheme() called with: _theme = [" + _theme + "]");
        if (! custom) {
            themeDir = String.format("themes/%s/", theme);
            jsonConfig = ConnectGuitar.loadJSONFromAssetFile(mainActivity, themeDir + "theme.json");
        }
        else {
            themeUri = Uri.parse(theme);
            themeDir= "" ;
            getUriFileList(themeUri);
            Uri _json = themeFiles.get("theme.json");
            jsonConfig = ConnectGuitar.loadJSONFromFile(mainActivity, _json);
        }
        Log.d(TAG, "setTheme: " + jsonConfig.toString());
        load ();
    }

    SkinEngine (MainActivity _mainActivity) {
        mainActivity = _mainActivity ;
        skinner = new Skinner(mainActivity);
        skinner.skinEngine = this ;
        skinner.init();
        paint = new Paint();
        setTheme("Adwaita"); // sane default
        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        display = mainActivity.getWindowManager().getDefaultDisplay();
        screenWidth = display.getWidth() ;
        screenHeight = display.getHeight();
        scaleFactor = screenWidth / 1120f ;

        Log.d(TAG, "SkinEngine: " + displayMetrics);
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
            String customBg = mainActivity.defaultSharedPreferences.getString("background", null);
            Bitmap bg = skinner.getBitmapFromAssets( skinner.displayMetrics.widthPixels, -1, themeDir + wallpaper);
            if (customBg == null)
                wall.setImageBitmap(bg);
            else {
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(mainActivity.getContentResolver(), Uri.parse(customBg));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (bitmap != null) {
                    wall.setCropToPadding(true);
                    wall.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    wall.setImageBitmap(bitmap);
                }
                else
                    wall.setImageBitmap(bg);
            }
        }
    }

    void header (LinearLayout layout) {
        String hBg = config.get("header").get("bg");
        String textColor = config.get("button").get("text-color");
        TextView appTitle = mainActivity.findViewById(R.id.app_main_title);
        appTitle.setTextColor(Color.parseColor(textColor));

        ToggleButton record = mainActivity.findViewById(R.id.record_button);
        record.setTextColor(Color.parseColor(textColor));

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
        Drawable drawable = new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                int w = toggleButton.getWidth(), h = toggleButton.getHeight() ;
                Bitmap b ;
                try {
                    b = skinner.getBitmapFromAssets(w, -1, themeDir + finalOn);
                } catch (AssertionError ae) {
                    throw ae ;
                }

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
        } ;

        toggleButton.setBackground(drawable);
    }

    void toggleWithKey (ToggleButton toggleButton, String key, String onState, String offState, boolean state) {
//        UsefulStuff.printBackTrace();
        Log.d(TAG, "toggleWithKey() called with: toggleButton = [" + toggleButton + "], key = [" + key + "], onState = [" + onState + "], offState = [" + offState + "], state = [" + state + "]");
        if (config.get(key).get(onState) == null)
            return;
        String on = config.get(key).get(onState) ;
        if (! state) {
            on = config.get(key).get(offState);
        }

        String finalOn = on;
        Drawable drawable = new Drawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                Log.d(TAG, "toggle with key draw() called with: canvas = [" + canvas + "]");
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
        } ;

        toggleButton.setBackground(drawable);
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
//        mainActivity.setTheme(nativeTheme);
        String mTheme = mainActivity.defaultSharedPreferences.getString("color_scheme", "Theme.AmpRack") ;
        Log.d(TAG, "setNativeTheme: " + mTheme);
        setColorScheme(mainActivity, mTheme);
    }

    void cardText (TextView textView) {
        String c_ = config.get("card").get("text-color") ;
        if (c_ == null)
            c_ = config.get("button").get("text-color");
        if (c_ == null)
            return ;

        int color = Color.parseColor(c_);
        textView.setTextColor(color);
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

        Bitmap finalBgBitmap = bgBitmap;
        BitmapDrawable bitmapDrawable = new BitmapDrawable() {
            @Override
            public void draw(@NonNull Canvas canvas) {
                setBounds(0, 0, w, h);
                if (bg.startsWith("#"))
                    canvas.drawColor(Color.parseColor(bg));
                else {
                    int j = 0, i = 0 ;
                    boolean drawing = true ;
                    while (drawing) {
                        /*
                        Log.d(TAG, "draw: " + String.format(
                                "i: %d\tj: %d\twidth: %d\theight: %d",
                                i, j, canvas.getWidth(), canvas.getHeight()
                        ));

                         */
                        canvas.drawBitmap(finalBgBitmap, i, j, paint);
                        i += finalBgBitmap.getWidth() ;
                        if (i > canvas.getWidth()) {
                            i = 0 ;
                            j += finalBgBitmap.getHeight();

                            if (j > canvas.getHeight()) {
                                drawing = false;
                                break ;
                            }
                        }
                    }
                }

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

        /*
        if (config.get("card").get("padding") != null) {
            int padding = Integer.valueOf(config.get("card").get("padding"));
            layout.setPadding(padding, padding, padding, padding);
        }

         */
    }

    boolean hasKnob () {
        return jsonConfig.has("knobs");
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
        seekBar.mNeedleColor = Color.parseColor(config.get("knobs").get("thumbColor"));
        seekBar.mKnobColor = Color.parseColor(config.get("knobs").get("thumbColor"));
        Bitmap bitmap = skinner.getBitmapFromAssets (0, 0, themeDir + config.get("knobs").get(String.valueOf(knobSize)));
        /*
        if (displayMetrics.widthPixels < 1080) {
            Display d = mainActivity.getWindowManager().getDefaultDisplay();
            Log.d(TAG, "rotary: detected screen dimensions: " + String.format(
                    "%d x %d", d.getWidth(), d.getHeight()
            ));
            float aspectRatio = bitmap.getWidth() / bitmap.getHeight(),
                    bw = bitmap.getWidth() * (d.getWidth()/1080f),
                    bh = bw * aspectRatio ;

            Log.d(TAG, "rotary: scaled dimensions " +
                    String.format("%f %f %f",
                            aspectRatio, bw, bh));
            bitmap = Bitmap.createScaledBitmap(bitmap, (int) bw, (int) bh, true);
        }

         */
        seekBar.setBackground(new BitmapDrawable(bitmap));
        seekBar.setMaxValue(max);
        seekBar.setMinValue(min);
        seekBar.setValue(value);
        seekBar.setRotation((float) value/max);
        Log.d(TAG, "rotary: " + String.format(
                "[%f %f]: %f (%f)", min, max, value, seekBar.valueToRotation()
        ));
    }

    void getUriFileList (Uri uri) {
        DocumentFile root = DocumentFile.fromTreeUri(mainActivity, uri);
        DocumentFile [] files = root.listFiles() ;
        for (DocumentFile file: files) {
            Log.d(TAG, "getUriFileList: " + String.format(
                    "\t%s:\t%s",
                    file.getName(),
                    file.getUri()
            ));
            themeFiles.put(file.getName(), file.getUri());
            bitmaps.put(file.getName(), skinner.getBitmap(file.getUri()));
            if (file.isDirectory()) {
                DocumentFile [] _files = file.listFiles() ;
                for (DocumentFile _file: _files) {
                    Log.d(TAG, "getUriFileList: " + String.format(
                            "\t%s:\t%s",
                            _file.getName(),
                            _file.getUri()
                    ));
                    themeFiles.put(file.getName() + "/" + _file.getName(), _file.getUri());
                    bitmaps.put(file.getName() + "/" + _file.getName(), skinner.getBitmap(_file.getUri()));

                }
            }
        }
    }
}
