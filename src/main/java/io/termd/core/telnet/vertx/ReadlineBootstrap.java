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
package io.termd.core.telnet.vertx;

import io.termd.core.telnet.TelnetBootstrap;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;
import io.termd.core.telnet.TelnetTtyConnection;

import java.util.function.Supplier;

/**
 * A test class.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineBootstrap {

  public synchronized static void main(String[] args) throws Exception {
    new ReadlineBootstrap("localhost", 4000).start();
    ReadlineBootstrap.class.wait();
  }

  private final TelnetBootstrap telnet;

  public ReadlineBootstrap(String host, int port) {
    this(new VertxTelnetBootstrap(host, port));
  }

  public ReadlineBootstrap(TelnetBootstrap telnet) {
    this.telnet = telnet;
  }

  public void start() {
    telnet.start(() -> new TelnetTtyConnection(io.termd.core.telnet.netty.ReadlineBootstrap.READLINE));
  }
}
