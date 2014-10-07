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
package io.modsh.core.readline;

import io.modsh.core.telnet.TelnetHandler;
import io.modsh.core.telnet.TelnetSession;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.net.NetServer;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

/**
 * A test class.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineBootstrap {

  public static void main(String[] args) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new ReadlineBootstrap("localhost", 4000).start();
    latch.await();
  }

  private final String host;
  private final int port;
  private final Vertx vertx;
  private NetServer server;

  public ReadlineBootstrap(String host, int port) {
    this(VertxFactory.newVertx(), host, port);
  }

  public ReadlineBootstrap(Vertx vertx, String host, int port) {
    this.vertx = vertx;
    this.host = host;
    this.port = port;
  }

  public void start() {
    NetServer server = vertx.createNetServer();
    server.connectHandler(new TelnetHandler(socket -> new TelnetSession(socket) {

      InputStream inputrc = ReadlineBootstrap.class.getResourceAsStream("inputrc");
      Reader reader = new Reader(inputrc);

      @Override
      public void handle(Buffer data) {
        super.handle(data);
        while (true) {
          Action action = reader.reduceOnce().popKey();
          if (action != null) {
            if (action instanceof KeyAction) {
              KeyAction key = (KeyAction) action;
              System.out.println("Key " + key);
            } else {
              FunctionAction fname = (FunctionAction) action;
              System.out.println("Function " + fname.getName());
            }
          } else {
            break;
          }
        }
      }

      @Override
      protected void onChar(int c) {
        reader.append(c);
      }
    }));
    server.listen(port, host);
  }

}
