#include "Plugin.h"

Plugin::Plugin (const LADSPA_Descriptor * _descriptor, unsigned long _sampleRate) {
    if (descriptor == NULL) {
        LOGF ("[%s:%s] null descriptor passed", __FILE__, __PRETTY_FUNCTION__ );
        return ;
    }

    descriptor = _descriptor ;
    if (_sampleRate > 0)
        sampleRate = _sampleRate ;
    else {
        LOGF ("[%s:%s] 0 sample rate passed", __FILE__, __PRETTY_FUNCTION__ );
        sampleRate = 48000 ;
    }

    handle = (LADSPA_Handle *) descriptor -> instantiate (descriptor, sampleRate);
    LOGD("[%s] loaded plugin %s", __PRETTY_FUNCTION__ , descriptor->Name);

    for (int i = 0 ; i < descriptor->PortCount ; i ++) {
        LADSPA_PortDescriptor port = descriptor -> PortDescriptors [i];
        if (LADSPA_IS_PORT_AUDIO(port)) {
            if (LADSPA_IS_PORT_INPUT(port)) {
                inputPort = port ;
            } else if (LADSPA_IS_PORT_OUTPUT(port)) {
                outputPort = port ;
            }
        } else if (LADSPA_IS_PORT_OUTPUT(port)) {
            LOGE("[%s:%d] %s: ladspa port is output but not audio!", descriptor->Name, i, descriptor->PortNames[i]);
            outputPort = port ;
        } else {
            PluginControl * pluginControl = new PluginControl (descriptor, i) ;
            descriptor->connect_port (handle, i, &pluginControl->val);
            pluginControls.push_back(pluginControl);
        }
    }
}