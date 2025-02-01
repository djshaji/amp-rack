//
// Created by djshaji on 1/29/25.
//

#include "atom.h"

void AmpAtom::release () {
    if (! buffer)
        return;

    free (buffer);
    buffer = nullptr;
}

void AmpAtom::mapURIs() {
    // Map all required URIs
    IN
    if (urid_map == nullptr) {
        LOGE ("urid_map is null! owowow");
        abort();
    }

    uris.atom_Sequence = urid_map->map(urid_map->handle, LV2_ATOM__Sequence);
    LOGD ("[uri atom sequence]: %s -> %d", LV2_ATOM__Sequence, uris.atom_Sequence);
    uris.atom_String = urid_map->map(urid_map->handle, LV2_ATOM__String);
    uris.atom_Path = urid_map->map(urid_map->handle, LV2_ATOM__Path);
    uris.atom_URID = urid_map->map(urid_map->handle, LV2_ATOM__URID);
    uris.atom_eventTransfer = urid_map->map(urid_map->handle, LV2_ATOM__eventTransfer);
    uris.patch_Set = urid_map->map(urid_map->handle, LV2_PATCH__Set);
    uris.atom_Object = urid_map->map(urid_map->handle, LV2_ATOM__Object);
    uris.patch_property = urid_map->map(urid_map->handle, LV2_PATCH__property);
//    uris.patch_property = urid_map->map(urid_map->handle, "urn:brummer:ratatouille#Neural_Model");
    uris.patch_value = urid_map->map(urid_map->handle, LV2_PATCH__value);
    uris.filename_URI = urid_map->map(urid_map->handle,
                                      "http://lv2plugin.com/ns/filename");
    OUT
}

LV2_Atom* AmpAtom::createFilenameMessage(const char* filename) {
    IN
    // Set up forge buffer
    lv2_atom_forge_set_buffer(&forge, buffer, sizeof(buffer));

    // Start a sequence
    LV2_Atom_Forge_Frame seq_frame;
    lv2_atom_forge_sequence_head(&forge, &seq_frame, 0);

    // Start a new frame for the Set message
    LV2_Atom_Forge_Frame frame;
    lv2_atom_forge_frame_time(&forge, 0);
    LOGD("[host forge] type set to %d (patch_Set)", uris.patch_Set);
    lv2_atom_forge_object(&forge, &frame, 0, uris.patch_Set);
//    lv2_atom_forge_object(&forge, &frame, 0, LV2_ATOM_OB);

    // Add property URI
    LOGD("[host forge] key set to %d (patch_property)", uris.patch_property);
    LOGD("[host forge] urid set to %d (filename_URI)", uris.filename_URI);
    lv2_atom_forge_key(&forge, uris.patch_property);
    lv2_atom_forge_urid(&forge, uris.filename_URI);

    // Add the filename value
    LOGD("[host forge] patch value set to %d (patch_value)", uris.patch_value);
    lv2_atom_forge_key(&forge, uris.patch_value);
    lv2_atom_forge_path(&forge, filename, strlen(filename));

    // Close frames
    lv2_atom_forge_pop(&forge, &frame);
    lv2_atom_forge_pop(&forge, &seq_frame);

    OUT
    return (LV2_Atom*)buffer;
}

void AmpAtom::sendFilenameToPlugin(LV2_Atom_Sequence* output_port, const char* filename) {
    IN
    if (!output_port || !filename) {
        throw std::runtime_error("Invalid parameters");
    }

    // Create the message
    LV2_Atom* msg = createFilenameMessage(filename);

    // Copy message to output port
    const uint32_t space = output_port->atom.size;
    if (msg->size > space) {
        throw std::runtime_error("Output buffer too small");
    }

    memcpy(output_port, msg, msg->size + sizeof(LV2_Atom));
    OUT
}

AmpAtom::AmpAtom(LV2_URID_Map* map, int _size) {
        IN
        if (!map) {
            throw std::runtime_error("URID map not provided");
        }

        urid_map = map ;

        // Initialize URIs
        mapURIs();

        // Initialize forge
        lv2_atom_forge_init(&forge, urid_map);
        buffer = static_cast<uint8_t *>(malloc(_size));
        OUT
}

void atom_send_message () {
    // Assuming we have these from the host
    LV2_URID_Map* map = nullptr;  // Should be provided by host
    LV2_Atom_Sequence* output_port = nullptr;  // Should be provided by host

    try {
        // Create host instance
        AmpAtom host(map, 1024);

        // Send a filename to the plugin
        const char* filename = "/path/to/your/file.wav";
        host.sendFilenameToPlugin(output_port, filename);

    } catch (const std::exception& e) {
        fprintf(stderr, "Error: %s\n", e.what());
    }
}

void AmpAtom::send_filename_to_plugin(LV2_URID_Map* map,
                             const char* filename,
                             uint8_t * atom_port_buffer,
                             size_t atom_port_buffer_size) {
    // Initialize URIDs
    HostURIs urids;

    // Initialize atom forge
    LV2_Atom_Forge forge;
    lv2_atom_forge_init(&forge, map);

    // Set up forge to write to atom port buffer
    LV2_Atom_Forge_Frame frame;
    lv2_atom_forge_set_buffer(&forge, atom_port_buffer, atom_port_buffer_size);

    // Start sequence
    lv2_atom_forge_sequence_head(&forge, &frame, 0);

    // Start a patch message
    LV2_Atom_Forge_Frame object_frame;
    lv2_atom_forge_frame_time(&forge, 0);
    lv2_atom_forge_object(&forge, &object_frame, 0, urids.patch_Set);

    // Add property
    lv2_atom_forge_key(&forge, urids.patch_property);
    lv2_atom_forge_urid(&forge, urids.atom_Path);

    // Add value (filename)
    lv2_atom_forge_key(&forge, urids.patch_value);
    lv2_atom_forge_path(&forge, filename, strlen(filename));

    // Close frames
    lv2_atom_forge_pop(&forge, &object_frame);
    lv2_atom_forge_pop(&forge, &frame);
}

void AmpAtom::setControl (LV2_Atom_Sequence * control, char * path) {
    IN
    LV2_Atom_Forge_Frame frame;
    lv2_atom_forge_set_buffer(&forge, buffer, strlen(path));

    lv2_atom_forge_object(&forge, &frame, 0, uris.patch_Set);
    lv2_atom_forge_key(&forge, uris.patch_property);
    lv2_atom_forge_urid(&forge, uris.patch_Set); // i think irfile1 etc. might come here
    lv2_atom_forge_key(&forge, uris.patch_value);
    lv2_atom_forge_atom(&forge, strlen(path), forge.Path);
    lv2_atom_forge_write(&forge, path, strlen(path));

    const LV2_Atom* atom = lv2_atom_forge_deref(&forge, frame.ref);
    HERE
    memcpy (control, atom, strlen(path) + sizeof (path));
//    write_event(
//                        control,
//                        lv2_atom_total_size(atom),
//                        uris.atom_eventTransfer,
//                        atom);
    OUT
}


int
AmpAtom::write_event(
                 const uint32_t    port_index,
                 const uint32_t    size,
                 const LV2_URID    type,
                 const void* const body)
{
    // TODO: Be more discriminate about what to send
    IN
    typedef struct {
        ControlChange change;
        LV2_Atom      atom;
    } Header;

    const Header header = {
            {port_index, uris.atom_eventTransfer, sizeof(LV2_Atom) + size},
            {size, type}};

    OUT
    return write_control_change(
            &header, sizeof(header), body, size);
}

int
AmpAtom::write_control_change(
                          const void* const header,
                          const uint32_t    header_size,
                          const void* const body,
                          const uint32_t    body_size)
{
    IN
    ZixRingTransaction tx = zix_ring_begin_write(ring);
    if (zix_ring_amend_write(ring, &tx, header, header_size) ||
        zix_ring_amend_write(ring, &tx, body, body_size)) {
        LOGD ("[atom transfer] send filename failed!");
        OUT
        return -1;
    }

    zix_ring_commit_write(ring, &tx);
    OUT
    return 0;
}

void AmpAtom::son_of_a (LV2_Atom_Sequence * control, const char * filename) {
    IN
// Initialize the Atom Forge
    LV2_Atom_Forge forge;
    lv2_atom_forge_init(&forge, urid_map);
// Set up forge buffer
    LV2_Atom_Forge_Frame frame;
//    uint8_t buffer[1024]; // Example buffer size
    lv2_atom_forge_set_buffer(&forge, buffer, sizeof(buffer));

// Start sequence
    lv2_atom_forge_sequence_head(&forge, &frame, 0);

    // Example: Write a filename
    lv2_atom_forge_frame_time(&forge, 0); // Event time in frames
    lv2_atom_forge_path(&forge, filename, strlen(filename));

// Close sequence
    lv2_atom_forge_pop(&forge, &frame);

// Example: Connect the Atom Sequence to the plugin's Atom port
    memcpy(control, buffer, sizeof(buffer));
    OUT
}