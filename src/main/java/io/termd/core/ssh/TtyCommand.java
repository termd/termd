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
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyEvent;
import io.termd.core.tty.TtyEventDecoder;
import io.termd.core.tty.TtyOutputMode;
import io.termd.core.util.Vector;
import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.io.IoInputStream;
import org.apache.sshd.common.io.IoOutputStream;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;
import org.apache.sshd.server.command.AsyncCommand;
import org.apache.sshd.server.channel.ChannelSessionAware;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelDataReceiver;
import org.apache.sshd.server.channel.ChannelSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
public class TtyCommand implements AsyncCommand, ChannelDataReceiver, ChannelSessionAware {

  private static final Pattern LC_PATTERN = Pattern.compile("(?:\\p{Alpha}{2}_\\p{Alpha}{2}\\.)?([^@]+)(?:@.+)?");

  private final Consumer<TtyConnection> handler;
  private final Charset defaultCharset;
  private Charset charset;
  private String term;
  private TtyEventDecoder eventDecoder;
  private BinaryDecoder decoder;
  private Consumer<int[]> stdout;
  private Consumer<byte[]> out;
  private Vector size = null;
  private Consumer<Vector> sizeHandler;
  private Consumer<String> termHandler;
  private Consumer<Void> closeHandler;
  protected ChannelSession session;
  private final AtomicBoolean closed = new AtomicBoolean();
  private ExitCallback exitCallback;
  private Connection conn;
  private IoOutputStream ioOut;
  private long lastAccessedTime = System.currentTimeMillis();

  public TtyCommand(Charset defaultCharset, Consumer<TtyConnection> handler) {
    this.handler = handler;
    this.defaultCharset = defaultCharset;
  }

  @Override
  public int data(ChannelSession channel, byte[] buf, int start, int len) throws IOException {
    if (decoder != null) {
      lastAccessedTime = System.currentTimeMillis();
      decoder.write(buf, start, len);
    } else {
      // Data send too early ?
    }
    return len;
  }

  @Override
  public void setChannelSession(ChannelSession session) {
    this.session = session;
  }


  @Override
  public void setInputStream(InputStream in) {
  }

  @Override
  public void setOutputStream(final OutputStream out) {
  }

  @Override
  public void setErrorStream(OutputStream err) {
  }

  @Override
  public void setIoInputStream(IoInputStream in) {
  }

  @Override
  public void setIoOutputStream(IoOutputStream out) {
    this.ioOut = out;
    this.out = bytes -> {
      ByteArrayBuffer byteArrayBuffer = new ByteArrayBuffer(bytes);
      while (byteArrayBuffer.available() > 0) {
        try {
          out.writeBuffer(byteArrayBuffer);
        } catch (WritePendingException ignored) {
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  @Override
  public void setIoErrorStream(IoOutputStream err) {

  }

  @Override
  public void setExitCallback(ExitCallback callback) {
    this.exitCallback = callback;
  }

  @Override
  public void start(ChannelSession channelSession, Environment env) throws IOException {
    String lcctype = env.getEnv().get("LC_CTYPE");
    if (lcctype != null) {
      charset = parseCharset(lcctype);
    }
    if (charset == null) {
      charset = defaultCharset;
    }
    env.addSignalListener((ch, signal) -> updateSize(env), EnumSet.of(org.apache.sshd.server.Signal.WINCH));
    updateSize(env);

    // Event handling
    int vintr = getControlChar(env, PtyMode.VINTR, 3);
    int vsusp = getControlChar(env, PtyMode.VSUSP, 26);
    int veof = getControlChar(env, PtyMode.VEOF, 4);

    //
    eventDecoder = new TtyEventDecoder(vintr, vsusp, veof);
    decoder = new BinaryDecoder(512, charset, eventDecoder);
    stdout = new TtyOutputMode(new BinaryEncoder(charset, out));
    term = env.getEnv().get("TERM");
    conn = new Connection();

    //
    session.setDataReceiver(this);
    handler.accept(conn);
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
  public void close() throws IOException {
    close(0);
  }

  private void close(int exit) throws IOException {
    ioOut.close(false).addListener(future -> {
      exitCallback.onExit(exit);
      if (closed.compareAndSet(false, true)) {
        if (closeHandler != null) {
          closeHandler.accept(null);
        } else {
          // This happen : report it to the SSHD project
        }
      }
    });
  }

  @Override
  public void destroy(ChannelSession channelSession) throws Exception {
    // Test this
  }

  protected void execute(Runnable task) {
    session.getSession().getFactoryManager().getScheduledExecutorService().execute(task);
  }

  protected void schedule(Runnable task, long delay, TimeUnit unit) {
    session.getSession().getFactoryManager().getScheduledExecutorService().schedule(task, delay, unit);
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

  private class Connection implements TtyConnection {

    @Override
    public Charset inputCharset() {
      return charset;
    }

    @Override
    public Charset outputCharset() {
      return charset;
    }

    @Override
    public long lastAccessedTime() {
      return lastAccessedTime;
    }

    @Override
    public String terminalType() {
      return term;
    }

    @Override
    public Consumer<int[]> getStdinHandler() {
      return eventDecoder.getReadHandler();
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
      eventDecoder.setReadHandler(handler);
    }

    @Override
    public Consumer<String> getTerminalTypeHandler() {
      return termHandler;
    }

    @Override
    public void setTerminalTypeHandler(Consumer<String> handler) {
      termHandler = handler;
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
    public void execute(Runnable task) {
      TtyCommand.this.execute(task);
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
      TtyCommand.this.schedule(task, delay, unit);
    }

    @Override
    public void setCloseHandler(Consumer<Void> handler) {
      closeHandler = handler;
    }

    @Override
    public Consumer<Void> getCloseHandler() {
      return closeHandler;
    }

    @Override
    public void close() {
      try {
        TtyCommand.this.close();
      } catch (IOException ignore) {
      }
    }

    @Override
    public void close(int exit) {
      try {
        TtyCommand.this.close(exit);
      } catch (IOException ignore) {
      }
    }
  }
}
