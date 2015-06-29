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

package io.termd.core.http.vertx;

import io.termd.core.io.BinaryDecoder;
import io.termd.core.readline.KeyDecoder;
import io.termd.core.readline.Keymap;
import io.termd.core.readline.Readline;
import io.termd.core.tty.TtyEvent;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NativeProcessBootstrap implements Consumer<TtyConnection> {


  @Override
  public void accept(final TtyConnection conn) {
    InputStream inputrc = KeyDecoder.class.getResourceAsStream("inputrc");
    Keymap keymap = new Keymap(inputrc);
    Readline readline = new Readline(keymap);
    for (io.termd.core.readline.Function function : Helper.loadServices(Thread.currentThread().getContextClassLoader(), io.termd.core.readline.Function.class)) {
      readline.addFunction(function);
    }
    conn.setTermHandler(term -> {
      // Not used yet but we should propagage this to the process builder
      System.out.println("CLIENT $TERM=" + term);
    });
    conn.write("Welcome sir\r\n\r\n");
    read(conn, readline);
  }

  public void read(final TtyConnection conn, final Readline readline) {
    readline.readline(conn, "% ", line -> {
      Task task = new Task(conn, readline, line);
      task.start();
    });
  }

  class Task extends Thread {

    final TtyConnection conn;
    final Readline readline;
    final String line;

    public Task(TtyConnection conn, Readline readline, String line) {
      this.conn = conn;
      this.readline = readline;
      this.line = line;
    }

    private class Pipe extends Thread {

      private final Charset charset = StandardCharsets.UTF_8; // We suppose the process out/err uses UTF-8
      private final InputStream in;
      private final BinaryDecoder decoder = new BinaryDecoder(charset, codepoints -> conn.schedule(() -> {

        // Replace any \n by \r\n (need to improve that somehow...)
        int len = codepoints.length;
        for (int i = 0;i < codepoints.length;i++) {
          if (codepoints[i] == '\n' && (i == 0 || codepoints[i -1] != '\r')) {
            len++;
          }
        }
        int ptr = 0;
        int[] corrected = new int[len];
        for (int i = 0;i < codepoints.length;i++) {
          if (codepoints[i] == '\n' && (i == 0 || codepoints[i -1] != '\r')) {
            corrected[ptr++] = '\r';
            corrected[ptr++] = '\n';
          } else {
            corrected[ptr++] = codepoints[i];
          }
        }

        conn.writeHandler().accept(corrected);
      }));

      public Pipe(InputStream in) {
        this.in = in;
      }

      @Override
      public void run() {
        byte[] buffer = new byte[512];
        while (true) {
          try {
            int l = in.read(buffer);
            if (l == -1) {
              break;
            }
            decoder.write(buffer, 0, l);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }

    @Override
    public void run() {
      ProcessBuilder builder = new ProcessBuilder(line.split("\\s+"));
      try {
        final Process process = builder.start();
        conn.setEventHandler(new Consumer<TtyEvent>() {
          boolean interrupted; // Signal state

          @Override
          public void accept(TtyEvent event) {
            if (event == TtyEvent.INTR) {
              if (!interrupted) {
                interrupted = true;
                process.destroy();
              }
            }
          }
        });
        Pipe stdout = new Pipe(process.getInputStream());
        Pipe stderr = new Pipe(process.getErrorStream());
        stdout.start();
        stderr.start();
        try {
          process.waitFor();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        try {
          stdout.join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        try {
          stderr.join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      } catch (IOException e) {
        conn.write(e.getMessage() + "\r\n");
      }

      // Read line again
      conn.setEventHandler(null);
      conn.schedule(() -> read(conn, readline));
    }
  }

  public static void main(String[] args) throws Exception {
    SockJSBootstrap bootstrap = new SockJSBootstrap(
        "localhost",
        8080,
        new NativeProcessBootstrap());
    final CountDownLatch latch = new CountDownLatch(1);
    bootstrap.bootstrap(event -> {
      if (event.succeeded()) {
        System.out.println("Server started on " + 8080);
      } else {
        System.out.println("Could not start");
        event.cause().printStackTrace();
        latch.countDown();
      }
    });
    latch.await();
  }
}
