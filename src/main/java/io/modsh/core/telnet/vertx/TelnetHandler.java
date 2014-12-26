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

import io.modsh.core.Provider;
import io.modsh.core.telnet.TelnetSession;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetSocket;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetHandler implements Handler<NetSocket> {

  final Provider<TelnetSession> factory;

  public TelnetHandler(Provider<TelnetSession> factory) {
    this.factory = factory;
  }

  @Override
  public void handle(NetSocket socket) {
    TelnetSession session = factory.provide();
    session.output = data -> socket.write(new Buffer(data));
    socket.dataHandler(data -> session.accept(data.getBytes()));
    socket.closeHandler(v -> session.close());
    session.init();
  }
}
