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

import io.termd.core.pty.PtyBootstrap;
import io.termd.core.pty.PtyMaster;
import io.termd.core.pty.PtyStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TermServer {

  private static Thread serverThread;
  Logger log = LoggerFactory.getLogger(TermServer.class);

  PtyBootstrap ptyBootstrap;
  private UndertowBootstrap undertowBootstrap;
  private int port;

  private Set<Consumer<PtyStatusEvent>> statusUpdateListeners = new HashSet<>();

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
        termServer.start("localhost", 0, Optional.of(onStart));
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


  public void start(String host, int portCandidate, final Optional<Runnable> onStart) throws InterruptedException {
    if (portCandidate == 0) {
      portCandidate = findFirstFreePort();
    }
    this.port = portCandidate;

    ptyBootstrap = new PtyBootstrap(onTaskCreated());

    undertowBootstrap = new UndertowBootstrap(host, port, this);

    undertowBootstrap.bootstrap(completionHandler -> {
      if (completionHandler) {
        log.info("Server started on " + host + ":" + port);
        onStart.ifPresent(r -> r.run());
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

  private Consumer<PtyMaster> onTaskCreated() {
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

  public PtyBootstrap getPtyBootstrap() {
    return ptyBootstrap;
  }

  public void stop() {
    undertowBootstrap.stop();
    log.info("Server stopped");
  }

  public void addStatusUpdateListener(Consumer<PtyStatusEvent> statusUpdateListener) {
    statusUpdateListeners.add(statusUpdateListener);
  }

  public void removeStatusUpdateListener(Consumer<PtyStatusEvent> statusUpdateListener) {
    statusUpdateListeners.remove(statusUpdateListener);
  }

  public int getPort() {
    return port;
  }
}
