#include "Plugin.h"

void Plugin::free () {
    descriptor->cleanup (handle);
}

Plugin::Plugin (const LADSPA_Descriptor * _descriptor, unsigned long _sampleRate, SharedLibrary::PluginType _type) {
    type = _type;
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

    if (type == SharedLibrary::LADSPA) {
        LOGD("Creating plugin: %s", _descriptor->Name);

        handle = (LADSPA_Handle *) descriptor->instantiate(descriptor, sampleRate);
        if (descriptor->activate) {
            descriptor->activate(handle);
        }

        LOGD("[%s] loaded plugin %s [%d: %s] at %u", __PRETTY_FUNCTION__, descriptor->Name,
             descriptor->UniqueID, descriptor->Label, sampleRate);
        print();

        for (int i = 0; i < descriptor->PortCount; i++) {
            LADSPA_PortDescriptor port = descriptor->PortDescriptors[i];
            if (LADSPA_IS_PORT_AUDIO(port)) {
                if (LADSPA_IS_PORT_INPUT(port)) {
                    LOGD("[%s %d]: found input port", descriptor->Name, i);
                    if (inputPort == -1)
                        inputPort = i;
                    else if (inputPort2 == -1)
                        inputPort2 = i;
                    else
                        LOGE("[%s %d]: %s is third input port", descriptor->Name, i,
                             descriptor->PortNames[i]);
                } else if (LADSPA_IS_PORT_OUTPUT(port)) {
                    LOGD("[%s %d]: found output port", descriptor->Name, i);
                    if (outputPort == -1)
                        outputPort = i;
                    else if (outputPort2 == -1)
                        outputPort2 = i;
                    else
                        LOGE("[%s %d]: %s is third output port", descriptor->Name, i,
                             descriptor->PortNames[i]);

                }
            } else if (/*LADSPA_IS_PORT_OUTPUT(port)*/ false) {
                LOGE("[%s:%d] %s: ladspa port is output but not audio!", descriptor->Name, i,
                     descriptor->PortNames[i]);
                // this, erm, doesn't work
                /*
                if (outputPort == -1)
                    outputPort = port ;
                */
            } else if (LADSPA_IS_PORT_CONTROL(port) && LADSPA_IS_PORT_INPUT(port)) {
                LOGD("[%s %d]: found control port", descriptor->Name, i);
                PluginControl *pluginControl = new PluginControl(descriptor, i);
                descriptor->connect_port(handle, i, pluginControl->def);
                pluginControls.push_back(pluginControl);
            } else if (LADSPA_IS_PORT_CONTROL(port) && LADSPA_IS_PORT_OUTPUT(port)) {
                LOGD("[%s %d]: found possible monitor port", descriptor->Name, i);
                descriptor->connect_port(handle, i, &dummy_output_control_port);
            } else {
                // special case, aaaargh!
                if (descriptor->UniqueID == 2606) {
                    if (i == 2)
                        inputPort = i;
                    if (i == 3)
                        outputPort = i;
                    if (i == 0 || i == 1) {
                        PluginControl *pluginControl = new PluginControl(descriptor, i);
                        descriptor->connect_port(handle, i, pluginControl->def);
                        pluginControls.push_back(pluginControl);

                        if (i == 0) {
                            pluginControl->min = 0;
                            pluginControl->max = 25;
                        } else if (i == 1) {
                            pluginControl->min = -24;
                            pluginControl->max = 24;
                        }
                    }
                } else {
                    LOGE("[%s %d]: unknown port %s for %s (%d)", descriptor->Name, i,
                         descriptor->PortNames[i], descriptor->Label, descriptor->UniqueID);
                    descriptor->connect_port(handle, i, &dummy_output_control_port);
                }
            }
        }
    } else if (type == SharedLibrary::LV2) {
        lv2Descriptor = (LV2_Descriptor *) _descriptor ;
        LOGD("Creating plugin: %s", lv2Descriptor->URI);
        const LV2_Feature *const *features ;
        handle = (LADSPA_Handle *) lv2Descriptor->instantiate(lv2Descriptor, sampleRate, sharedLibrary->LIBRARY_PATH.c_str(), features);
        LOGD("[LV2] Handle instantiated ok! Congratulations");
    }
}

void Plugin::print () {
    LOGD("--------| Controls for %s: %d |--------------", descriptor->Name, descriptor ->PortCount) ;
    for (int i = 0 ; i < pluginControls.size() ; i ++) {
        pluginControls.at(i)->print();
    }
}
