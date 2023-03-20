//
// Created by djshaji on 12/20/22.
//

#ifndef AMP_RACK_PLUGIN2_H
#define AMP_RACK_PLUGIN2_H
#include "lv2.h"
#include "Plugin.h"

class Plugin2: public Plugin {
    LV2_Descriptor * descriptor;
    LV2_Handle * handle ;
};

#endif //AMP_RACK_PLUGIN2_H
