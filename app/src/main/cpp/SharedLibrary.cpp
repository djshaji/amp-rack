#include "SharedLibrary.h"

void SharedLibrary::setSampleRate (unsigned long _sampleRate) {
    sampleRate = _sampleRate ;
}

SharedLibrary::SharedLibrary (char * plugin_file) {
    so_file = std::string (plugin_file) ;
}

//> Returns NULL if ok, error otherwise
char * SharedLibrary::load (void) {
    IN ;
    dl_handle = dlopen (so_file.c_str(), RTLD_LAZY);
    if (dl_handle == NULL) {
        char * err = dlerror () ;
        LOGE ("Failed to load library: %s\n", err);
        OUT ;
        return err ;
    }

    LOGD("dlopen [ok]. Looking for descriptor function ...");
    descriptorFunction = (LADSPA_Descriptor_Function) dlsym (dl_handle, "ladspa_descriptor");
    // count plugins
    if (descriptorFunction == NULL) {
        LOGE("Failed to find descriptor function") ;
    } else {
        LOGD("Descriptor function [ok] Counting plugins ...") ;
        for (total_plugins = 0;; total_plugins++) {
            const LADSPA_Descriptor *d = descriptorFunction(total_plugins);
            if (d == NULL) break;
            LOGD("plugin found [%s:%d] %s", so_file.c_str(),total_plugins, d->Name);
            descriptors.push_back(d);
        }

        LOGI("\t\t... found %d plugins", total_plugins);
    }

    OUT ;
    return NULL ;
}
