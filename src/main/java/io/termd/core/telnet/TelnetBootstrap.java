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
package io.termd.core.telnet;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A test class.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TelnetBootstrap {

  public CompletableFuture<?> start(Supplier<TelnetHandler> factory) {
    CompletableFuture<?> fut = new CompletableFuture<>();
    start(factory, err -> {
      if (err == null) {
        fut.complete(null);
      } else {
        fut.completeExceptionally(err);
      }
    });
    return fut;
  }

  public CompletableFuture<?> stop() {
    CompletableFuture<?> fut = new CompletableFuture<>();
    stop(err -> {
      fut.complete(null);
    });
    return fut;
  }

  /**
   * Start the telnet server
   *
   * @param factory the telnet handler factory
   * @param doneHandler the done handler
   */
  public abstract void start(Supplier<TelnetHandler> factory, Consumer<Throwable> doneHandler);

  public abstract void stop(Consumer<Void> doneHandler);

}
