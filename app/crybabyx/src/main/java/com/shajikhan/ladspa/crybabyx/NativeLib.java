package com.shajikhan.ladspa.crybabyx;

public class NativeLib {

    // Used to load the 'crybabyx' library on application startup.
    static {
        System.loadLibrary("crybabyx");
    }

    /**
     * A native method that is implemented by the 'crybabyx' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}