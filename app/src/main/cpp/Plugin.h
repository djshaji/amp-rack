#ifndef __PLUGIN_H
#define __PLUGIN_H
#include <ladspa.h>
#include <lv2.h>

#include <cstddef>
#include <fstream>
#include <logging_macros.h>
#include <vector>

#include "android/asset_manager.h"
#include "android/asset_manager_jni.h"

#include "PluginControl.h"
#include "SharedLibrary.h"
#include "json.hpp"

class Plugin {
    LADSPA_Data ** portControls ;
    unsigned long sampleRate ;
public:
    bool active = true ;
    SharedLibrary::PluginType type ;
    LADSPA_Data run_adding_gain = 1 ;
    std::vector <PluginControl *> pluginControls ;
    const LADSPA_Descriptor * descriptor ;
    const LV2_Descriptor * lv2Descriptor;
    SharedLibrary * sharedLibrary;
    int inputPort = -1;
    int inputPort2 = -1;
    int outputPort = -1;
    int outputPort2 = -1;
    LADSPA_Data dummy_output_control_port = 0; // from th pulseaudio ladspa sink module
    LADSPA_Handle *handle ;
    Plugin(const LADSPA_Descriptor * descriptor, unsigned long _sampleRate, SharedLibrary::PluginType _type = SharedLibrary::LADSPA);
    void print();

    void free();
    std::string getLV2JSON (std::string pluginName);

    void load();

    int addPluginControl(const LV2_Descriptor *_descriptor, nlohmann::json _j);
};

template<class UnaryFunction>
void recursive_iterate(const Plugin &plugin, const nlohmann::json& j, UnaryFunction f)
{
    for(auto it = j.begin(); it != j.end(); ++it)
    {
        if (it->is_structured())
        {
            recursive_iterate(plugin, *it, f);
        }
        else
        {
            f(plugin, it);
        }
    }
}

#endif // __PLUGIN_H