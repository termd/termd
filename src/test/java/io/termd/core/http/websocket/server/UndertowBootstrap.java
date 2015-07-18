/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.http.websocket.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.termd.core.http.websocket.Configurations;
import io.termd.core.pty.PtyStatusEvent;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class UndertowBootstrap {

  Logger log = LoggerFactory.getLogger(UndertowBootstrap.class);

  final String host;
  final int port;
  private final Executor executor = Executors.newFixedThreadPool(1);
  private Undertow server;
  private TermServer termServer;

  public UndertowBootstrap(String host, int port, TermServer termServer) {
    this.host = host;
    this.port = port;
    this.termServer = termServer;
  }

  public void bootstrap(final Consumer<Boolean> completionHandler) {
    String servletPath = "/servlet";
    String socketPath = "/socket";
    String httpPath = "/";

    server = Undertow.builder()
        .addHttpListener(port, host)
        .setHandler(exchange -> UndertowBootstrap.this.handleWebSocketRequests(exchange))
        .build();

    server.start();

    completionHandler.accept(true);
  }

  private void handleWebSocketRequests(HttpServerExchange exchange) throws Exception {
    String requestPath = exchange.getRequestPath();

    if (requestPath.startsWith(Configurations.TERM_PATH)) {
      String invokerContext = requestPath.replace(Configurations.TERM_PATH + "/", "");
      getWebSocketHandler(invokerContext).handleRequest(exchange);
      return;
    }
    if (requestPath.startsWith(Configurations.PROCESS_UPDATES_PATH)) {
      String invokerContext = requestPath.replace(Configurations.PROCESS_UPDATES_PATH + "/", "");
      webSocketStatusUpdateHandler(invokerContext).handleRequest(exchange);
      return;
    }
  }

  private void handleHttpRequests(HttpServerExchange exchange) throws Exception {
    String requestPath = exchange.getRequestPath();
    if (requestPath.equals("/")) {
      exchange.getResponseSender().send("Hello! This page is served by Undertow.");
      return;
    }
  }

  private HttpHandler getWebSocketHandler(String invokerContext) {
    WebSocketConnectionCallback onWebSocketConnected = (exchange, webSocketChannel) -> {
      WebSocketTtyConnection conn = new WebSocketTtyConnection(webSocketChannel, executor, invokerContext);
      termServer.getPtyBootstrap().accept(conn);
    };

    HttpHandler webSocketHandshakeHandler = new WebSocketProtocolHandshakeHandler(onWebSocketConnected);
    return webSocketHandshakeHandler;
  }

  private HttpHandler webSocketStatusUpdateHandler(String invokerContext) {
    WebSocketConnectionCallback webSocketConnectionCallback = (exchange, webSocketChannel) -> {
      Consumer<PtyStatusEvent> statusUpdateListener = (statusUpdateEvent) -> {
        boolean isContextDefined = invokerContext != null && !invokerContext.equals("");
        boolean isContextMatching = invokerContext.equals(statusUpdateEvent.getContext());
        if (!isContextDefined || isContextMatching) {
          Map<String, Object> statusUpdate = new HashMap<>();
          statusUpdate.put("action", "status-update");
          TaskStatusUpdateEvent taskStatusUpdateEventWrapper = new TaskStatusUpdateEvent(statusUpdateEvent);
          statusUpdate.put("event", taskStatusUpdateEventWrapper);

          ObjectMapper objectMapper = new ObjectMapper();
          try {
            String message = objectMapper.writeValueAsString(statusUpdate);
            WebSockets.sendText(message, webSocketChannel, null);
          } catch (JsonProcessingException e) {
            log.error("Cannot write object to JSON", e);
            String errorMessage = "Cannot write object to JSON: " + e.getMessage();
            WebSockets.sendClose(CloseMessage.UNEXPECTED_ERROR, errorMessage, webSocketChannel, null);
          }
        }
      };
      log.debug("Registering new status update listener {}.", statusUpdateListener);
      termServer.addStatusUpdateListener(statusUpdateListener);
      webSocketChannel.addCloseTask((task) -> termServer.removeStatusUpdateListener(statusUpdateListener));
    };

    HttpHandler webSocketHandshakeHandler = new WebSocketProtocolHandshakeHandler(webSocketConnectionCallback);
    return webSocketHandshakeHandler;
  }

  public void stop() {
    if (server != null) {
      server.stop();
    }
  }
}
