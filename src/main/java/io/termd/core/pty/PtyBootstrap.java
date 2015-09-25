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

package io.termd.core.pty;

import io.termd.core.tty.TtyConnection;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class PtyBootstrap implements Consumer<TtyConnection> {

  public PtyBootstrap() {
  }

  public static void main(String[] args) throws Exception {
/*
    PtyBootstrap bootstrap = new PtyBootstrap();
    VertxSockJSBootstrap sockJSBootstrap = new VertxSockJSBootstrap(
        "localhost",
        8080,
        bootstrap);
    final CountDownLatch latch = new CountDownLatch(1);
    sockJSBootstrap.bootstrap(event -> {
      if (event.succeeded()) {
        System.out.println("Server started on " + 8080);
      } else {
        System.out.println("Could not start");
        event.cause().printStackTrace();
        latch.countDown();
      }
    });
    latch.await();
*/
  }

  @Override
  public void accept(final TtyConnection conn) {
    TtyBridge bridge = new TtyBridge(conn);
    bridge.readline();
  }
}
