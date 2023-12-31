/*
 * Copyright (c) 2023 DECO3801 Team Segmentation fault (core dumped).
 *
 * See the "@author" comment for who retains the copyright on this file.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.decosegfault.atlas.map

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
import com.decosegfault.atlas.render.GraphicsPreset
import com.decosegfault.atlas.util.AtlasUtils
import kotlin.math.pow

/**
 * A square planar tile with recursive sub-tiles, and image decal for texturing.
 *
 * @param x     X-coordinate of tile centre
 * @param z     Z-coordinate of the tile centre
 * @param size  The size of the square tile
 *
 * The basic framework and culling behaviour is based upon AtlasVehicle by Matt Young
 *
 * @author Henry Batt
 */
data class Tile(val x: Float, val z: Float, val size: Float, val tileLookup : Vector3, val parent : Tile? = null) : Disposable {

    /** Bounding box of the tile*/
    var bbox = BoundingBox()

    /** If the tile was culled in the last render pass */
    var didCull = false

    /** If the tile was divided into sub-tiles in the last render pass */
    var didUseSubTiles = false

    /** Decal display of tile */
    private var decal: Decal? = null

    private var connectedSubs = mutableListOf<Tile>()

    private var shouldDraw = true

    /** Breakdown of tile in 2x2 sub-tiles */
    private var subTiles = mutableListOf<Tile>()

    init {
        bbox = BoundingBox(Vector3(x - 64, 0f, z - 64),
            Vector3(x + size + 64, 0f, z + size + 64))
    }

    /**
     * Recursively generate 4 sub-tiles till min size reached
     *
     * @param min_size  Minimum size of sub-tiles to generate.
     */
    private fun generateSubTiles(min_size: Float) {
        // Smallest resolution tile reached. Base Case
        if (size == min_size) {
            return
        }

        val subSize = size / 2

        // Create the 4 sub-tiles
        val q1 = Tile(x, z, subSize,
            Vector3(2 * tileLookup.x, 2 * tileLookup.y, tileLookup.z + 1), this)
        subTiles.add(q1) // Top Left

        val q2 = Tile(  x + subSize, z, subSize,
            Vector3(2 * tileLookup.x + 1, 2 * tileLookup.y, tileLookup.z + 1), this)
        subTiles.add(q2) // Top Right

        val q3 = Tile(x,z + subSize, subSize,
            Vector3(2 * tileLookup.x, 2 * tileLookup.y + 1, tileLookup.z + 1), this)
        subTiles.add(q3) // Bottom Left

        val q4 = Tile(x + subSize, z + subSize, subSize,
            Vector3(2 * tileLookup.x + 1, 2 * tileLookup.y + 1, tileLookup.z + 1), this)
        subTiles.add(q4) // Bottom Right

        q1.addConnectedSubs(subTiles)
        q2.addConnectedSubs(subTiles)
        q3.addConnectedSubs(subTiles)
        q4.addConnectedSubs(subTiles)
    }

    /** Return the decal that represents this tile */
    fun getDecal(): Decal? {
        // We don't need this tile yet, skip for now
        if (!shouldDraw) {
            return null
        }

        // If first time rendering, generate the decal.
        if (decal == null) {
            generateDecal()
            setDrawAllConnected(false)
            return parent?.getDecal()
        }

        return decal
    }

    private fun addConnectedSubs(connectedTiles: MutableList<Tile>) {
        this.connectedSubs = connectedTiles
    }

    private fun setDraw(shouldDraw: Boolean) {
        this.shouldDraw = shouldDraw
    }

    private fun setDrawAllConnected(shouldDraw: Boolean) {
        for (tile in connectedSubs) {
            tile.setDraw(shouldDraw)
        }
    }

    /**
     * Generate the decal by grabbing texture from GCTileCache.
     */
    private fun generateDecal() {
        val shift = size / 2
        GCTileCache.retrieve(tileLookup) {
            decal = Decal.newDecal(size, size, TextureRegion(it))
            decal!!.setPosition(x + shift, 0f, z + shift)
            decal!!.setRotationX(270f)
        }
    }

    private fun updateTexture() {
        GCTileCache.retrieve(tileLookup) {
            decal?.textureRegion?.texture = it
        }
    }

    /**
     * Get all sub-tiles that make up this tile at desired resolution.
     *
     * @param size The desired tile size to get tiles for
     *
     * @return A list of all the sub-tiles at given scale
     */
    fun getTiles(size: Float): MutableList<Tile> {
        val allTiles = mutableListOf<Tile>()

        // Found desired resolution, grab it. Base Case
        if (this.size == size) {
            allTiles.add(this)

        // Not at resolution yet, add sub-tiles tiles at scale
        } else if (this.size > size) {

            if (subTiles.size == 0) {
                generateSubTiles(size)
            }

            for (tile in subTiles) {
                for (subTile in tile.getTiles(size)) {
                    allTiles.add(subTile)
                }
            }
        }
        return allTiles
    }

    /**
     * Get all sub-tiles that make up tile at a given resolution,
     * but apply culling methods to reduce amount to only desired.
     * Increases the viewing distance of tiles based on height.
     *
     * @param height    The height of the camera.
     * @param cam       Scene camera to cull in relation to.
     * @param graphics  Graphics preset to dictate culling amount.
     *
     * @return All the sub-tiles at given scale that haven't been culled
     */
    fun getTilesCulledHeightScaled(height: Float, cam: Camera, graphics: GraphicsPreset): MutableList<Tile> {
        val allTiles = mutableListOf<Tile>()
        didCull = false
        didUseSubTiles = false
        shouldDraw = true

        // Check iff tile can be culled
        val closestPoint = AtlasUtils.bboxClosestPoint(Vector3(cam.position.x, 0f, cam.position.z), bbox)
        val dist = cam.position.dst(closestPoint)
        if (dist >= graphics.tileDrawDist * height || !cam.frustum.boundsInFrustum(bbox)) {
            didCull = true
            decal = null
            subTiles.clear()
            return allTiles
        }

        val distance = 2.0.pow((dist / AtlasUtils.MAX_DIST).toInt()).toFloat()
        val size = AtlasUtils.MIN_SIZE * distance

        // We are at correct resolution, draw this tile. Base Case
        if (this.size <= size) {
            // tell the GCTileCache that we are in use
            generateDecal()
            GCTileCache.markUsed(tileLookup)
            allTiles.add(this)
        }

        // Can get more detailed, check them all and see
        if (this.size > size) {

            // Dynamically create sub-tiles if not saved.
            if (subTiles.size == 0) {
                generateSubTiles(size)
            }

            if (this.size != size * 2) {
                this.decal = null
            } else {
                generateDecal()
                GCTileCache.markUsed(tileLookup)
            }

            // Add sub-tiles sub-tiles
            for (tile in subTiles) {
                didUseSubTiles = true
                for (subTile in tile.getTilesCulledHeightScaled(height, cam, graphics)) {
                    allTiles.add(subTile)
                }
            }
        }
        return allTiles
    }

    /** Clean up tile and all corresponding sub-tiles */
    override fun dispose() {
        for (tile in subTiles) {
            tile.dispose()
        }
        decal = null
        subTiles.clear()
    }

    /**
     * Draw debug boxes around the tiles.
     *
     * @param render    The ShapeRenderer to add boxes to.
     */
    fun debug(render: ShapeRenderer) {
        if (didCull) return
        render.color = if (didUseSubTiles) Color.GREEN else Color.RED
        render.box(bbox.min.x, bbox.min.y, bbox.max.z, bbox.width, bbox.height, bbox.depth)
    }

    fun debugText(batch: SpriteBatch, font: BitmapFont, cam: PerspectiveCamera) {
        val pos = cam.project(bbox.getCenter(Vector3()))
        font.color = Color.BLACK
        font.draw(batch, "${tileLookup.x.toInt()},${tileLookup.y.toInt()},${tileLookup.z.toInt()}", pos.x, pos.y)
    }
}
