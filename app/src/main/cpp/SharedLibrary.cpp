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
    if (type != LILV) {
        dl_handle = dlopen (so_file.c_str(), RTLD_LAZY);
        if (dl_handle == NULL) {
            char * err = dlerror () ;
            LOGE ("Failed to load library %s: %s\n", so_file.c_str(), err);
            OUT ;
            return err ;
        }
        
        LOGD("dlopen [ok]. Looking for descriptor function ...");
    }

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
    } else if (type == LV2 || type == LILV ){
        LOGI("[LV2] Loading plugin library %s", so_file.c_str());
        feature_list = (const LV2_Feature**)calloc(1, sizeof(features));
        LOGD("[LV2] %s calloc [ok], trying memcpy ...", so_file.c_str());
        memcpy(feature_list, &features, sizeof(features));
        LOGD("[LV2] memcpy ok ...") ;

        // *new* LV2 feature stuff
        m_supportedFeatureURIs.insert(LV2_URID__map);
        m_supportedFeatureURIs.insert(LV2_URID__unmap);
        m_supportedFeatureURIs.insert(LV2_OPTIONS__options);
        for(std::pair<const char* const, void*>& pr : m_featureByUri)
        {
            m_features.push_back(LV2_Feature { pr.first, pr.second });
        }

        m_featurePointers.reserve(m_features.size() + 1);
        for (const auto& feature : m_features)
        {
            m_featurePointers.push_back(&feature);
        }

        m_featurePointers.push_back(nullptr);

//        LOGD("[LV2] %s", features.log_feature.URI);

        /* the following does not work.... _for some reason_
        for (const LV2_Feature* const* f = feature_list; *f; ++f) {
            LOGD("[LV2] discovered feature %s", (*f)->URI);
        }
         */

//        if (feature_list == NULL) {
//            LOGF("[LV2] Fatal error: feature list for %s is NULL, we will probably crash ...!", so_file.c_str());
//        }

//        LV2_Feature *lv2Feature = (LV2_Feature *) feature_list;
//        while (lv2Feature != NULL) {
//            LOGD("[LV2] discovered feature %s", lv2Feature ->URI);
//            lv2Feature ++;
//        }


        if (type == LILV) {
# ifdef __linux__
#ifndef __ANDROID__
            LilvWorld* world = (LilvWorld* )lilv_world_new();
            lilv_world_load_all(world);
            
            LOGD ("[lilv] world loaded [ok]\n");
            LilvPlugins* plugins = (LilvPlugins* )lilv_world_get_all_plugins(world);

            LOGD ("[lilv] get all plugins [ok], loading %s\n", so_file.c_str ());
            
            LilvNode* plugin_uri = lilv_new_uri(world, so_file.c_str ());
            plugin = (LilvPlugin *)lilv_plugins_get_by_uri(plugins, plugin_uri);
            
            LOGD ("[lilv] plugin loaded [ok]\n");
            const char * name = lilv_node_as_string (lilv_plugin_get_name (plugin));
            printf("[LV2] %s\n", name);

            // todo: get sample rate automatically
            LOGD ("got plugin\n");
            instance = lilv_plugin_instantiate(plugin, 48000.0, NULL);
            LOGD ("got instance\n");
            descriptors.push_back(reinterpret_cast<const _LADSPA_Descriptor *const>(instance->lv2_descriptor));
            
            const LilvNode* const bundle_uri = lilv_plugin_get_bundle_uri(plugin);
            LIBRARY_PATH = std::string (lilv_node_as_string (bundle_uri)) ;
            so_file = std::string ("");
#endif
#endif
            return NULL ;
        } else {
            lv2DescriptorFunction = (LV2_Descriptor_Function) dlsym(dl_handle, "lv2_descriptor");
        }
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

void SharedLibrary::setLibraryPath (std::string path) {
    HERE LOGD("setting library path to %s", path.c_str());
    LIBRARY_PATH = std::string (path) ;
}
