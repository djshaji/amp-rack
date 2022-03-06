#ifndef __PLUGIN_H
#define __PLUGIN_H
#include <ladspa.h>
#include <cstddef>
#include <logging_macros.h>
#include <vector>

#include "PluginControl.h"

class Plugin {
    LADSPA_Data ** portControls ;
    unsigned long sampleRate ;
public:
    bool active = true ;
    LADSPA_Data run_adding_gain = 1 ;
    std::vector <PluginControl *> pluginControls ;
    const LADSPA_Descriptor * descriptor ;
    int inputPort = -1;
    int inputPort2 = -1;
    int outputPort = -1;
    int outputPort2 = -1;
    LADSPA_Data dummy_output_control_port = 0; // from th pulseaudio ladspa sink module
    LADSPA_Handle *handle ;
    Plugin(const LADSPA_Descriptor * descriptor, unsigned long _sampleRate);

    void print();
};

#endif // __PLUGIN_H