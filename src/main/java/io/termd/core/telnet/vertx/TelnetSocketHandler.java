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

import io.termd.core.telnet.TelnetHandler;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

import java.util.function.Supplier;

/**
 * Telnet server integration with Vert.x {@link org.vertx.java.core.net.NetServer}.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetSocketHandler implements Handler<NetSocket> {

  final Vertx vertx;
  final Supplier<TelnetHandler> factory;

  public TelnetSocketHandler(Vertx vertx, Supplier<TelnetHandler> factory) {
    this.vertx = vertx;
    this.factory = factory;
  }

  @Override
  public void handle(final NetSocket socket) {
    TelnetHandler handler = factory.get();
    final VertxTelnetConnection connection = new VertxTelnetConnection(handler, Vertx.currentContext(), socket);
    socket.handler(event -> connection.receive(event.getBytes()));
    socket.closeHandler(event -> connection.onClose());
    connection.onInit();
  }
}
