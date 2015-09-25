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

package io.termd.core.ssh;

import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyEvent;
import io.termd.core.tty.TtyEventDecoder;
import io.termd.core.tty.TtyOutputMode;
import io.termd.core.util.Vector;
import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.server.ChannelSessionAware;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.channel.ChannelDataReceiver;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.session.ServerSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
public class SshTtyConnection implements Command, SessionAware, ChannelSessionAware, TtyConnection {

  private static final Pattern LC_PATTERN = Pattern.compile("(?:\\p{Alpha}{2}_\\p{Alpha}{2}\\.)?([^@]+)(?:@.+)?");

  private final Consumer<TtyConnection> handler;
  private Charset charset;
  private String term;
  private TtyEventDecoder eventDecoder;
  private ReadBuffer readBuffer;
  private BinaryDecoder decoder;
  private Consumer<int[]> stdout;
  private Consumer<byte[]> out;
  private Vector size = null;
  private Consumer<Vector> sizeHandler;
  private Consumer<String> termHandler;
  private Consumer<Void> closeHandler;
  protected ChannelSession session;
  private final AtomicBoolean closed = new AtomicBoolean();

  public SshTtyConnection(Consumer<TtyConnection> handler) {
    this.handler = handler;
  }

  @Override
  public Consumer<int[]> getStdinHandler() {
    return readBuffer.getReadHandler();
  }

  @Override
  public void setStdinHandler(Consumer<int[]> handler) {
    readBuffer.setReadHandler(handler);
  }

  @Override
  public Consumer<String> getTermHandler() {
    return termHandler;
  }

  @Override
  public void setTermHandler(Consumer<String> handler) {
    termHandler = handler;
    if (handler != null && term != null) {
      handler.accept(term);
    }
  }

  @Override
  public Vector size() {
    return size;
  }

  @Override
  public Consumer<Vector> getSizeHandler() {
    return sizeHandler;
  }

  @Override
  public void setSizeHandler(Consumer<Vector> handler) {
    sizeHandler = handler;
  }

  @Override
  public BiConsumer<TtyEvent, Integer> getEventHandler() {
    return eventDecoder.getEventHandler();
  }

  @Override
  public void setEventHandler(BiConsumer<TtyEvent, Integer> handler) {
    eventDecoder.setEventHandler(handler);
  }

  @Override
  public Consumer<int[]> stdoutHandler() {
    return stdout;
  }

  @Override
  public void setChannelSession(ChannelSession session) {


    // Set data receiver at this moment to prevent setting a blocking input stream
    session.setDataReceiver(new ChannelDataReceiver() {
      @Override
      public int data(ChannelSession channel, byte[] buf, int start, int len) throws IOException {
        if (decoder != null) {
          decoder.write(buf, start, len);
        } else {
          // Data send too early ?
        }
        return len;
      }

      @Override
      public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
          if (closeHandler != null) {
            closeHandler.accept(null);
          } else {
            // This happen : report it to the SSHD project
          }
        }
      }
    });

    this.session = session;
  }

  @Override
  public void setSession(ServerSession session) {
  }

  @Override
  public void setInputStream(InputStream in) {
  }

  @Override
  public void setOutputStream(final OutputStream out) {
    this.out = event -> {
      // beware : this might be blocking
      try {
        out.write(event);
        out.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    };
  }

  @Override
  public void schedule(Runnable task) {
    session.getSession().getFactoryManager().getScheduledExecutorService().execute(task);
  }

  @Override
  public void setErrorStream(OutputStream err) {
  }

  @Override
  public void setExitCallback(ExitCallback callback) {
  }

  @Override
  public void start(final Environment env) throws IOException {
    String lcctype = env.getEnv().get("LC_CTYPE");
    if (lcctype != null) {
      charset = parseCharset(lcctype);
    }
    if (charset == null) {
      charset = Charset.forName("UTF-8");
    }
    env.addSignalListener(signal -> updateSize(env), EnumSet.of(org.apache.sshd.server.Signal.WINCH));
    updateSize(env);

    // Event handling
    int vintr = getControlChar(env, PtyMode.VINTR, 3);
    int vsusp = getControlChar(env, PtyMode.VSUSP, 26);
    int veof = getControlChar(env, PtyMode.VEOF, 4);

    //
    readBuffer = new ReadBuffer(this::schedule);
    eventDecoder = new TtyEventDecoder(vintr, vsusp, veof).setReadHandler(readBuffer);
    decoder = new BinaryDecoder(512, charset, eventDecoder);
    stdout = new TtyOutputMode(new BinaryEncoder(512, charset, out));
    term = env.getEnv().get("TERM");

    //
    handler.accept(this);
  }

  private int getControlChar(Environment env, PtyMode key, int def) {
    Integer controlChar = env.getPtyModes().get(key);
    return controlChar != null ? controlChar : def;
  }

  public void updateSize(Environment env) {
    String columns = env.getEnv().get(Environment.ENV_COLUMNS);
    String lines = env.getEnv().get(Environment.ENV_LINES);
    if (lines != null && columns != null) {
      Vector size;
      try {
        int width = Integer.parseInt(columns);
        int height = Integer.parseInt(lines);
        size = new Vector(width, height);
      }
      catch (Exception ignore) {
        size = null;
      }
      if (size != null) {
        this.size = size;
        if (sizeHandler != null) {
          sizeHandler.accept(size);
        }
      }
    }
  }

  @Override
  public void destroy() {
  }

  @Override
  public void setCloseHandler(Consumer<Void> closeHandler) {
    this.closeHandler = closeHandler;
  }

  @Override
  public Consumer<Void> getCloseHandler() {
    return closeHandler;
  }

  @Override
  public void close() {
    session.close(false);
  }

  private static Charset parseCharset(String value) {
    Matcher matcher = LC_PATTERN.matcher(value);
    if (matcher.matches()) {
      try {
        return Charset.forName(matcher.group(1));
      }
      catch (Exception ignore) {
      }
    }
    return null;
  }
}
