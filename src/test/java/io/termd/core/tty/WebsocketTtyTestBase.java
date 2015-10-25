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

package io.termd.core.tty;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
  public abstract class WebsocketTtyTestBase extends TtyTestBase {

  private Endpoint endpoint;
  private Session session;
  private PipedReader in;

  @After
  public void after() throws Exception {
    if (session != null) {
      try {
        session.close();
      } finally {
        in = null;
        session = null;
        endpoint = null;
      }
    }
  }

  @Override
  public boolean checkDisconnected() {
    return session == null || !session.isOpen();
  }

  @Override
  protected void assertConnect(String term) throws Exception {
    if (endpoint != null) {
      throw failure("Already a session");
    }
    CountDownLatch latch = new CountDownLatch(1);
    PipedWriter out = new PipedWriter();
    in = new PipedReader(out);
    endpoint = new Endpoint() {
      @Override
      public void onOpen(Session session, EndpointConfig endpointConfig) {
        session.addMessageHandler(new MessageHandler.Whole<String>() {
          @Override
          public void onMessage(String message) {
            try {
              out.write(message);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        });
        latch.countDown();
      }
      @Override
      public void onClose(Session sess, CloseReason closeReason) {
        session = null;
        endpoint = null;
        in = null;
      }
      @Override
      public void onError(Session session, Throwable thr) {
      }
    };
    ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().build();
    WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
    session = webSocketContainer.connectToServer(endpoint, clientEndpointConfig, new URI("http://localhost:8080/ws"));
    latch.await();
  }

  @Override
  protected void assertWrite(String s) throws Exception {
    Map<String, String> msg = new HashMap<>();
    msg.put("action", "read");
    msg.put("data", s);
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(msg);
    session.getBasicRemote().sendText(json);
  }

  @Override
  protected String assertReadString(int len) throws Exception {
    char[] buf = new char[len];
    while (len > 0) {
      int count = in.read(buf, buf.length - len, len);
      if (count == -1) {
        throw failure("Could not read enough");
      }
      len -= count;
    }
    return new String(buf);
  }

  @Override
  protected void assertWriteln(String s) throws Exception {
    assertWrite(s + "\r");
  }

  @Override
  protected void assertDisconnect(boolean clean) throws Exception {
    if (clean) {
      session.close();
    } else {
      // No way ???
      session.close();
    }
  }

  @Override
  protected void resize(int width, int height) throws Exception {
    Map<String, Object> msg = new HashMap<>();
    msg.put("action", "resize");
    msg.put("cols", width);
    msg.put("rows", height);
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(msg);
    session.getBasicRemote().sendText(json);
  }
}
