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

package io.termd.core.telnet.vertx;

import io.termd.core.telnet.TelnetBootstrap;
import io.termd.core.telnet.TelnetHandler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxTelnetBootstrap extends TelnetBootstrap {

  public static void main(String[] args) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new VertxTelnetBootstrap("localhost", 4000).start();
    latch.await();
  }

  private final boolean closeVertx;
  private final Vertx vertx;
  private NetServer server;

  private VertxTelnetBootstrap(Vertx vertx, NetServerOptions options, boolean closeVertx) {
    this.server = vertx.createNetServer(options);
    this.vertx = vertx;
    this.closeVertx = closeVertx;
  }

  public VertxTelnetBootstrap(Vertx vertx, NetServerOptions options) {
    this(vertx, options, false);
  }

  public VertxTelnetBootstrap(Vertx vertx, String host, int port) {
    this(vertx, new NetServerOptions().setHost(host).setPort(port));
  }

  public VertxTelnetBootstrap(String host, int port) {
    this(new NetServerOptions().setHost(host).setPort(port));
  }

  public VertxTelnetBootstrap(NetServerOptions options) {
    this(Vertx.vertx(), options, true);
  }

  @Override
  public void start(Supplier<TelnetHandler> factory) {
    server.connectHandler(new TelnetSocketHandler(vertx, factory));
    server.listen();
  }

  @Override
  public void stop() {
    if (server != null) {
      server.close();
    }
    if (closeVertx) {
      vertx.close();
    }
  }
}


