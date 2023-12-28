package com.shajikhan.ladspa.amprack;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import io.grpc.okhttp.internal.Util;

public class Skinner {
    String TAG = getClass().getSimpleName();
    MainActivity mainActivity ;
//    int sanityCheck = 0 ;
    SkinEngine skinEngine ;

    DisplayMetrics displayMetrics = new DisplayMetrics();
    float density = 1 ;
    float UPSCALE_FACTOR = 1.43f ;

    void init () {
        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        density = displayMetrics.scaledDensity;
        Log.d(TAG, "Skinner: "+ String.format(
                "density: %f upscale factor: %f",
                density,
                UPSCALE_FACTOR
        ));

    }

    Skinner (MainActivity _mainActivity) {
        mainActivity = _mainActivity ;
        skinEngine = mainActivity.skinEngine ;
    }

    public int convertDpToPixel(float dp){
        double px = dp * (displayMetrics.densityDpi / 160D);
        return (int) Math.round(px);
    }

    public Bitmap getBitmap (float x, float y, float width, float height, int resource) {
        Log.d(TAG, String.format("getBitmap:  %f , %f => %f x %f", x,y,width,height) );
        Bitmap mBitmap = BitmapFactory.decodeResource(mainActivity.getResources(), resource) ;
        Log.d(TAG, String.format("getBitmap: image dimensions: %d x %d", mBitmap.getWidth(), mBitmap.getHeight()));
        if (x + width > mBitmap.getWidth())
            x = mBitmap.getWidth() - width ;
        try {
            mBitmap = Bitmap.createBitmap(mBitmap, convertDpToPixel(x), convertDpToPixel(y), convertDpToPixel(width), convertDpToPixel(height));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, String.format("getBitmap: failed function call %d %d %d %d",
                    convertDpToPixel(x), convertDpToPixel(y), convertDpToPixel(width), convertDpToPixel(height)));
            return mBitmap;
        }

        return mBitmap ;
    }

    public Bitmap upscaleBitmap (Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap,
                (int)((float) bitmap.getWidth() * UPSCALE_FACTOR),
                (int)((float) bitmap.getHeight() * UPSCALE_FACTOR),
                true
        );
    }

    public Bitmap upscaleBitmapEx (Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap,
                (int)((float) bitmap.getWidth() * UPSCALE_FACTOR) + (int) density,
                (int)((float) bitmap.getHeight() * UPSCALE_FACTOR) + (int) density,
                true
        );
    }

    public Bitmap getBitmapFromAssets (int width, int height, String filename) {
//        Log.d(TAG, "getBitmapFromAssets() called with: width = [" + width + "], height = [" + height + "], filename = [" + filename + "]");
//        UsefulStuff.printBackTrace();
        InputStream assetFilename = null;
        Bitmap mBitmap = null;

        try {
            if (! skinEngine.custom) {
                if (skinEngine.bitmaps.containsKey(filename))
                    mBitmap = skinEngine.bitmaps.get(filename) ;
                else {
                    assetFilename = mainActivity.getAssets().open(filename);
                    mBitmap = BitmapFactory.decodeStream(assetFilename);
                }
                } else {
                if (skinEngine.custom && skinEngine.bitmaps.containsKey(filename))
                    mBitmap = skinEngine.bitmaps.get(filename);
                else {
                    assetFilename = mainActivity.getContentResolver().openInputStream(skinEngine.themeFiles.get(filename));
                    mBitmap = BitmapFactory.decodeStream(assetFilename);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "getBitmapFromAssets: unable to load " + filename, e);
            return null ;
        }

        if (width == 0 || height == 0) {
            Log.d(TAG, "getBitmapFromAssets: returning " +
                    String.format ("[%s: %dx%d]",
                            filename, mBitmap.getWidth(), mBitmap.getHeight()));
            try {
                if (assetFilename != null)
                    assetFilename.close();
            } catch (IOException e) {
                Log.e(TAG, "getBitmapFromAssets: ", e);
            }
            if (!skinEngine.bitmaps.containsKey(filename))
                skinEngine.bitmaps.put(filename, mBitmap.copy(mBitmap.getConfig(), true));
            return mBitmap;
        }

        if (width == -1) width = (int) (height * ((float) mBitmap.getWidth() / (float) mBitmap.getHeight()));
        if (height == -1) height = (int) (width * ((float) mBitmap.getHeight() / (float) mBitmap.getWidth()));

//        Log.d(TAG, "getBitmapFromAssets: " +
//                String.format("scaling from %dx%d to %dx%d",
//                        mBitmap.getWidth(), mBitmap.getHeight(),
//                        width, height));
        try {
            if (assetFilename != null)
                assetFilename.close();
        } catch (IOException e) {
            Log.e(TAG, "getBitmapFromAssets: ", e);
        }
        return Bitmap.createScaledBitmap(mBitmap,
                (int) width,
                (int) height,
                true
        );

    }

    public Bitmap getBitmapFromAssets1 (float x, float y, float width, float height, String filename) {
        Log.d(TAG, String.format("getBitmap:  %f , %f => %f x %f", x,y,width,height) );
        InputStream assetFilename ;
        Bitmap mBitmap ;

        try {
             assetFilename = mainActivity.getAssets().open(filename) ;
             mBitmap = BitmapFactory.decodeStream(assetFilename);
        } catch (IOException e) {
            Log.e(TAG, "getBitmapFromAssets: unable to load " + filename, e);
            return null ;
        }

        if (width == -1) width = height * (mBitmap.getWidth() / mBitmap.getHeight());
        if (height == -1) height = width * (mBitmap.getHeight() / mBitmap.getWidth());

        Log.d(TAG, String.format("getBitmap: image dimensions: %d x %d", mBitmap.getWidth(), mBitmap.getHeight()));
        if (x + width > mBitmap.getWidth())
            x = mBitmap.getWidth() - width ;
        try {
            mBitmap = Bitmap.createBitmap(mBitmap, convertDpToPixel(x), convertDpToPixel(y), convertDpToPixel(width), convertDpToPixel(height));
        } catch (IllegalArgumentException e) {
            Log.e(TAG, String.format("getBitmap: failed function call %d %d %d %d",
                    convertDpToPixel(x), convertDpToPixel(y), convertDpToPixel(width), convertDpToPixel(height)));
            Log.e(TAG, "getBitmapFromAssets: ", e);
            return mBitmap;
        }

        return mBitmap ;
    }

    int pixelToDp (int px) {
        return px / (mainActivity.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public Bitmap getBitmap (Uri uri) {
        Bitmap mBitmap = null ;
        try {
            InputStream assetFilename = mainActivity.getContentResolver().openInputStream(uri);
            mBitmap = BitmapFactory.decodeStream(assetFilename);
            assetFilename.close();
        } catch (IOException e) {
            Log.e(TAG, "getBitmap: ", e);
        }

        return mBitmap;
    }
}
