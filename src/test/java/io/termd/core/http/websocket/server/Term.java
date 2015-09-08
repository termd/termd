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
import io.termd.core.pty.TtyBridge;
import io.undertow.server.HttpHandler;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
* @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
*/
class Term {

  private Logger log = LoggerFactory.getLogger(Term.class);

  final String context;
  private Runnable onDestroy;
  final Set<Consumer<TaskStatusUpdateEvent>> statusUpdateListeners = new HashSet<>();
  private WebSocketTtyConnection webSocketTtyConnection;
  private boolean activeCommand;
  private Executor executor;

  public Term(TermServer termServer, String context, Runnable onDestroy, Executor executor) {
    this.context = context;
    this.onDestroy = onDestroy;
    this.executor = executor;
  }

  public void addStatusUpdateListener(Consumer<TaskStatusUpdateEvent> statusUpdateListener) {
    statusUpdateListeners.add(statusUpdateListener);
  }

  public void removeStatusUpdateListener(Consumer<TaskStatusUpdateEvent> statusUpdateListener) {
    statusUpdateListeners.remove(statusUpdateListener);
  }

  public Consumer<PtyMaster> onTaskCreated() {
    return (ptyMaster) -> {
      ptyMaster.setChangeHandler((prev, next) -> {
        notifyStatusUpdated(
            new TaskStatusUpdateEvent("" + ptyMaster.getId(), prev, next, context)
        );
      });
    };
  }

  void notifyStatusUpdated(TaskStatusUpdateEvent event) {
    if (event.getNewStatus().isFinal()) {
      activeCommand = false;
      destroyIfInactiveAndDisconnected();
    } else {
      activeCommand = true;
    }
    for (Consumer<TaskStatusUpdateEvent> statusUpdateListener : statusUpdateListeners) {
      log.debug("Notifying listener {} in task {} with new status {}", statusUpdateListener, event.getTaskId(), event.getNewStatus());
      statusUpdateListener.accept(event);
    }
  }

  private void destroyIfInactiveAndDisconnected() {
    if (!activeCommand && !webSocketTtyConnection.isOpen()) {
      onDestroy.run();
    }
  }

  synchronized HttpHandler getWebSocketHandler() {
    WebSocketConnectionCallback onWebSocketConnected = (exchange, webSocketChannel) -> {
      if (webSocketTtyConnection == null) {
        webSocketTtyConnection = new WebSocketTtyConnection(webSocketChannel, executor);
        webSocketChannel.addCloseTask((task) -> {webSocketTtyConnection.removeWebSocketChannel(); destroyIfInactiveAndDisconnected();});
        TtyBridge ttyBridge = new TtyBridge(webSocketTtyConnection);
        ttyBridge
            .setProcessListener(onTaskCreated())
            .readline();
      } else {
        if (webSocketTtyConnection.isOpen()) {
          webSocketTtyConnection.addReadonlyChannel(webSocketChannel);
          webSocketChannel.addCloseTask((task) -> {webSocketTtyConnection.removeReadonlyChannel(webSocketChannel); destroyIfInactiveAndDisconnected();});
        } else {
          webSocketTtyConnection.setWebSocketChannel(webSocketChannel);
          webSocketChannel.addCloseTask((task) -> {webSocketTtyConnection.removeWebSocketChannel(); destroyIfInactiveAndDisconnected();});
        }
      }
    };
    return new WebSocketProtocolHandshakeHandler(onWebSocketConnected);
  }

  HttpHandler webSocketStatusUpdateHandler() {
    WebSocketConnectionCallback webSocketConnectionCallback = (exchange, webSocketChannel) -> {
      Consumer<TaskStatusUpdateEvent> statusUpdateListener = event -> {
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("action", "status-update");
        statusUpdate.put("event", event);

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

    return new WebSocketProtocolHandshakeHandler(webSocketConnectionCallback);
  }
}
