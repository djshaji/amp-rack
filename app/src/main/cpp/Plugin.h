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
    std::vector <PluginControl *> pluginControls ;
    const LADSPA_Descriptor * descriptor ;
    int inputPort = -1;
    int outputPort = -1;
    LADSPA_Handle *handle ;
    Plugin(const LADSPA_Descriptor * descriptor, unsigned long _sampleRate);
};

#endif // __PLUGIN_H