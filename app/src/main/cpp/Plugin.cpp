#include "Plugin.h"
#include "lv2/atom/atom.h"
#include "lv2/lv2plug.in/ns/ext/atom/forge.h"

using namespace nlohmann ;
void replaceAll(std::string& str, const std::string& from, const std::string& to) {
    if(from.empty())
        return;
    size_t start_pos = 0;
    while((start_pos = str.find(from, start_pos)) != std::string::npos) {
        str.replace(start_pos, from.length(), to);
        start_pos += to.length(); // In case 'to' contains 'from', like replacing 'x' with 'yx'
    }
}

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
        //~ LOGD("Creating plugin: %s", _descriptor->Name);
        lv2_name = std::string  (_descriptor->Name);

        handle = (LADSPA_Handle *) descriptor->instantiate(descriptor, sampleRate);
        ID = descriptor->UniqueID;
        //~ LOGD("[%s] loaded plugin %s [%d: %s] at %u", __PRETTY_FUNCTION__, descriptor->Name,
             //~ descriptor->UniqueID, descriptor->Label, sampleRate);
        //~ print();

        for (int i = 0; i < descriptor->PortCount; i++) {
            LADSPA_PortDescriptor port = descriptor->PortDescriptors[i];
            if (LADSPA_IS_PORT_AUDIO(port)) {
                if (LADSPA_IS_PORT_INPUT(port)) {
                    //~ LOGD("[%s %d]: found input port", descriptor->Name, i);
                    if (inputPort == -1)
                        inputPort = i;
                    else if (inputPort2 == -1)
                        inputPort2 = i;
                    else
                        LOGE("[%s %d]: %s is third input port", descriptor->Name, i,
                             descriptor->PortNames[i]);
                } else if (LADSPA_IS_PORT_OUTPUT(port)) {
                    //~ LOGD("[%s %d]: found output port", descriptor->Name, i);
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
                //~ LOGD("[%s %d]: found control port", descriptor->Name, i);
                PluginControl *pluginControl = new PluginControl(descriptor, i);
                descriptor->connect_port(handle, i, pluginControl->def);
                pluginControls.push_back(pluginControl);
            } else if (LADSPA_IS_PORT_CONTROL(port) && LADSPA_IS_PORT_OUTPUT(port)) {
                //~ LOGD("[%s %d]: found possible monitor port", descriptor->Name, i);
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

        //> WARNING: Moving this here because ports have to be connected first
        //  before activating a plugin
        if (descriptor->activate) {
            descriptor->activate(handle);
        }
    } else /* if (type == SharedLibrary::LV2 ) */ {
        //~ LOGD("[LV2] waiting for shared library pointer ...") ;
        lv2Descriptor = (LV2_Descriptor *) descriptor ;

    }
}

void Plugin::print () {
    //~ LOGD("--------| Controls for %s: %d |--------------", descriptor->Name, descriptor ->PortCount) ;
    for (int i = 0 ; i < pluginControls.size() ; i ++) {
        pluginControls.at(i)->print();
    }
}

void Plugin::load () {
    IN
    lv2FeaturesInit();
    
    HERE
    //~ LOGD("Creating plugin: %s from %s @ %s\n", lv2Descriptor->URI, sharedLibrary->LIBRARY_PATH.c_str(), sharedLibrary->so_file.c_str());
    std::string lib_path = sharedLibrary->LIBRARY_PATH + "/" + sharedLibrary -> so_file + ".lv2/" ;
    LOGD("[LV2] library path: %s\n", lib_path.c_str());

    if (lv2Descriptor == NULL) {
        HERE LOGF("[LV2] lv2Descriptor is NULL, we will probably crash ...!\nplugin: %s\n", sharedLibrary->so_file.c_str());
    } 
    //~ else
        //~ LOGD ("[LV2] descriptor is ok, instantiating handle at %d ...\n", sampleRate);

//    handle = (LADSPA_Handle *) lv2Descriptor->instantiate(lv2Descriptor, sampleRate, lib_path.c_str(), sharedLibrary->featurePointers());
    handle = (LADSPA_Handle *) lv2Descriptor->instantiate(lv2Descriptor, sampleRate, lib_path.c_str(), features.data());
//    handle = (LADSPA_Handle *) lv2Descriptor->instantiate(lv2Descriptor, sampleRate, lib_path.c_str(), sharedLibrary->feature_list);
    if (handle == NULL)
        LOGF("[LV2] plugin handle is NULL, we will probably crash ...!\n") ;
    //~ else
        //~ LOGD("[LV2] Handle instantiated ok! Congratulations\n");

    std::string _uri_ = std::string (lv2Descriptor -> URI);
    replaceAll (_uri_, ":", "_");
    std::string json_ ;
    if (type == SharedLibrary::PluginType::LV2)
#ifdef __ANDROID__
        json_ = getLV2JSON(_uri_.c_str());
#else 
        json_ = getLV2JSON_PC(_uri_.c_str());
#endif
#ifndef __ANDROID__
    else if (type == SharedLibrary::PluginType::LILV)
        json_ = getLV2JSON_PC(_uri_.c_str());
#endif
    //~ LOGD ("parsing json: %s\n", json_.c_str ());
    json j = json::parse(json_);
    lv2_name = j ["-1"]["pluginName"];

    //~ LOGD("[LV2 JSON] %s", std::string (j ["1"]["name"]).c_str());
    for (auto& el : j.items())
    {
        //~ LOGD("[LV2] %s", el.key().c_str());
        //~ LOGD("[LV2] %s -> %s", el.key().c_str(), el.value().dump().c_str());
        if (el.key () == "-1") {
            continue ;
        }

        json jsonPort = json::parse (el.value ().dump ());
        const char * portName = std::string (jsonPort ["name"]).c_str ();
        const char * pluginName = sharedLibrary->so_file.c_str() ;

        LADSPA_PortDescriptor port = jsonPort .find ("index").value();
        if (jsonPort.find ("AudioPort") != jsonPort.end ()) {
            if (jsonPort.find ("InputPort")  != jsonPort.end ()) {
                //~ LOGD("[%s %d]: found input port", portName, port);
                if (inputPort == -1)
                    inputPort = port;
                else if (inputPort2 == -1)
                    inputPort2 = port;
                else
                    LOGE("[%s %d]: %s is third input port", pluginName, port, portName);
            } else if (jsonPort.find ("OutputPort")  != jsonPort.end ()) {
                //~ LOGD("[%s %d]: found output port", pluginName, port);
                if (outputPort == -1)
                    outputPort = port;
                else if (outputPort2 == -1)
                    outputPort2 = port;
                else
                    LOGE("[%s %d]: %s is third output port",
                         pluginName, port, portName);
            }
        } else if (jsonPort.find ("InputPort") != jsonPort.end() && jsonPort.find ("ControlPort") != jsonPort.end()) {
            //~ LOGD("[%s %d]: found control port", pluginName, port);
            int pluginIndex = addPluginControl(lv2Descriptor, jsonPort) - 1;
            lv2Descriptor->connect_port(handle, port, pluginControls.at (pluginIndex) ->def);
        } else if (jsonPort.find ("OutputPort") != jsonPort.end() && jsonPort.find("ControlPort") != jsonPort.end()) {
            LOGD("[%s %d]: found possible monitor port", lv2Descriptor->URI, port);
//            lv2Descriptor->connect_port(handle, port, &dummy_output_control_port);
        } else if (jsonPort.find ("AtomPort") != jsonPort.end() && jsonPort.find ("InputPort") != jsonPort.end()) {
            if (filePort == nullptr)
                filePort = (LV2_Atom_Sequence *) malloc (jsonPort .find("minimumSize").value());

            int pluginIndex = addPluginControl(lv2Descriptor, jsonPort) - 1;
            lv2Descriptor->connect_port(handle, port, filePort);

            LOGD("[%s %d/%d]: found possible atom port", lv2Descriptor->URI, port, pluginIndex);
        } else if (jsonPort.find ("AtomPort") != jsonPort.end() && jsonPort.find ("OutputPort") != jsonPort.end()) {
            if (notifyPort == nullptr) {
                notifyPort = (LV2_Atom_Sequence *) malloc(jsonPort.find("minimumSize").value());
//                notifyPort->atom.type =
            }

            lv2Descriptor->connect_port(handle, port, notifyPort);

            LOGD("[%s %d]: found possible notify port", lv2Descriptor->URI, port);
        } else {
            LOGD("[LV2] Cannot understand port %d of %s: %s", port, pluginName, portName);
        }

//        std::cout << "key: " << el.key() << ", value:" << el.value() << '\n';
    }

    // this is here because activate may use "ports" which are allocated above
    if (lv2Descriptor->activate) {
        lv2Descriptor->activate(handle);
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

    lv2ConnectWorkers();
    OUT
}

std::string Plugin::getLV2JSON (std::string pluginName) {
    IN
    //~ LOGD("[LV2] getting JSON for %s/%s", sharedLibrary->so_file.c_str(), pluginName.c_str());
    JNIEnv *env;
    sharedLibrary -> vm-> GetEnv((void**)&env, JNI_VERSION_1_6);
    if (env == NULL) {
        LOGF("cannot find env!");
    }

    jstring jstr1 = env->NewStringUTF(pluginName.c_str());
    jstring libname = env->NewStringUTF(sharedLibrary->so_file.c_str());

    if (sharedLibrary->mainActivityClassName == "")
        sharedLibrary->mainActivityClassName = std::string ("com/shajikhan/ladspa/amprack/MainActivity");
    jclass clazz = env->FindClass(sharedLibrary->mainActivityClassName.c_str());
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

void Plugin::setBuffer (float * buffer, int read_bytes) {
    IN
    // dangerous non standard stuff
    // dont try this at home
    if (type == SharedLibrary::LV2) {
        //~ LOGD("setting buffer for LV2 plugin") ;
        lv2Descriptor->connect_port(handle, 99, &read_bytes);
        lv2Descriptor->connect_port(handle, 100, buffer);
    } else {
        //~ LOGD("setting buffer for LADSPA plugin");
        descriptor->connect_port(handle, 99, reinterpret_cast<LADSPA_Data *>(&read_bytes));
        descriptor->connect_port(handle, 100, buffer);
    }
    OUT
}

void Plugin::setFileName (std::string filename) {
    IN
    float s = filename.size() ;
    lv2Descriptor->connect_port(handle, 99, (void *) &s);
    lv2Descriptor->connect_port(handle, 100, (void *) filename.c_str());
    loadedFileName = std::string (filename);
    loadedFileType = 1 ;
    OUT
}

void Plugin::lv2FeaturesURID () {
    lv2UridMap.handle = &urid ;
    lv2UridMap.map = reinterpret_cast<LV2_URID (*)(LV2_URID_Map_Handle, const char *)>(lv2_urid_map);
//    lv2UridMap.unmap = reinterpret_cast<LV2_URID (*)(LV2_URID_Map_Handle, const char *)>(lv2_urid_unmap);

    featureURID.URI = strdup (LV2_URID__map);
    featureURID.data = &lv2UridMap ;

    logLog.handle = NULL ;
    logLog.printf = logger_printf ;
    logLog.vprintf = logger_vprintf ;

    featureLog.URI = strdup (LV2_LOG__log) ;
    featureLog.data = &logLog ;

    lv2WorkerSchedule.schedule_work = lv2ScheduleWork ;
    lv2WorkerSchedule.handle = this;
    featureSchedule.URI = strdup (LV2_WORKER__schedule);
    featureSchedule.data = &lv2WorkerSchedule ;

    LV2_Options_Interface optionsInterface ;
    optionsInterface.get = lv2_options_get;
    optionsInterface.set = lv2_options_set;
    featureState.URI = strdup (LV2_OPTIONS__options);
    featureState.data = &optionsInterface;

    features.push_back(&featureURID);
    features.push_back(&featureLog);
    features.push_back(&featureSchedule);
    features.push_back(&featureState);
    features.push_back(nullptr);
}

void Plugin::lv2FeaturesInit () {
    IN
    lv2FeaturesURID();
    OUT
}

void Plugin::lv2ConnectWorkers () {
    if (lv2Descriptor->extension_data == nullptr)
        return;
    lv2WorkerInterface = (LV2_Worker_Interface *) lv2Descriptor->extension_data (LV2_WORKER__interface);
    lv2StateInterface = (LV2_State_Interface *) lv2Descriptor->extension_data (LV2_STATE__interface);
}

LV2_Worker_Status lv2ScheduleWork (LV2_Worker_Schedule_Handle handle, uint32_t size, const void * data) {
    Plugin * plugin = reinterpret_cast<Plugin *>(handle);
    return plugin->lv2WorkerInterface->work (plugin->handle, plugin->lv2WorkerInterface->work_response, plugin->handle, size, data);
}

uint32_t lv2_options_set (LV2_Handle instance, const LV2_Options_Option* options) {
    return 0u;
}

uint32_t lv2_options_get (LV2_Handle instance, LV2_Options_Option* options) {
    return 0u;
}

void Plugin::setFilePortValue (std::string filename) {
    IN
    int size = filename.size();
    char * str = (char * ) malloc (filename.size() + 1);
    strcpy(str, filename.c_str());
    lv2Descriptor->connect_port(handle, 9, &size);
    lv2Descriptor->connect_port(handle, 4, str);
    OUT
}

void Plugin::setFilePortValue1 (std::string filename) {
    IN
    if (filePortIndex == -1) {
        LOGE("set file port requested but no file port available") ;
        return;
    }

    //~ LOGD("[atom sequence] %s", filename.c_str());
#ifdef USE_THIS
    LV2_Atom_Forge       forge ;

    LV2_Atom_Forge_Frame frame;
    LV2_URID_Map map ;
    map.handle = &urid;
    map.map = reinterpret_cast<LV2_URID (*)(LV2_URID_Map_Handle, const char *)>(lv2_urid_map);
    lv2_atom_forge_init(&forge, &map);
    LV2_Atom* set = (LV2_Atom*)lv2_atom_forge_blank(
            &forge, &frame, 1, 9);

    lv2_atom_forge_property_head(&forge, 10, 0);
    lv2_atom_forge_urid(&forge, 6);
    lv2_atom_forge_property_head(&forge, 11, 0);
    lv2_atom_forge_path(&forge, filename.c_str(), filename.size());

    lv2_atom_forge_pop(&forge, &frame);
    lv2_atom_forge_sequence_head(&forge, &frame, 0);

    uint8_t buf [1024];
    lv2_atom_forge_set_buffer(&forge,
                              (uint8_t *) filePort,
                              1024);



    LV2_Atom_Object lv2AtomObject;
    lv2AtomObject.body.otype = 9 ;
#else
    LV2_Atom_Forge       forge ;
    LV2_Atom_Forge_Frame frame;
    uint8_t              buf[1024];
    lv2_atom_forge_set_buffer(&forge, buf, sizeof(buf));

    lv2_atom_forge_object(&forge, &frame, 0, 9);
    lv2_atom_forge_key(&forge, 10);
    lv2_atom_forge_urid(&forge, 6);
    lv2_atom_forge_key(&forge, 11);
    lv2_atom_forge_atom(&forge, filename.size(), 12);
    lv2_atom_forge_write(&forge, filename.c_str(), filename.size());

    const LV2_Atom* atom = lv2_atom_forge_deref(&forge, frame.ref);
    typedef struct {
        uint32_t index;
        uint32_t protocol;
        uint32_t size;
        // Followed immediately by size bytes of data
    } ControlChange;
    typedef struct {
        ControlChange change;
        LV2_Atom      atom;
    } Header;
    const Header header = {
            {0, 5, (uint32_t) (sizeof(LV2_Atom) + filename.size())},
            {uint32_t (filename.size()), 12}};

//    lv2Descriptor->connect_port(handle, filePortIndex, filePort);
    memcpy(filePort, &header, sizeof  (header));
    memcpy(filePort + sizeof (header), &atom, sizeof  (atom));
    __atomic_store_n(&filePort, filePort, __ATOMIC_RELEASE) ;

    lv2Descriptor->connect_port(handle, filePortIndex, filePort);

#endif
    LV2_ATOM_SEQUENCE_FOREACH(filePort, ev) {
        const LV2_Atom_Object *obj = (LV2_Atom_Object *) &ev->body;
        //~ LOGD ("[command] %d", obj->body.otype);
        const LV2_Atom* property = NULL;
        lv2_atom_object_get(obj, 10, &property, 0);
        //~ LOGD ("%s", property);

    }
    OUT
}

#ifndef __ANDROID__
std::string Plugin::getLV2JSON_PC (std::string pluginName) {
    IN
    //~ HERE LOGD ("[%s] plugin: %s\n", sharedLibrary->so_file.c_str (), pluginName.c_str ());
    // todo:
    // file name here, load and return this json file
    // rename lv2 directories !
    std::string stub = std::string (lv2Descriptor->URI) ;
    if (stub.find ("#" ) != -1)
        stub = stub.substr (stub.find ("#") + 1, stub.size ()) ;
    else 
        stub = stub.substr (stub.find_last_of ("/") + 1, stub.size ()) ;
        
    std::string lib = std::string (sharedLibrary->so_file);
    lib = lib.substr (lib.find_last_of ("/") + 1, lib.size ());
    std::string path = sharedLibrary->lv2_config_path ;
    path.append ("/").append (lib).append ("/").append (stub).append (".json");
    std::replace(path.begin(), path.end(), ':', '_');

    LOGD ("[LV2 %s] config for %s: %s\n", stub.c_str (), pluginName.c_str (), path.c_str ());
    std::ifstream fJson(path.c_str ());
    std::stringstream buffer;
    buffer << fJson.rdbuf();
    OUT
    return buffer.str () ;
    
    /*
    auto json = nlohmann::json::parse(buffer.str());
    std::string name (lilv_node_as_string (lilv_plugin_get_name (sharedLibrary->plugin)));

    for (auto plugin : json) {
        std::string s = plugin ["name"].dump();
        std::string ss = s.substr (1, s.size() - 2) ;
        //~ printf ("comparing plugin: %s | %s \n", ss.c_str (), name.c_str ());
        if (ss == name) {
            printf ("found config for plugin: %s\n", pluginName.c_str ());
            
            sharedLibrary->LIBRARY_PATH = plugin["library"].dump ();
            sharedLibrary->LIBRARY_PATH = sharedLibrary->LIBRARY_PATH.substr (1, sharedLibrary->LIBRARY_PATH.size () - 2);
            lv2_name = ss ;
            break ;
        }
    }
    
    std::string stub = std::string (lv2Descriptor->URI);
    //~ printf ("%s > %d\n", lv2Descriptor->URI, stub.find ("#"));
    if (stub.find ("#") == -1) {
        stub = stub.substr (stub.find_last_of ("/") + 1, stub.size () - 1) ;
        printf ("stub: %s\n", stub.c_str ());
    } else {
        stub = stub.substr (stub.find ("#"), stub.size () - 1);
    }
    
    std::string path = std::string ("assets/lv2/").append (sharedLibrary->LIBRARY_PATH).append ("/").append (stub).append (".json").c_str () ;
    printf ("path: %s\n", path.c_str());
    std::ifstream fson(path.c_str ());
    std::stringstream buffer_;
    buffer_ << fson.rdbuf();
    //~ printf ("[plugin] json: %s\n", buffer.str ().c_str ());
    //~ json = nlohmann::json::parse(buffer.str());
    
    OUT
    return buffer_.str ();
    */
}
#endif
