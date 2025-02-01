// Copyright 2011-2022 David Robillard <d@drobilla.net>
// SPDX-License-Identifier: ISC

#include "symap.h"

#include <assert.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

/**
  @file symap.c Implementation of Symap, a basic symbol map (string interner).

  This implementation is primitive, but has some desirable qualities: good
  (O(lg(n)) lookup performance for already-mapped symbols, minimal space
  overhead, extremely fast (O(1)) reverse mapping (ID to string), simple code,
  no dependencies.

  The tradeoff is that mapping new symbols may be quite slow.  In other words,
  this implementation is ideal for use cases with a relatively limited set of
  symbols, or where most symbols are mapped early.  It will not fare so well
  with very dynamic sets of symbols.  For that, you're better off with a
  tree-based implementation (and the associated space cost, especially if you
  need reverse mapping).
*/

struct SymapImpl {
  char**    symbols; ///< String array where symbols[i] is the symbol for ID i
  uint32_t* index;   ///< ID array sorted by corresponding string in `symbols`
  uint32_t  size;    ///< Number of symbols (length of both symbols and index)
  uint32_t  pad;     ///< Unused padding
};

Symap*
symap_new(void)
{
  Symap* const map = (Symap*)calloc(1, sizeof(Symap));

  if (map) {
    map->symbols = NULL;
    map->index   = NULL;
    map->size    = 0U;
  }

  return map;
}

void
symap_free(Symap* const map)
{
  if (map) {
    for (uint32_t i = 0U; i < map->size; ++i) {
      free(map->symbols[i]);
    }

    free(map->symbols);
    free(map->index);
    free(map);
  }
}

static char*
symap_strdup(const char* const str)
{
  const size_t len  = strlen(str);
  char* const  copy = (char*)malloc(len + 1);
  memcpy(copy, str, len + 1);
  return copy;
}

/**
   Return the index into map->index (not the ID) corresponding to `sym`,
   or the index where a new entry for `sym` should be inserted.
*/
static uint32_t
symap_search(const Symap* const map, const char* const sym, bool* const exact)
{
  *exact = false;

  if (map->size == 0) {
    return 0; // Empty map, insert at 0
  }

  if (strcmp(map->symbols[map->index[map->size - 1] - 1], sym) < 0) {
    return map->size; // Greater than last element, append
  }

  uint32_t lower = 0;
  uint32_t upper = map->size - 1;
  uint32_t i     = upper;
  int      cmp   = 0;

  while (upper >= lower) {
    i   = lower + ((upper - lower) / 2);
    cmp = strcmp(map->symbols[map->index[i] - 1], sym);

    if (cmp == 0) {
      *exact = true;
      return i;
    }

    if (cmp > 0) {
      if (i == 0) {
        break; // Avoid underflow
      }
      upper = i - 1;
    } else {
      lower = ++i;
    }
  }

  assert(!*exact || strcmp(map->symbols[map->index[i] - 1], sym) > 0);
  return i;
}

uint32_t
symap_try_map(const Symap* const map, const char* const sym)
{
  bool           exact = false;
  const uint32_t index = symap_search(map, sym, &exact);
  if (exact) {
    return map->index[index];
  }

  return 0;
}

uint32_t
symap_map(Symap* const map, const char* sym)
{
  // Search for existing symbol
  bool           exact = false;
  const uint32_t index = symap_search(map, sym, &exact);
  if (exact) {
    assert(!strcmp(map->symbols[map->index[index] - 1], sym));
    LOGD ("[symap] %s -> %d", sym, map->index[index]);
    return map->index[index];
  }

  // Claim a new highest ID
  const uint32_t id = map->size + 1;

  // Grow symbol array
  char** new_symbols = (char**)realloc(map->symbols, id * sizeof(sym));
  if (!new_symbols) {
    LOGD ("[symap] WARNING!!! %s -> %d", sym, 0);
    return 0;
  }

  map->symbols = new_symbols;

  // Grow index array
  uint32_t* new_index = (uint32_t*)realloc(map->index, id * sizeof(index));
  if (!new_index) {
    LOGD ("[symap] WARNING!!! %s -> %d", sym, 0);
    return 0;
  }

  map->index = new_index;

  // Append new symbol to symbols array
  map->size            = id;
  map->symbols[id - 1] = symap_strdup(sym);

  // Insert new index element into sorted index
  map->index = new_index;
  if (index < map->size - 1) {
    memmove(map->index + index + 1,
            map->index + index,
            (map->size - index - 1) * sizeof(uint32_t));
  }

  map->index[index] = id;

  LOGD ("[symap] %s -> %d", sym, id);
  return id;
}

const char*
symap_unmap(const Symap* const map, const uint32_t id)
{
  if (id == 0) {
    return NULL;
  }

  if (id <= map->size) {
    return map->symbols[id - 1];
  }

  return NULL;
}

#ifdef SYMAP_STANDALONE

#  include <stdio.h>

static void
symap_dump(Symap* const map)
{
  fprintf(stderr, "{\n");
  for (uint32_t i = 0; i < map->size; ++i) {
    fprintf(
      stderr, "\t%u = %s\n", map->index[i], map->symbols[map->index[i] - 1]);
  }
  fprintf(stderr, "}\n");
}

static int
symap_test(Symap* const map)
{
#  define N_SYMS 5

  static const char* const syms[N_SYMS] = {
    "hello", "bonjour", "goodbye", "aloha", "salut"};

  for (int i = 0; i < N_SYMS; ++i) {
    if (symap_try_map(map, syms[i])) {
      return fprintf(stderr, "error: Symbol already mapped\n");
    }

    const uint32_t id = symap_map(map, syms[i]);
    if (!id) {
      return fprintf(stderr, "error: Failed to insert ID\n");
    }

    if (!!strcmp(map->symbols[id - 1], syms[i])) {
      return fprintf(stderr, "error: Corrupt symbol table\n");
    }

    if (symap_map(map, syms[i]) != id) {
      return fprintf(stderr, "error: Remapped symbol to a different ID\n");
    }

    symap_dump(map);
  }

  return 0;

#  undef N_SYMS
}

int
main(void)
{
  Symap* const map = symap_new();
  const int    st  = symap_test(map);

  symap_free(map);
  return st;
}

#endif // SYMAP_STANDALONE
