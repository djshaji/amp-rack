//
// Created by djshaji on 1/29/25.
//

#ifndef AMP_RACK_ATOM_H
#define AMP_RACK_ATOM_H


#include "lv2/lv2plug.in/ns/ext/patch/patch.h"
#include <lv2/atom/atom.h>
#include <lv2/atom/util.h>
#include <lv2/atom/forge.h>
#include <lv2/core/lv2.h>
#include <lv2/urid/urid.h>
#include <lv2/atom/forge.h>
#include <cstring>
#include <stdexcept>

// Define custom URIs for our host
struct HostURIs {
    LV2_URID atom_Sequence;
    LV2_URID atom_String;
    LV2_URID atom_Path;
    LV2_URID atom_URID;
    LV2_URID atom_eventTransfer;
    LV2_URID patch_Set;
    LV2_URID patch_property;
    LV2_URID patch_value;
    LV2_URID filename_URI;
};

class AmpAtom {
private:
    HostURIs uris;
    LV2_URID_Map* urid_map;
    LV2_Atom_Forge forge;

    // Buffer for atom sequence
    uint8_t * buffer;

public:
    AmpAtom(LV2_URID_Map *map, int _size);

    void sendFilenameToPlugin(LV2_Atom_Sequence *output_port, const char *filename);

    LV2_Atom *createFilenameMessage(const char *filename);

    void mapURIs();

    void release();
};


#endif //AMP_RACK_ATOM_H
