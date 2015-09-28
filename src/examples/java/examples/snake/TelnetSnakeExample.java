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
package examples.snake;

import examples.shell.Shell;
import io.termd.core.telnet.TelnetBootstrap;
import io.termd.core.telnet.TelnetTtyConnection;
import io.termd.core.telnet.netty.NettyTelnetBootstrap;

/**
 * A test class.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetSnakeExample {

  public synchronized static void main(String[] args) throws Exception {
    new TelnetSnakeExample("localhost", 4000).start();
    TelnetSnakeExample.class.wait();
  }

  private final TelnetBootstrap telnet;

  public TelnetSnakeExample(String host, int port) {
    this(new NettyTelnetBootstrap(host, port));
  }

  public TelnetSnakeExample(TelnetBootstrap telnet) {
    this.telnet = telnet;
  }

  public void start() {
    telnet.start(() -> new TelnetTtyConnection(new Snake()));
  }
}
