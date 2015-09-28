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

  public static final TelnetHandler DEBUG_HANDLER = new TelnetHandler() {

    @Override
    protected void onOpen(TelnetConnection conn) {
      System.out.println("New client");
    }

    @Override
    protected void onClose() {
      System.out.println("Client closed");
    }

    @Override
    protected void onSize(int width, int height) {
      System.out.println("Resize:(" + width + "," + height + ")");
    }

    @Override
    protected void onTerminalType(String terminalType) {
      System.out.println("Terminal type: " + terminalType);
    }

    @Override
    protected void onNAWS(boolean naws) {
      System.out.println("Option NAWS:" + naws);
    }

    @Override
    protected void onEcho(boolean echo) {
      System.out.println("Option echo:" + echo);
    }

    @Override
    protected void onSGA(boolean sga) {
      System.out.println("Option SGA:" + sga);
    }

    @Override
    protected void onData(byte[] data) {
      for (byte b : data) {
        if (b >= 32) {
          System.out.println("Char:" + (char) b);
        } else {
          System.out.println("Char:<" + b + ">");
        }
      }
    }

    @Override
    protected void onCommand(byte command) {
      System.out.println("Command:" + command);
    }
  };

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
