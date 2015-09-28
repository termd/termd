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

import io.termd.core.http.netty.NettyWebsocketBootstrap;
import io.termd.core.http.websocket.Configurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyWebsocketTest {

  private NettyWebsocketBootstrap bootstrap;

  @Before
  public void setUp() throws Exception {
    bootstrap = new NettyWebsocketBootstrap("localhost", 8080);
    bootstrap.startBlocking(conn -> {
      conn.write("hello");
      conn.close();
    });
  }

  @Test
  public void testFoo() throws Exception {

    CountDownLatch latch = new CountDownLatch(1);

    Endpoint endpoint = new Endpoint() {
      @Override
      public void onOpen(Session session, EndpointConfig endpointConfig) {
        System.out.println("opened");
        session.addMessageHandler(new MessageHandler.Whole<String>() {
          @Override
          public void onMessage(String message) {
            System.out.println("GOT MSG " + message);
            System.out.println("GOT MSG " + message);
            System.out.println("GOT MSG " + message);
            System.out.println("GOT MSG " + message);
            System.out.println("GOT MSG " + message);
          }
        });
        try {
          session.getBasicRemote().sendText("the_message");
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      @Override
      public void onClose(Session session, CloseReason closeReason) {
        latch.countDown();
        System.out.println("close");
      }
      @Override
      public void onError(Session session, Throwable thr) {
        System.out.println("error");
      }
    };

    ClientEndpointConfig clientEndpointConfig = ClientEndpointConfig.Builder.create().build();
    WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
    webSocketContainer.connectToServer(endpoint, clientEndpointConfig, new URI("http://localhost:8080/ws"));

    latch.await();
  }

  @After
  public void tearDown() throws Exception {
    bootstrap.stopBlocking();
  }

}
