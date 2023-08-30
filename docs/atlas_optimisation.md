# Atlas optimisation ideas
- Matt Young

## Introduction
This document contains optimisation ideas for the Atlas renderer.

## Currently implemented
- Distance culling
- Frustum culling using model AABBs
- Two tier (low poly, high poly) LoD system

## Model caching in `AtlasVehicle`
- Putting **all** renderables between `cache.begin()` & `cache.end()` in `AtlasSceneManager` is **too slow**
- Each `AtlasVehicle` should maintain two caches for the high and low LoDs which are baked on creation
- Make sure we use `TightMeshPool`
- In `AtlasVehicle#getRenderable`, we return the instance of the cache instead of the model

## Ground plane optimisation
- Problem: Each tile may either be a mesh or a model (probably model?)
- Solution: Ground is batched into a `ModelCache`, this cache is only invalidated if the visibility changes
  - HUGE: We _should_ be able to update the `AtlasVehicle` and ground plane model cache asynchronously!!
  - Beware of threading overhead
- Consider drawing _beyond_ camera bounds to reduce frequency of ModelCache updates

## Quadtree model caching
- Imagine we apply `ModelCache` to the entire scene
- If a vehicle moves, we have to invalidate the cache
- Worst case scenario: only _one_ vehicle moves, we have to invalidate the entire cache and rebuild
it regardless
- **Idea:** Divide cache into a quadtree, only invalidate and rebuild regions in which the vehicle
moved
- Quadtree depth based on tradeoff between reducing draw calls (shallower quadtree) and reducing
cache invalidations (deeper quadtree)
- Quadtree subdivision could be based on vehicle velocity (likelihood to move)
