/*
 * Copyright (c) 2023 DECO3801 Team Segmentation fault (core dumped).
 *
 * See the "@author" comment for who retains the copyright on this file.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.decosegfault.hermes.frontend;

import org.tinylog.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Cathy Nguyen, Matt Young
 * Reference: baeldung.com/java-websockets
 */
@ServerEndpoint(value="/socket", encoders=FrontendDataEncoder.class, decoders=FrontendDataDecoder.class)
public class FrontendEndpoint {
    private static final Set<FrontendEndpoint> endpoints = ConcurrentHashMap.newKeySet();
    public Session session;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        endpoints.add(this);
        this.session = session;
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        endpoints.remove(this);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.printf("Error encountered: %s", session.getId());
        Logger.warn(throwable);
    }

    public static void broadcast(FrontendData message) {
        for (FrontendEndpoint endpoint : endpoints) {
            try {
                endpoint.session.getBasicRemote().sendObject(message);
                Logger.debug("Sent data {} to client {}", message, endpoint.session.getId());
            } catch (IOException | EncodeException e) {
                Logger.warn("Failed to send message to client: {}", message);
                Logger.warn(e);
            }
        }
    }
}
