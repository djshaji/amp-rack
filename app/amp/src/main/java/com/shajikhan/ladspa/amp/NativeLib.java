package com.shajikhan.ladspa.amp;

public class NativeLib {

    // Used to load the 'amp' library on application startup.
    static {
        System.loadLibrary("amp");
    }

    /**
     * A native method that is implemented by the 'amp' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}