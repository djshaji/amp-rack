#ifndef SHARED_LIBRARY_H
#define SHARED_LIBRARY_H

#include <dlfcn.h>
#include <logging_macros.h>
#include <cstring>
#include <string>
#include <vector>
#include "ladspa.h"

class SharedLibrary {
public:
    SharedLibrary(char * plugin_file);

    std::string so_file ;
    std::vector<const LADSPA_Descriptor *> descriptors;
    int total_plugins = 0 ;
    void * dl_handle = NULL;
    unsigned long sampleRate ;
    LADSPA_Descriptor_Function descriptorFunction ;

    void setSampleRate(unsigned long _sampleRate);
    char *load(void);
};

#endif // SHARED_LIBRARY_H