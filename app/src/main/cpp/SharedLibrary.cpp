#include "SharedLibrary.h"

void SharedLibrary::setSampleRate (unsigned long _sampleRate) {
    sampleRate = _sampleRate ;
}

SharedLibrary::SharedLibrary (char * plugin_file, PluginType pluginType) {
    so_file = std::string (plugin_file) ;
    type = pluginType ;
}

void SharedLibrary::unload () {
    dlclose (dl_handle);
}

//> Returns NULL if ok, error otherwise
char * SharedLibrary::load () {
    IN ;
    dl_handle = dlopen (so_file.c_str(), RTLD_LAZY);
    if (dl_handle == NULL) {
        char * err = dlerror () ;
        LOGE ("Failed to load library: %s\n", err);
        OUT ;
        return err ;
    }

    LOGD("dlopen [ok]. Looking for descriptor function ...");
    if (type == LADSPA) {
        descriptorFunction = (LADSPA_Descriptor_Function) dlsym(dl_handle, "ladspa_descriptor");
        // count plugins
        if (descriptorFunction == NULL) {
            LOGE("Failed to find descriptor function");
        } else {
            LOGD("Descriptor function [ok] Counting plugins ...");
            for (total_plugins = 0;; total_plugins++) {
                const LADSPA_Descriptor *d = descriptorFunction(total_plugins);
                if (d == NULL) break;
                LOGD("plugin found [%s:%d] %s (%s %d)", so_file.c_str(), total_plugins, d->Name,
                     d->Label, d->UniqueID);
                descriptors.push_back(d);
            }

            LOGI("\t\t... found %d plugins", total_plugins);
        }
    } else if (type == LV2 ){
        LOGI("[LV2] Loading plugin library %s", so_file.c_str());
        lv2DescriptorFunction = (LV2_Descriptor_Function) dlsym(dl_handle, "lv2_descriptor");
        if (lv2DescriptorFunction == NULL) {
            LOGE("[LV2]: Failed to find descriptor function");
        } else {
            LOGD("[LV2] Descriptor function [ok] Counting plugins ...");
            for (total_plugins = 0;; total_plugins++) {
                const LV2_Descriptor * d = lv2DescriptorFunction (total_plugins);
                if (d == NULL) break;
                LOGD("[LV2] plugin found [%s:%d] %s", so_file.c_str(), total_plugins, d->URI);
//                lv2_descriptors.push_back(d);
                descriptors.push_back(reinterpret_cast<const _LADSPA_Descriptor *const>(d));
            }

            LOGI("\t\t... found %d LV2 plugins", total_plugins);
        }
    }

    OUT ;
    return NULL ;
}

bool SharedLibrary::plugin_is_valid (const LADSPA_Descriptor * descriptor)
{
    unsigned long i;
    unsigned long icount = 0;
    unsigned long ocount = 0;

    for (i = 0; i < descriptor->PortCount; i++)
    {
        if (!LADSPA_IS_PORT_AUDIO (descriptor->PortDescriptors[i]))
            continue;

        if (LADSPA_IS_PORT_INPUT (descriptor->PortDescriptors[i]))
            icount++;
        else
            ocount++;
    }

    if (icount == 0 || ocount == 0)
        return false;

    return true;
}

