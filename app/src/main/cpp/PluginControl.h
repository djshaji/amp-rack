#ifndef __PLUGIN_CONTROL_H
#define __PLUGIN_CONTROL_H

#include <cstdlib>
#include "logging_macros.h"
#include "ladspa.h"
#include "lv2.h"
#include "json.hpp"

class PluginControl {
    const LADSPA_PortDescriptor *desc;
    const LADSPA_PortRangeHint *hint;
    /* values selected in the interface */
    LADSPA_Data sel;
    /* value range */
    struct { LADSPA_Data fine; LADSPA_Data coarse; } inc;
    unsigned long sample_rate = 48000;

    enum Type {
        FLOAT = 0,
        INT = 1,
        TOGGLE = 2
    };

public:
    unsigned long port;
    LADSPA_Data min;
    LADSPA_Data max;
    LADSPA_Data default_value = 1; // 1 == no change in signal

    /* value in the plugin */
    LADSPA_Data val;
    LADSPA_Data *def;
    LADSPA_Data presetValue = -1;
    Type type ;
    std::string lv2_name ;

    LADSPA_Data control_rounding(LADSPA_Data _val);

    void setValue(float value);

    void setSampleRate(unsigned long rate);

    LADSPA_Data getMin();

    LADSPA_Data getMax();

    LADSPA_Data getDefault();

    LADSPA_Data getValue();

    PluginControl(const LADSPA_Descriptor *descriptor, int _port);
    PluginControl(const LV2_Descriptor *descriptor, nlohmann::json j);

    void print();

//    unsigned long ctrl;
    const char *name;

    void freeMemory();

    void setPresetValue(float value);
};


#endif // __PLUGIN_CONTROL_H