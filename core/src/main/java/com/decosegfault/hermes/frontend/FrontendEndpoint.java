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
@ServerEndpoint(value = "/socket", encoders= FrontendDataEncoder.class, decoders=FrontendDataDecoder.class)
public class FrontendEndpoint {
    private static final Set<FrontendEndpoint> endpoints = ConcurrentHashMap.newKeySet();
    public Session session;

    //public static ArrayList<FrontendData> frontendDataList = new ArrayList<>();

//    public static void main(String[] args) {
//        routeID = "1234";
//        routeShortName = "ROUTE_SHORT_NAME";
//        routeLongName = "ROUTE_LONG_NAME";
//        vehicleType = VehicleType.BUS;
//        expectedTime = "2023-09-14T12:00:00Z";
//        actualTime = "2023-09-14T12:15:00Z";
//    }

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
            } catch (IOException| EncodeException e) {
                Logger.warn("Failed to send message to client: {}", message);
                Logger.warn(e);
            }
        }
    }
}
