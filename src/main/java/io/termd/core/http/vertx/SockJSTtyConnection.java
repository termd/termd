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

import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.TtyEvent;
import io.termd.core.tty.TtyEventDecoder;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Dimension;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SockJSTtyConnection implements TtyConnection {

  private final SockJSSocket socket;
  private Dimension size = null;
  private Consumer<Dimension> resizeHandler;
  private Consumer<Void> closeHandler;
  private final Context context;
  private final ReadBuffer readBuffer = new ReadBuffer(new Executor() {
    @Override
    public void execute(final Runnable command) {
      context.runOnContext(event -> command.run());
    }
  });
  private final TtyEventDecoder eventDecoder = new TtyEventDecoder(3, 26, 4).setReadHandler(readBuffer);
  private final BinaryDecoder decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, eventDecoder);
  private final BinaryEncoder encoder = new BinaryEncoder(512, StandardCharsets.US_ASCII, new Consumer<byte[]>() {
    @Override
    public void accept(byte[] event) {
      socket.write(Buffer.buffer(event));
    }
  });

  public SockJSTtyConnection(SockJSSocket socket) {

    // Todo handle socket close handler ?

    this.socket = socket;
    this.context = Vertx.currentContext();

    socket.handler(new io.vertx.core.Handler<Buffer>() {
      @Override
      public void handle(Buffer msg) {
        JsonObject obj = new JsonObject(msg.toString());
        switch (obj.getString("action")) {
          case "read":
            String data = obj.getString("data");
            decoder.write(data.getBytes());
            break;
        }
      }
    });
  }

  @Override
  public Consumer<String> getTermHandler() {
    return null;
  }

  @Override
  public void setTermHandler(Consumer<String> handler) {
  }

  @Override
  public Consumer<Dimension> getResizeHandler() {
    return resizeHandler;
  }

  @Override
  public void setResizeHandler(Consumer<Dimension> handler) {
    this.resizeHandler = handler;
    if (handler != null && size != null) {
      handler.accept(size);
    }
  }

  @Override
  public void schedule(final Runnable task) {
    context.runOnContext(v -> task.run());
  }

  @Override
  public Consumer<TtyEvent> getEventHandler() {
    return eventDecoder.getEventHandler();
  }

  @Override
  public void setEventHandler(Consumer<TtyEvent> handler) {
    eventDecoder.setEventHandler(handler);
  }

  @Override
  public Consumer<int[]> getReadHandler() {
    return readBuffer.getReadHandler();
  }

  @Override
  public void setReadHandler(Consumer<int[]> handler) {
    readBuffer.setReadHandler(handler);
  }

  @Override
  public Consumer<int[]> writeHandler() {
    return encoder;
  }

  @Override
  public void setCloseHandler(Consumer<Void> closeHandler) {
    this.closeHandler = closeHandler;
  }

  @Override
  public Consumer<Void> closeHandler() {
    return closeHandler;
  }

  @Override
  public void close() {
    // Should we call the close handler ? there is no close handler on sockjs socket
    socket.close();
  }
}
