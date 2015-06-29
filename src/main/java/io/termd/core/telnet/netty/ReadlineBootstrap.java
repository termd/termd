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
package io.termd.core.telnet.netty;

import io.termd.core.readline.KeyDecoder;
import io.termd.core.readline.Keymap;
import io.termd.core.readline.Readline;
import io.termd.core.term.Capability;
import io.termd.core.term.Device;
import io.termd.core.term.Feature;
import io.termd.core.term.TermInfo;
import io.termd.core.tty.Signal;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Helper;
import io.termd.core.telnet.TelnetTtyConnection;
import io.termd.core.telnet.TelnetBootstrap;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A test class.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineBootstrap {

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

  public static void main(String[] args) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new ReadlineBootstrap("localhost", 4000).start();
    latch.await();
  }

  private final TelnetBootstrap telnet;

  public ReadlineBootstrap(String host, int port) {
    this(new NettyTelnetBootstrap(host, port));
  }

  public ReadlineBootstrap(TelnetBootstrap telnet) {
    this.telnet = telnet;
  }

  public static final Consumer<TtyConnection> READLINE = new Consumer<TtyConnection>() {
    @Override
    public void accept(final TtyConnection conn) {
      InputStream inputrc = KeyDecoder.class.getResourceAsStream("inputrc");
      Keymap keymap = new Keymap(inputrc);
      Readline readline = new Readline(keymap);
      for (io.termd.core.readline.Function function : Helper.loadServices(Thread.currentThread().getContextClassLoader(), io.termd.core.readline.Function.class)) {
        readline.addFunction(function);
      }
      conn.setTermHandler(term -> {
        TermInfo info = TermInfo.getDefault();
        Device device = info.getDevice(term.toLowerCase());
        Integer maxColors = device.getFeature(Capability.max_colors);
        StringBuilder msg = new StringBuilder("Your term is " + term + " and we found a description for it:\r\n");
        for (Feature<?> feature : device.getFeatures()) {
          Capability<?> capability = feature.getCapability();
          msg.append(capability.name).append(" (").append(capability.description).
              append(")").append("\r\n");
        }
        conn.write(msg.toString());
      });
      conn.write("Welcome sir\r\n\r\n");
      read(conn, readline);
    }

    class Task extends Thread implements Consumer<Signal> {

      final TtyConnection conn;
      final Readline readline;
      final String line;
      volatile boolean sleeping;

      public Task(TtyConnection conn, Readline readline, String line) {
        this.conn = conn;
        this.readline = readline;
        this.line = line;
      }

      @Override
      public void accept(Signal event) {
        System.out.println("event = " + event);
        switch (event) {
          case INTR:
            if (sleeping) {
              interrupt();
            }
        }
      }

      @Override
      public void run() {
        conn.write("Running " + line + "\r\n");
        conn.setSignalHandler(this);
        sleeping = true;
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          conn.write("Interrupted\r\n");
        } finally {
          sleeping = false;
          conn.setSignalHandler(null);
        }
        read(conn, readline);
      }
    }

    public void read(final TtyConnection conn, final Readline readline) {
      readline.readline(conn, "% ", line -> {
        Task task = new Task(conn, readline, line);
        task.start();
      });
    }
  };

  public void start() {
    telnet.start(new Supplier<TelnetHandler>() {
      @Override
      public TelnetHandler get() {
        return new TelnetTtyConnection() {
          @Override
          protected void onOpen(TelnetConnection conn) {
            super.onOpen(conn);
            READLINE.accept(this);
          }
        };
      }
    });
  }
}
