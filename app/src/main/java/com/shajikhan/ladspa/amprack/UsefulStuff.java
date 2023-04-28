package com.shajikhan.ladspa.amprack;

import android.util.Log;

public class UsefulStuff {
    public static void printBackTrace () {
        Throwable throwable = new Throwable().fillInStackTrace() ;
        StackTraceElement [] stackTraceElements = throwable.getStackTrace();
        for (int a = 0 ; a < stackTraceElements.length ; a ++) {
            Log.d("Backtrace", String.format("[%d] %s", a, stackTraceElements [a].getMethodName())) ;
        }
    }
}
