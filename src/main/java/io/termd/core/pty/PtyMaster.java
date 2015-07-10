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

import io.termd.core.io.BinaryDecoder;
import io.termd.core.readline.Readline;
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyEvent;
import io.termd.core.util.Helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * todo : integrate with
 * - https://github.com/traff/pty4j
 * - https://github.com/jawi/JPty
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class PtyMaster extends Thread {

  private final PtyBootstrap bootstrap;
  private final TtyConnection conn;
  private final Readline readline;
  private final String line;
  private Consumer<PtyStatusEvent> taskStatusUpdateListener;
  private Consumer<int[]> processOutputConsumer;
  private Consumer<String> processInputConsumer;
  private Status status;
  private String invokerContext; //Context is attached to the PtyStatusEvent

  public PtyMaster(PtyBootstrap bootstrap, TtyConnection conn, Readline readline, String line, String invokerContext) {
    this.bootstrap = bootstrap;
    this.conn = conn;
    this.readline = readline;
    this.line = line;
    status = Status.NEW;
    this.invokerContext = invokerContext;
  }

  public void setProcessOutputConsumer(Consumer<int[]> processOutputConsumer) {
    this.processOutputConsumer = processOutputConsumer;
  }

  public void setProcessInputConsumer(Consumer<String> processInputConsumer) {
    this.processInputConsumer = processInputConsumer;
  }

  public void setTaskStatusUpdateListener(Consumer<PtyStatusEvent> taskStatusUpdateListener) {
    this.taskStatusUpdateListener = taskStatusUpdateListener;
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
        if (processOutputConsumer != null) {
          processOutputConsumer.accept(corrected);
        }
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
    if (processInputConsumer != null) {
      processInputConsumer.accept(line);
    }
    ProcessBuilder builder = new ProcessBuilder(line.split("\\s+"));
    try {
      final java.lang.Process process = builder.start();
      setStatus(Status.RUNNING);
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
      int exitValue = -1;
      try {
        process.waitFor();
        exitValue = process.exitValue();
      } catch (InterruptedException e) {
        setStatus(Status.INTERRUPTED);
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
      if (exitValue == 0) {
        setStatus(Status.COMPLETED);
      } else {
        setStatus(Status.FAILED);
      }
    } catch (IOException e) {
      conn.writeHandler().accept(Helper.toCodePoints(e.getMessage() + "\r\n"));
    }

    // Read line again
    conn.setEventHandler(null);
    conn.schedule(() -> bootstrap.read(conn, readline, invokerContext));
  }

  private void setStatus(Status status) {
    Status old = this.status;
    this.status = status;
    PtyStatusEvent statusUpdateEvent = new PtyStatusEvent(this, old, status, invokerContext);
    if (taskStatusUpdateListener != null) {
      taskStatusUpdateListener.accept(statusUpdateEvent);
    }
  }

  public Status getStatus() {
    return status;
  }
}
