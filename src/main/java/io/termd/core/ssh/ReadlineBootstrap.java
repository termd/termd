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

import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.Signal;
import io.termd.core.tty.SignalDecoder;
import io.termd.core.util.Dimension;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.tty.TtyConnection;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.PtyMode;
import org.apache.sshd.server.ChannelSessionAware;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.SignalListener;
import org.apache.sshd.server.channel.ChannelDataReceiver;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Readline bootstrap for SSH.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineBootstrap {

  private static final Pattern LC_PATTERN = Pattern.compile("(?:\\p{Alpha}{2}_\\p{Alpha}{2}\\.)?([^@]+)(?:@.+)?");

  public static void main(String[] args) throws Exception {

    SshServer sshd = SshServer.setUpDefaultServer();

    class TtyCommand implements Command, SessionAware, ChannelSessionAware, TtyConnection {

      private Charset charset;
      private String term;
      private SignalDecoder signalDecoder;
      private ReadBuffer readBuffer;
      private BinaryDecoder decoder;
      private BinaryEncoder encoder;
      private Consumer<byte[]> out;
      private Dimension size = null;
      private Consumer<Dimension> resizeHandler;
      private Consumer<String> termHandler;

      @Override
      public Consumer<int[]> getReadHandler() {
        return readBuffer.getReadHandler();
      }

      @Override
      public void setReadHandler(Consumer<int[]> handler) {
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
      public Consumer<Dimension> getResizeHandler() {
        return resizeHandler;
      }

      @Override
      public void setResizeHandler(Consumer<Dimension> handler) {
        resizeHandler = handler;
        if (handler != null && size != null) {
          handler.accept(size);
        }
      }

      @Override
      public Consumer<Signal> getSignalHandler() {
        return signalDecoder.getSignalHandler();
      }

      @Override
      public void setSignalHandler(Consumer<Signal> handler) {
        signalDecoder.setSignalHandler(handler);
      }

      @Override
      public Consumer<int[]> writeHandler() {
        return encoder;
      }

      @Override
      public void setChannelSession(final ChannelSession session) {


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

          }
        });
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
        new Thread(task).start(); // Not awesome but ok for now and testing
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

        // Signal handling
        int vintr = getControlChar(env, PtyMode.VINTR, 3);
        int vsusp = getControlChar(env, PtyMode.VSUSP, 26);
        int veof = getControlChar(env, PtyMode.VEOF, 4);

        //
        readBuffer = new ReadBuffer(this::schedule);
        signalDecoder = new SignalDecoder(vintr, vsusp, veof).setReadHandler(readBuffer);
        decoder = new BinaryDecoder(512, charset, signalDecoder);
        encoder = new BinaryEncoder(512, charset, out);
        term = env.getEnv().get("TERM");

        //
        io.termd.core.telnet.netty.ReadlineBootstrap.READLINE.accept(this);
      }

      private int getControlChar(Environment env, PtyMode key, int def) {
        Integer controlChar = env.getPtyModes().get(key);
        return controlChar != null ? controlChar : def;
      }

      public void updateSize(Environment env) {
        String columns = env.getEnv().get(Environment.ENV_COLUMNS);
        String lines = env.getEnv().get(Environment.ENV_LINES);
        if (lines != null && columns != null) {
          Dimension size;
          try {
            int width = Integer.parseInt(columns);
            int height = Integer.parseInt(lines);
            size = new Dimension(width, height);
          }
          catch (Exception ignore) {
            size = null;
          }
          if (size != null) {
            this.size = size;
            if (resizeHandler != null) {
              resizeHandler.accept(size);
            }
          }
        }
      }

      @Override
      public void destroy() {
      }
    }

    sshd.setShellFactory(TtyCommand::new);

    sshd.setPort(5000);
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
    sshd.setPasswordAuthenticator((username, password, session) -> true);
    sshd.start();

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
