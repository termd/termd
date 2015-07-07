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

import io.termd.core.http.HttpTtyConnection;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxSockJSTtyConnection extends HttpTtyConnection {

  private final SockJSSocket socket;
  private final Context context;

  public VertxSockJSTtyConnection(SockJSSocket socket) {
    socket.handler(msg -> writeToDecoder(msg.toString()));

    this.socket = socket;
    this.context = Vertx.currentContext();
  }

  public void schedule(final Runnable task) {
    context.runOnContext(v -> task.run());
  }

  @Override
  protected void write(byte[] buffer) {
    socket.write(Buffer.buffer(buffer));
  }
}
