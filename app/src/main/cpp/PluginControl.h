#ifndef __PLUGIN_CONTROL_H
#define __PLUGIN_CONTROL_H

#include <cstdlib>
#include <logging_macros.h>
#include "ladspa.h"

class PluginControl {

    unsigned long port;
//    unsigned long ctrl;
    const char *name;
    const LADSPA_PortDescriptor *desc;
    const LADSPA_PortRangeHint *hint;
    /* values selected in the interface */
    LADSPA_Data sel;
    /* value range */
    LADSPA_Data min;
    LADSPA_Data max;
    LADSPA_Data default_value = 1; // 1 == no change in signal
    struct { LADSPA_Data fine; LADSPA_Data coarse; } inc;
    unsigned long sample_rate = 48000;

    enum Type {
        FLOAT = 0,
        INT = 1,
        TOGGLE = 2
    };

public:

    /* value in the plugin */
    LADSPA_Data val;
    LADSPA_Data *def;
    Type type ;

    LADSPA_Data control_rounding(LADSPA_Data _val);

    void setValue(float value);

    void setSampleRate(unsigned long rate);

    LADSPA_Data getMin();

    LADSPA_Data getMax();

    LADSPA_Data getDefault();

    LADSPA_Data getValue();

    PluginControl(const LADSPA_Descriptor *descriptor, int _port);

    void print();
};


#endif // __PLUGIN_CONTROL_H