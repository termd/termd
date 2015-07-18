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
import io.termd.core.pty.PtyMaster;
import io.termd.core.pty.PtyStatusEvent;
import io.termd.core.pty.TtyBridge;
import io.undertow.server.HttpHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TermServer {

  private static Thread serverThread;
  Logger log = LoggerFactory.getLogger(TermServer.class);

  private final Executor executor = Executors.newFixedThreadPool(1);
  private UndertowBootstrap undertowBootstrap;
  private int port;
  final ConcurrentHashMap<String, Term> terms = new ConcurrentHashMap<>();

  /**
   * Method returns once server is started.
   *
   * @throws InterruptedException
   */
  public static TermServer start() throws InterruptedException {
    Semaphore mutex = new Semaphore(1);

    Runnable onStart = () -> {
      mutex.release();
    };
    TermServer termServer = new TermServer();

    serverThread = new Thread(() -> {
      try {
        termServer.start("localhost", 0, onStart);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    });
    mutex.acquire();
    serverThread.start();

    mutex.acquire();
    return termServer;
  }

  public static void stopServer() {
    serverThread.interrupt();
  }


  public void start(String host, int portCandidate, Runnable onStart) throws InterruptedException {
    if (portCandidate == 0) {
      portCandidate = findFirstFreePort();
    }
    this.port = portCandidate;

    undertowBootstrap = new UndertowBootstrap(host, port, this);

    undertowBootstrap.bootstrap(completionHandler -> {
      if (completionHandler) {
        log.info("Server started on " + host + ":" + port);
        if (onStart != null) {
          onStart.run();
        }
      } else {
        log.info("Could not start server");
      }
    });
  }

  private int findFirstFreePort() {
    try (ServerSocket s = new ServerSocket(0)) {
      return s.getLocalPort();
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not obtain default port, try specifying it explicitly");
    }
  }

  public void stop() {
    undertowBootstrap.stop();
    log.info("Server stopped");
  }

  public int getPort() {
    return port;
  }

  Term newTerm(String context) {
    return new Term(context);
  }

  class Term {

    final String context;
    final Set<Consumer<PtyStatusEvent>> statusUpdateListeners = new HashSet<>();

    public Term(String context) {
      this.context = context;
    }

    public void addStatusUpdateListener(Consumer<PtyStatusEvent> statusUpdateListener) {
      statusUpdateListeners.add(statusUpdateListener);
    }

    public void removeStatusUpdateListener(Consumer<PtyStatusEvent> statusUpdateListener) {
      statusUpdateListeners.remove(statusUpdateListener);
    }

    public Consumer<PtyMaster> onTaskCreated() {
      return (ptyMaster) -> {
        Optional<FileOutputStream> fileOutputStream = Optional.empty();
        ptyMaster.setTaskStatusUpdateListener(onTaskStatusUpdate(fileOutputStream));
      };
    }

    private Consumer<PtyStatusEvent> onTaskStatusUpdate(Optional<FileOutputStream> fileOutputStream) {
      return (statusUpdateEvent) -> {
        notifyStatusUpdated(statusUpdateEvent);
      };
    }

    void notifyStatusUpdated(PtyStatusEvent statusUpdateEvent) {
      for (Consumer<PtyStatusEvent> statusUpdateListener : statusUpdateListeners) {
        log.debug("Notifying listener {} in task {} with new status {}", statusUpdateListener, statusUpdateEvent.getProcess().getId(), statusUpdateEvent.getNewStatus());
        statusUpdateListener.accept(statusUpdateEvent);
      }
    }

    HttpHandler getWebSocketHandler(String invokerContext) {
      WebSocketConnectionCallback onWebSocketConnected = (exchange, webSocketChannel) -> {
        WebSocketTtyConnection conn = new WebSocketTtyConnection(webSocketChannel, executor);
        new TtyBridge(conn, onTaskCreated()).handle();
      };

      HttpHandler webSocketHandshakeHandler = new WebSocketProtocolHandshakeHandler(onWebSocketConnected);
      return webSocketHandshakeHandler;
    }

    HttpHandler webSocketStatusUpdateHandler() {
      WebSocketConnectionCallback webSocketConnectionCallback = (exchange, webSocketChannel) -> {
        Consumer<PtyStatusEvent> statusUpdateListener = (statusUpdateEvent) -> {
          Map<String, Object> statusUpdate = new HashMap<>();
          statusUpdate.put("action", "status-update");
          TaskStatusUpdateEvent taskStatusUpdateEventWrapper = new TaskStatusUpdateEvent(statusUpdateEvent, context);
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
        };
        log.debug("Registering new status update listener {}.", statusUpdateListener);
        addStatusUpdateListener(statusUpdateListener);
        webSocketChannel.addCloseTask((task) -> removeStatusUpdateListener(statusUpdateListener));
      };

      HttpHandler webSocketHandshakeHandler = new WebSocketProtocolHandshakeHandler(webSocketConnectionCallback);
      return webSocketHandshakeHandler;
    }
  }
}
