package com.shajikhan.ladspa.amprack;

import android.view.View;

public class MIDIControl {
    enum Type {
        NUMBER,
        TOGGLE
    } ;

    enum Scope {
        UI,
        PLUGIN
    } ;

    View view ;
    int plugin ;
    int control ;
    Type type ;
    Scope scope ;

    int channel ;
    int program ;
}
