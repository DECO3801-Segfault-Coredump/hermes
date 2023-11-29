/*
 * Copyright (c) 2023 DECO3801 Team Segmentation fault (core dumped).
 *
 * See the "@author" comment for who retains the copyright on this file.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.decosegfault.atlas;

import com.badlogic.gdx.Game;
import com.decosegfault.atlas.screens.LoadingScreen;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 *
 * @author Matt Young
 */
public class AtlasGame extends Game {
    @Override
    public void create() {
        setScreen(new LoadingScreen(this));
    }
}
