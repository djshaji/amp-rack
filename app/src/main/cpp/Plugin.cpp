#include "Plugin.h"
using namespace nlohmann ;

void Plugin::free () {
    IN
    if (type == SharedLibrary::LADSPA)
        descriptor->cleanup (handle);
    else
        lv2Descriptor->cleanup (handle);
    OUT
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
        LOGD ("[%s: %s] 0 sample rate passed", __FILE__, __PRETTY_FUNCTION__ );
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
        LOGD("[LV2] waiting for shared library pointer ...") ;
        lv2Descriptor = (LV2_Descriptor *) descriptor ;

    }
}

void Plugin::print () {
    LOGD("--------| Controls for %s: %d |--------------", descriptor->Name, descriptor ->PortCount) ;
    for (int i = 0 ; i < pluginControls.size() ; i ++) {
        pluginControls.at(i)->print();
    }
}

void Plugin::load () {
    IN
    LOGD("Creating plugin: %s from %s @ %s", lv2Descriptor->URI, sharedLibrary->LIBRARY_PATH.c_str(), sharedLibrary->so_file.c_str());
    std::string lib_path = sharedLibrary->LIBRARY_PATH + "/" + sharedLibrary -> so_file + ".lv2/" ;
    LOGD("[LV2] library path: %s", lib_path.c_str());

    if (lv2Descriptor == NULL) {
        HERE LOGF("[LV2] lv2Descriptor is NULL, we will probably crash ...!\nplugin: %s", sharedLibrary->so_file.c_str());
    } else
        LOGD ("[LV2] descriptor is ok, instantiating handle ...");

    handle = (LADSPA_Handle *) lv2Descriptor->instantiate(lv2Descriptor, sampleRate, lib_path.c_str(), sharedLibrary->featurePointers());
//    handle = (LADSPA_Handle *) lv2Descriptor->instantiate(lv2Descriptor, sampleRate, lib_path.c_str(), sharedLibrary->feature_list);
    if (handle == NULL)
        LOGF("[LV2] plugin handle is NULL, we will probably crash ...!") ;
    else
        LOGD("[LV2] Handle instantiated ok! Congratulations");

    if (lv2Descriptor->activate) {
        lv2Descriptor->activate(handle);
    }

    std::string json_ = getLV2JSON(lv2Descriptor -> URI);
    json j = json::parse(json_);
    lv2_name = j ["-1"]["pluginName"];

    LOGD("[LV2 JSON] %s", std::string (j ["1"]["name"]).c_str());
    for (auto& el : j.items())
    {
        LOGD("[LV2] %s", el.key().c_str());
        LOGD("[LV2] %s -> %s", el.key().c_str(), el.value().dump().c_str());
        if (el.key () == "-1") {
            continue ;
        }

        json jsonPort = json::parse (el.value ().dump ());
        const char * portName = std::string (jsonPort ["name"]).c_str ();
        const char * pluginName = sharedLibrary->so_file.c_str() ;

        LADSPA_PortDescriptor port = jsonPort .find ("index").value();
        if (jsonPort.find ("AudioPort") != jsonPort.end ()) {
            if (jsonPort.find ("InputPort")  != jsonPort.end ()) {
                LOGD("[%s %d]: found input port", portName, port);
                if (inputPort == -1)
                    inputPort = port;
                else if (inputPort2 == -1)
                    inputPort2 = port;
                else
                    LOGE("[%s %d]: %s is third input port", pluginName, port, portName);
            } else if (jsonPort.find ("OutputPort")  != jsonPort.end ()) {
                LOGD("[%s %d]: found output port", pluginName, port);
                if (outputPort == -1)
                    outputPort = port;
                else if (outputPort2 == -1)
                    outputPort2 = port;
                else
                    LOGE("[%s %d]: %s is third output port",
                         pluginName, port, portName);
            }
        } else if (jsonPort.find ("InputPort") != jsonPort.end() && jsonPort.find ("ControlPort") != jsonPort.end()) {
            LOGD("[%s %d]: found control port", pluginName, port);
            int pluginIndex = addPluginControl(lv2Descriptor, jsonPort) - 1;
            lv2Descriptor->connect_port(handle, port, pluginControls.at (pluginIndex) ->def);
        } else if (jsonPort.find ("OutputPort") != jsonPort.end() && jsonPort.find("ControlPort") != jsonPort.end()) {
            LOGD("[%s %d]: found possible monitor port", lv2Descriptor->URI, port);
//            lv2Descriptor->connect_port(handle, port, &dummy_output_control_port);
        } else {
            LOGD("[LV2] Cannot understand port %d of %s: %s", port, pluginName, portName);
        }

//        std::cout << "key: " << el.key() << ", value:" << el.value() << '\n';
    }

    /*
    recursive_iterate(*this, j, [*this, &j](Plugin *plugin, json::const_iterator it){
        json jsonPort = (json) it.value() ;
        const char * portName = std::string (jsonPort ["name"]).c_str ();
        const char * pluginName = plugin->sharedLibrary->so_file.c_str() ;

        LADSPA_PortDescriptor port = jsonPort ["index"];
        if (jsonPort.find ("AudioPort") != jsonPort.end ()) {
            if (jsonPort.find ("InputPort")  != jsonPort.end ()) {
                LOGD("[%s %d]: found input port", portName, port);
                if (plugin->inputPort == -1)
                    plugin->inputPort = port;
                else if (plugin->inputPort2 == -1)
                    plugin->inputPort2 = port;
                else
                    LOGE("[%s %d]: %s is third input port", pluginName, port, portName);
            } else if (jsonPort.find ("OutputPort")  != jsonPort.end ()) {
                LOGD("[%s %d]: found output port", pluginName, port);
                if (plugin->outputPort == -1)
                    plugin->outputPort = port;
                else if (plugin ->outputPort2 == -1)
                    plugin->outputPort2 = port;
                else
                    LOGE("[%s %d]: %s is third output port",
                         pluginName, port, portName);
            }
        } else if (jsonPort.find ("InputPort") != jsonPort.end() && jsonPort.find ("ControlPort") != jsonPort.end()) {
            LOGD("[%s %d]: found control port", pluginName, port);
            int pluginIndex = plugin -> addPluginControl(plugin->lv2Descriptor, j);
            lv2Descriptor->connect_port(handle, port, plugin -> pluginControls.at (pluginIndex) ->def);
        } else if (jsonPort.find ("OutputPort") != jsonPort.end() && jsonPort.find("ControlPort") != jsonPort.end()) {
            LOGD("[%s %d]: found possible monitor port", lv2Descriptor->URI, port);
//            lv2Descriptor->connect_port(handle, port, &dummy_output_control_port);
        } else {
            LOGD("[LV2] Cannot understand port %d of %s: %s", port, pluginName, portName);
        }
    });
     */

    OUT
}

std::string Plugin::getLV2JSON (std::string pluginName) {
    IN
    LOGD("[LV2] getting JSON for %s/%s", sharedLibrary->so_file.c_str(), pluginName.c_str());
    JNIEnv *env;
    sharedLibrary -> vm-> GetEnv((void**)&env, JNI_VERSION_1_6);
    if (env == NULL) {
        LOGF("cannot find env!");
    }

    jstring jstr1 = env->NewStringUTF(pluginName.c_str());
    jstring libname = env->NewStringUTF(sharedLibrary->so_file.c_str());

    jclass clazz = env->FindClass("com/shajikhan/ladspa/amprack/MainActivity");
    if (clazz == nullptr) {
        HERE LOGF("cannot find class!");
    }

    jmethodID mid = env->GetStaticMethodID(clazz, "getLV2Info",
                                           "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    if (mid == nullptr) {
        LOGF("cannot find method!");
    }

    jobject obj = env->CallStaticObjectMethod(clazz, mid, libname, jstr1);
    if (obj == nullptr) {
        LOGF("cannot find class!");
    }

    jstring retStr = (jstring)obj;
    const char *nativeString = env->GetStringUTFChars(retStr, 0);
    std::string str = std::string (nativeString);
    env->ReleaseStringUTFChars(retStr, nativeString);

    OUT
    return str;
}


int Plugin::addPluginControl (const LV2_Descriptor * _descriptor, nlohmann::json _j) {
    IN ;
    PluginControl * pluginControl = new PluginControl(_descriptor, _j);
    pluginControls.push_back(pluginControl);
    OUT ;
    return pluginControls.size();
}
