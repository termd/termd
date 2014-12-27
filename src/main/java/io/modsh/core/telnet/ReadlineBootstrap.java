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

import io.modsh.core.Function;
import io.modsh.core.Handler;
import io.modsh.core.io.BinaryEncoder;
import io.modsh.core.readline.Action;
import io.modsh.core.readline.ActionHandler;
import io.modsh.core.readline.Reader;
import io.modsh.core.telnet.vertx.VertxTelnetBootstrap;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
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

  private final TelnetBootstrap telnet;

  public ReadlineBootstrap(String host, int port) {
    this(new VertxTelnetBootstrap(host, port));
  }

  public ReadlineBootstrap(TelnetBootstrap telnet) {
    this.telnet = telnet;
  }

  public void start() {

    InputStream inputrc = Reader.class.getResourceAsStream("inputrc");
    final Reader reader = new Reader(inputrc);

    telnet.start(new Function<Handler<byte[]>, TelnetConnection>() {
      @Override
      public TelnetConnection call(Handler<byte[]> output) {
        final ActionHandler handler = new ActionHandler(new BinaryEncoder(Charset.forName("UTF-8"), output));
        TelnetTermConnection conn = new TelnetTermConnection(output);
        conn.charsHandler(new Handler<int[]>() {
          @Override
          public void handle(int[] event) {
            reader.append(event);
            while (true) {
              Action action = reader.reduceOnce().popKey();
              if (action != null) {
                handler.handle(action);
              } else {
                break;
              }
            }
          }
        });
        conn.sizeHandler(new Handler<Map.Entry<Integer, Integer>>() {
          @Override
          public void handle(Map.Entry<Integer, Integer> event) {
            System.out.println("Window size changed width=" + event.getKey() + " height=" + event.getValue());
          }
        });
        return conn;
      }
    });
  }
}
