#ifndef SHARED_LIBRARY_H
#define SHARED_LIBRARY_H

#include <dlfcn.h>
#include "logging_macros.h"
#include <cstring>
#include <string>
#include <set>
#include "string.h"
#include <vector>
#include "ladspa.h"
#include "lv2.h"

#include "lv2/worker/worker.h"
#include "lv2/state/state.h"
#include "lv2/log/log.h"
#include "lv2/options/options.h"
#include "lv2/ui/ui.h"
#include "lv2/data-access/data-access.h"
#include <jni.h>
#include <map>
#include "lilv/lilv.h"

typedef struct {
    LV2_Feature                map_feature;
    LV2_Feature                unmap_feature;
    LV2_State_Make_Path        make_path;
    LV2_Feature                make_path_feature;
    LV2_Worker_Schedule        sched;
    LV2_Feature                sched_feature;
    LV2_Worker_Schedule        ssched;
    LV2_Feature                state_sched_feature;
    LV2_Log_Log                llog;
    LV2_Feature                log_feature;
    LV2_Options_Option         options[7];
    LV2_Feature                options_feature;
    LV2_Feature                safe_restore_feature;
    LV2UI_Request_Value        request_value;
    LV2_Feature                request_value_feature;
    LV2_Extension_Data_Feature ext_data;
} LV2Features;

struct CmpStr
{
    bool operator()(const char *a, const char *b) const
    {
        return std::strcmp(a, b) < 0;
    }
};


class SharedLibrary {
public:
    typedef enum {
        LADSPA,
        LV2,
        LILV
    } PluginType ;

    std::string mainActivityClassName ;

    LilvPlugin* plugin = nullptr ;
    LilvInstance* instance = nullptr;
    std::string lv2_config_path ;
    //! feature storage
    std::vector<LV2_Feature> m_features;
    //! pointers to m_features, required for lilv_plugin_instantiate
    std::vector<const LV2_Feature*> m_featurePointers;
    //! features + data, ordered by URI
    std::map<const char*, void*, CmpStr> m_featureByUri;
    std::set<const char*, CmpStr> m_supportedFeatureURIs;

    const LV2_Feature* const* featurePointers() const
    {
        return m_featurePointers.data();
    }

    std::string LIBRARY_PATH ;
    JavaVM * vm ;
    SharedLibrary(char * plugin_file, PluginType type = LADSPA);
    PluginType type = LADSPA ; // by default
    std::string so_file ;
    std::vector<const LADSPA_Descriptor *> descriptors;
    std::vector<const LV2_Descriptor *> lv2_descriptors;

    LV2Features  features ;
    const LV2_Feature** feature_list;
    int total_plugins = 0 ;
    void * dl_handle = NULL;
    unsigned long sampleRate ;
    LADSPA_Descriptor_Function descriptorFunction ;
    LV2_Descriptor_Function lv2DescriptorFunction ;

    void setSampleRate(unsigned long _sampleRate);
    char *load();

    bool plugin_is_valid(const LADSPA_Descriptor *descriptor);

    void unload();

    void setLibraryPath(std::string path);
};

#endif // SHARED_LIBRARY_H
