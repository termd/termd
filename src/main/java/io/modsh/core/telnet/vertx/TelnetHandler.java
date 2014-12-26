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
package io.modsh.core.telnet.vertx;

import io.modsh.core.Function;
import io.modsh.core.telnet.TelnetConnection;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetHandler implements Handler<NetSocket> {

  final Function<io.modsh.core.Handler<byte[]>, TelnetConnection> factory;

  public TelnetHandler(Function<io.modsh.core.Handler<byte[]>, TelnetConnection> factory) {
    this.factory = factory;
  }

  @Override
  public void handle(final NetSocket socket) {
    final TelnetConnection session = factory.call(new io.modsh.core.Handler<byte[]>() {
      @Override
      public void handle(byte[] event) {
        socket.write(new Buffer(event));
      }
    });
    socket.dataHandler(new Handler<Buffer>() {
      @Override
      public void handle(Buffer event) {
        session.handle(event.getBytes());
      }
    });
    socket.closeHandler(new Handler<Void>() {
      @Override
      public void handle(Void event) {
        session.close();
      }
    });
    session.init();
  }
}
