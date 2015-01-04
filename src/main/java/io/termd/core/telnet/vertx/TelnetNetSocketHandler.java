/*
 * Copyright 2014 Julien Viet
 *
 * Julien Viet licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package io.termd.core.telnet.vertx;

import io.termd.core.Provider;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetNetSocketHandler implements Handler<NetSocket> {

  final Vertx vertx;
  final Provider<TelnetHandler> factory;

  public TelnetNetSocketHandler(Vertx vertx, Provider<TelnetHandler> factory) {
    this.vertx = vertx;
    this.factory = factory;
  }

  @Override
  public void handle(final NetSocket socket) {
    TelnetHandler handler = factory.provide();
    final TelnetConnection connection = new VertxTelnetConnection(handler, vertx.currentContext(), socket);
    socket.dataHandler(new Handler<Buffer>() {
      @Override
      public void handle(Buffer event) {
        connection.receive(event.getBytes());
      }
    });
    socket.closeHandler(new Handler<Void>() {
      @Override
      public void handle(Void event) {
        connection.close();
      }
    });
    connection.init();
  }
}
