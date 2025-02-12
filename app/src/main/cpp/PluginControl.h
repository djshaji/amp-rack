#ifndef __PLUGIN_CONTROL_H
#define __PLUGIN_CONTROL_H

#include <cstdlib>
#include "logging_macros.h"
#include "ladspa.h"
#include "lv2.h"
#include "lv2/atom/atom.h"
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
        TOGGLE = 2,
        /*
        ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⠂⠀⠀⠀⢀⣠⠔⠈⠀⠀⠀⠀⠀⠀⠀
        ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⠎⠀⠀⠀⣠⡶⠛⠁⠀⠀⠀⢀⣠⠄⠀⠀⠀
        ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⡾⠁⠀⢀⣴⡿⠋⠀⠀⢀⣠⣴⠿⠋⠁⠀⠀⠀⠀
        ⠀⠀⠀⠀⠀⠀⠀⠀⢀⣴⠟⠀⢀⣴⡿⠋⠀⢀⣠⣾⡿⠋⠁⠀⠀⠀⠀⠀⠀⠀
        ⠀⠀⠀⠀⠀⠀⠀⣠⣾⠏⢀⣴⡿⠋⠀⣠⣶⣿⠟⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        ⠀⠀⠀⠀⠀⠀⣴⡿⠁⣴⡿⠋⢀⣴⣾⡿⠋⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        ⠀⠀⠀⠀⢀⣼⠋⢠⣾⠟⠁⣴⣿⠟⠋⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        ⠀⠀⠀⠀⡾⠁⣴⡿⠁⣠⣾⠟⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        ⠀⢠⣶⣀⡅⠘⠋⢰⣾⡿⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        ⠀⣿⣿⣿⣷⣾⣇⣈⣁⣠⣦⡀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        ⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⡿⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        ⠀⣿⣿⣿⣿⣿⣿⣿⣭⣥⣴⡆⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        ⠀⣿⣿⢿⣿⣿⣿⣿⣿⣿⠟⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
        ⠀⠉⠀⠀⠀⠉⠉⠉⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
         */
        ATOM = 3
    };

public:
    unsigned long port;
    uint32_t urid ;
    LADSPA_Data min;
    LADSPA_Data max;
    LADSPA_Data default_value = 1; // 1 == no change in signal

    bool isLogarithmic = false;

    /* value in the plugin */
    LADSPA_Data val;
    LADSPA_Data *def;
    LADSPA_Data presetValue = -1;
    Type type ;
    std::string lv2_name ;
    bool name_allocated = false ;
    LV2_Atom_Sequence * lv2AtomSequence ;

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
