#ifndef SHARED_LIBRARY_H
#define SHARED_LIBRARY_H

#include <dlfcn.h>
#include <logging_macros.h>
#include <cstring>
#include <string>
#include <vector>
#include "ladspa.h"
#include "lv2.h"

class SharedLibrary {
public:
    typedef enum {
        LADSPA,
        LV2
    } PluginType ;

    SharedLibrary(char * plugin_file, PluginType type = LADSPA);
    PluginType type = LADSPA ; // by default
    std::string so_file ;
    std::vector<const LADSPA_Descriptor *> descriptors;
    std::vector<const LV2_Descriptor *> lv2_descriptors;

    int total_plugins = 0 ;
    void * dl_handle = NULL;
    unsigned long sampleRate ;
    LADSPA_Descriptor_Function descriptorFunction ;
    LV2_Descriptor_Function lv2DescriptorFunction ;

    void setSampleRate(unsigned long _sampleRate);
    char *load();

    bool plugin_is_valid(const LADSPA_Descriptor *descriptor);

    void unload();
};

#endif // SHARED_LIBRARY_H