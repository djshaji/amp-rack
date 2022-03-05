#include "Plugin.h"

Plugin::Plugin (const LADSPA_Descriptor * _descriptor, unsigned long _sampleRate) {
    if (_descriptor == NULL) {
        LOGF ("[%s:%s] null descriptor passed", __FILE__, __PRETTY_FUNCTION__ );
        return ;
    }

    descriptor = _descriptor ;
    if (_sampleRate > 0)
        sampleRate = _sampleRate ;
    else {
        LOGF ("[%s: %s] 0 sample rate passed", __FILE__, __PRETTY_FUNCTION__ );
        sampleRate = 48000 ;
    }

    handle = (LADSPA_Handle *) descriptor -> instantiate (descriptor, sampleRate);
    if (descriptor->activate) {
        descriptor->activate (handle);
    }

    LOGD("[%s] loaded plugin %s at %u", __PRETTY_FUNCTION__ , descriptor->Name, sampleRate);
    print();

    for (int i = 0 ; i < descriptor->PortCount ; i ++) {
        LADSPA_PortDescriptor port = descriptor -> PortDescriptors [i];
        if (LADSPA_IS_PORT_AUDIO(port)) {
            if (LADSPA_IS_PORT_INPUT(port)) {
                if (inputPort == -1)
                    inputPort = i ;
                else if (inputPort2 == -1)
                    inputPort2 = i ;
                else
                    LOGE("[%s %d]: %s is third input port", descriptor->Name, i, descriptor->PortNames [i]);
            } else if (LADSPA_IS_PORT_OUTPUT(port)) {
                if (outputPort == -1)
                    outputPort = i ;
                else if (outputPort2 == -1)
                    outputPort2 = i ;
                else
                    LOGE("[%s %d]: %s is third output port", descriptor->Name, i, descriptor->PortNames [i]);

            }
        } else if (LADSPA_IS_PORT_OUTPUT(port)) {
            LOGE("[%s:%d] %s: ladspa port is output but not audio!", descriptor->Name, i, descriptor->PortNames[i]);
            // this, erm, doesn't work
            /*
            if (outputPort == -1)
                outputPort = port ;
            */
        } else {
            PluginControl * pluginControl = new PluginControl (descriptor, i) ;
            descriptor->connect_port (handle, i, &pluginControl->val);
            pluginControls.push_back(pluginControl);
        }
    }
}

void Plugin::print () {
    LOGD("--------| Controls for %s |--------------", descriptor->Name) ;
    for (int i = 0 ; i < pluginControls.size() ; i ++) {
        pluginControls.at(i)->print();
    }
}