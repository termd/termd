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
package io.modsh.core.telnet;

import org.vertx.java.core.Handler;
import org.vertx.java.core.net.NetSocket;

import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetHandler implements Handler<NetSocket> {

  final Function<NetSocket, TelnetSession> factory;

  public TelnetHandler(Function<NetSocket, TelnetSession> factory) {
    this.factory = factory;
  }

  public TelnetHandler() {
    this(TelnetSession::new);
  }

  @Override
  public void handle(NetSocket socket) {
    TelnetSession session = factory.apply(socket);
    socket.dataHandler(session);
    socket.closeHandler(v -> session.onClose());
    session.init();
  }
}
