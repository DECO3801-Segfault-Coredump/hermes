package com.decosegfault.hermes;

import org.tinylog.Logger;
import javax.websocket.EncodeException;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Cathy Nguyen, Matt Young
 */
@ServerEndpoint(value = "/server/deco3801")
public class FrontendEndpoint {

    private static final Set<FrontendEndpoint> endpoints = ConcurrentHashMap.newKeySet();
    public Session session;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        endpoints.add(this);
        this.session = session;
    }

    private static void broadcast(String message) {
        for (FrontendEndpoint endpoint : endpoints) {
            try {
                endpoint.session.getBasicRemote().sendObject(message);
            } catch (IOException| EncodeException e) {
                Logger.warn("Failed to send message to client: {}", message);
                Logger.warn(e);
            }
        }
    }
}
