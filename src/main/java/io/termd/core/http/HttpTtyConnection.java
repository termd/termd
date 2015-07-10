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

package io.termd.core.http;

import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.TtyEvent;
import io.termd.core.tty.TtyEventDecoder;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * A connection to an http client, independant of the protocol, it could be straight Bebsockets or
 * SockJS, etc...
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public abstract class HttpTtyConnection implements TtyConnection {

  private static Logger log = LoggerFactory.getLogger(HttpTtyConnection.class);

  private Dimension size = null;
  private Consumer<Dimension> sizeHandler;
  private final ReadBuffer readBuffer;
  private final TtyEventDecoder onCharSignalDecoder;
  private final BinaryDecoder decoder;
  private final BinaryEncoder encoder;
  private Consumer<Void> closeHandler;
  private String invokerContext;

  public HttpTtyConnection() {
    readBuffer = new ReadBuffer(command -> {
      log.debug("Server read buffer executing command: {}" + command);
      schedule(command);
    });
    onCharSignalDecoder = new TtyEventDecoder(3, 26, 4).setReadHandler(readBuffer);
    decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, onCharSignalDecoder);
    encoder = new BinaryEncoder(512, StandardCharsets.US_ASCII, this::write);
  }

  public HttpTtyConnection(String invokerContext) {
    this();
    this.invokerContext = invokerContext;
  }

  protected abstract void write(byte[] buffer);

  public void writeToDecoder(String msg) throws DecodeException {
    JsonObject obj = new JsonObject(msg.toString());
    switch (obj.getString("action")) {
      case "read":
        String data = obj.getString("data");
        decoder.write(data.getBytes()); //write back echo
        break;
    }
  }

  public String getInvokerContext() {
    return invokerContext;
  }

  public Consumer<String> termHandler() {
    return null; //TODO
  }

  public void setTermHandler(Consumer<String> handler) {
      //TODO
  }

  public Consumer<Dimension> sizeHandler() {
    return sizeHandler;
  }

  public void setSizeHandler(Consumer<Dimension> handler) {
    this.sizeHandler = handler;
    if (handler != null && size != null) {
      handler.accept(size);
    }
  }

  @Override
  public Consumer<TtyEvent> eventHandler() {
    return onCharSignalDecoder.getEventHandler();
  }

  @Override
  public void setEventHandler(Consumer<TtyEvent> handler) {
    onCharSignalDecoder.setEventHandler(handler);
  }

  public Consumer<int[]> readHandler() {
    return readBuffer.getReadHandler();
  }

  public void setReadHandler(Consumer<int[]> handler) {
    readBuffer.setReadHandler(handler);
  }

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
    //socket.close(); //TODO
  }
}
