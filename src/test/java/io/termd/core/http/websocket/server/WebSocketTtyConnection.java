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

package io.termd.core.http.websocket.server;

import io.termd.core.http.HttpTtyConnection;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.Pooled;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class WebSocketTtyConnection extends HttpTtyConnection {

  private static Logger log = LoggerFactory.getLogger(WebSocketTtyConnection.class);
  private WebSocketChannel webSocketChannel;
  private final Executor executor;
  private Set<WebSocketChannel> readonlyChannels = new HashSet<>();

  @Override
  protected void write(byte[] buffer) {
    if (isOpen()) {
      sendBinary(buffer, webSocketChannel);
    }
    readonlyChannels.forEach((wsChannel) -> sendBinary(buffer, wsChannel));
  }

  private void sendBinary(byte[] buffer, WebSocketChannel webSocketChannel) {
    WebSockets.sendBinary(ByteBuffer.wrap(buffer), webSocketChannel, null);
  }

  @Override
  public void schedule(Runnable task) {
    executor.execute(task);
  }

  public WebSocketTtyConnection(WebSocketChannel webSocketChannel, Executor executor) {
    this.webSocketChannel = webSocketChannel;
    this.executor = executor;

    registerWebSocketChannelListener(webSocketChannel);
    webSocketChannel.resumeReceives();
  }

  private void registerWebSocketChannelListener(WebSocketChannel webSocketChannel) {
    ChannelListener<WebSocketChannel> listener = new AbstractReceiveListener() {

      @Override
      protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
        log.trace("Server received full binary message");
        Pooled<ByteBuffer[]> pulledData = message.getData();
        try {
          ByteBuffer[] resource = pulledData.getResource();
          ByteBuffer byteBuffer = WebSockets.mergeBuffers(resource);
          String msg = new String(byteBuffer.array());
          log.trace("Sending message to decoder: {}", msg);
          writeToDecoder(msg);
        } finally {
          pulledData.discard();
        }
      }
    };
    webSocketChannel.getReceiveSetter().set(listener);
  }

  public boolean isOpen() {
    return webSocketChannel != null && webSocketChannel.isOpen();
  }

  public void setWebSocketChannel(WebSocketChannel webSocketChannel) {
    this.webSocketChannel = webSocketChannel;
  }

  public void addReadonlyChannel(WebSocketChannel webSocketChannel) {
    readonlyChannels.add(webSocketChannel);
  }

  public void removeReadonlyChannel(WebSocketChannel webSocketChannel) {
    readonlyChannels.remove(webSocketChannel);
  }

  public void removeWebSocketChannel() {
    webSocketChannel = null;
  }
}
