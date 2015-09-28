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
package examples;

import io.termd.core.telnet.netty.NettyTelnetBootstrap;
import io.termd.core.telnet.TelnetTtyConnection;
import io.termd.core.telnet.TelnetBootstrap;

/**
 * A test class.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetReadlineExample {

/*
  public static final Handler<ReadlineRequest> ECHO_HANDLER = new Handler<ReadlineRequest>() {
    @Override
    public void handle(final ReadlineRequest request) {
      if (request.requestCount() == 0) {
        request.write("Welcome sir\r\n\r\n% ").end();
      } else {
        request.eventHandler(new Handler<TermEvent>() {
          @Override
          public void handle(TermEvent event) {
            if (event instanceof TermEvent.Read) {
              request.write("key pressed " + Helper.fromCodePoints(((TermEvent.Read) event).getData()) + "\r\n");
            }
          }
        });
        new Thread() {
          @Override
          public void run() {
            new Thread() {
              @Override
              public void run() {
                try {
                  Thread.sleep(3000);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                } finally {
                  request.write("You just typed :" + request.line());
                  request.write("\r\n% ").end();
                }
              }
            }.start();
          }
        }.start();
      }
    }
  };
*/

  public synchronized static void main(String[] args) throws Exception {
    new TelnetReadlineExample("localhost", 4000).start();
    TelnetReadlineExample.class.wait();
  }

  private final TelnetBootstrap telnet;

  public TelnetReadlineExample(String host, int port) {
    this(new NettyTelnetBootstrap(host, port));
  }

  public TelnetReadlineExample(TelnetBootstrap telnet) {
    this.telnet = telnet;
  }

  public void start() {
    telnet.start(() -> new TelnetTtyConnection(new ReadlineApp()));
  }
}
