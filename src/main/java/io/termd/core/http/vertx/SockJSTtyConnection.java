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

package io.termd.core.http.vertx;

import io.termd.core.http.TtyConnectionBridge;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SockJSTtyConnection {

  private final SockJSSocket socket;
  private final Context context;
  private final TtyConnectionBridge ttyConnection;

  public SockJSTtyConnection(SockJSSocket socket) {
    ttyConnection = new TtyConnectionBridge(onByteHandler(), (task) -> schedule(task));

    this.socket = socket;
    this.context = Vertx.currentContext();

    socket.handler(msg -> ttyConnection.writeToDecoder(msg.toString()));
  }

  private void schedule(final Runnable task) {
    context.runOnContext(v -> task.run());
  }

  private Consumer<byte[]> onByteHandler() {
    return (event) -> socket.write(Buffer.buffer(event));
  }

  public TtyConnectionBridge getTtyConnection() {
    return ttyConnection;
  }
}
