package com.shajikhan.ladspa.tubex;

public class NativeLib {

    // Used to load the 'tubex' library on application startup.
    static {
        System.loadLibrary("tubex");
    }

    /**
     * A native method that is implemented by the 'tubex' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}