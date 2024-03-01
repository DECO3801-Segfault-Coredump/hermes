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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketServletContainerInitializer;
import org.tinylog.Logger;

/**
 * WebSocket server for frontend
 * Based on: https://github.com/jetty-project/embedded-jetty-websocket-examples/blob/10.0.x/javax.websocket-example/src/main/java/org/eclipse/jetty/demo/EventServer.java
 *
 * @author Cathy Nguyen, Matt Young, Henry Batt
 */
public class FrontendServer {
    private final Server server;
    private final ServerConnector connector;

    public FrontendServer() {
        server = new Server(42069); // WebSocket on port 42069
        connector = new ServerConnector(server);
        server.addConnector(connector);

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // Initialize javax.websocket layer
        JavaxWebSocketServletContainerInitializer.configure(context, (servletContext, wsContainer) ->
        {
            // This lambda will be called at the appropriate place in the
            // ServletContext initialization phase where you can initialize
            // and configure  your websocket container.

            // Configure defaults for container
            wsContainer.setDefaultMaxTextMessageBufferSize(65535);

            // Add WebSocket endpoint to javax.websocket layer
            wsContainer.addEndpoint(FrontendEndpoint.class);
        });
    }

    public void start() {
        try {
            server.start();
            Logger.info("Server started");
        } catch (Exception e) {
            Logger.error("Failed to start server: {}", e);
            Logger.error(e);
        }
    }

    public void stop() {
        try {
            server.stop();
            Logger.info("Server stop");
        } catch (Exception e) {
            Logger.error("Failed to stop server: {}", e);
            Logger.error(e);
        }
    }
}
