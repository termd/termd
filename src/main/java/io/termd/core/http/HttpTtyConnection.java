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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.TtyEvent;
import io.termd.core.tty.TtyEventDecoder;
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyOutputMode;
import io.termd.core.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiConsumer;
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

  private Vector size = new Vector(80, 24); // For now hardcoded
  private Consumer<Vector> sizeHandler;
  private final ReadBuffer readBuffer;
  private final TtyEventDecoder onCharSignalDecoder;
  private final BinaryDecoder decoder;
  private final Consumer<int[]> stdout;
  private Consumer<Void> closeHandler;

  public HttpTtyConnection() {
    readBuffer = new ReadBuffer(command -> {
      log.debug("Server read buffer executing command: {}" + command);
      schedule(command);
    });
    onCharSignalDecoder = new TtyEventDecoder(3, 26, 4).setReadHandler(readBuffer);
    decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, onCharSignalDecoder);
    stdout = new TtyOutputMode(new BinaryEncoder(512, StandardCharsets.US_ASCII, this::write));
  }

  protected abstract void write(byte[] buffer);

  public void writeToDecoder(String msg) {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> obj = null;
    String action;

    try {
      obj = mapper.readValue(msg, Map.class);
      action = obj.get("action");
    } catch (IOException e) {
      throw new RuntimeException("Cannot deserialize object from json", e);
    }

    if (obj != null) {
      switch (action) {
        case "read":
          String data = obj.get("data");
          decoder.write(data.getBytes()); //write back echo
          break;
      }
    }
  }

  public Consumer<String> getTermHandler() {
    return null; //TODO
  }

  public void setTermHandler(Consumer<String> handler) {
      //TODO
  }

  @Override
  public Vector size() {
    return size;
  }

  public Consumer<Vector> getSizeHandler() {
    return sizeHandler;
  }

  public void setSizeHandler(Consumer<Vector> handler) {
    this.sizeHandler = handler;
    if (handler != null && size != null) {
      handler.accept(size);
    }
  }

  @Override
  public BiConsumer<TtyEvent, Integer> getEventHandler() {
    return onCharSignalDecoder.getEventHandler();
  }

  @Override
  public void setEventHandler(BiConsumer<TtyEvent, Integer> handler) {
    onCharSignalDecoder.setEventHandler(handler);
  }

  public Consumer<int[]> getStdinHandler() {
    return readBuffer.getReadHandler();
  }

  public void setStdinHandler(Consumer<int[]> handler) {
    readBuffer.setReadHandler(handler);
  }

  public Consumer<int[]> stdoutHandler() {
    return stdout;
  }

  @Override
  public void setCloseHandler(Consumer<Void> closeHandler) {
    this.closeHandler = closeHandler;
  }

  @Override
  public Consumer<Void> getCloseHandler() {
    return closeHandler;
  }

  @Override
  public void close() {
    if (closeHandler != null) {
      closeHandler.accept(null);
    }
  }
}
