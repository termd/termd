package io.termd.core.ssh;

import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.Signal;
import io.termd.core.tty.SignalDecoder;
import io.termd.core.util.Dimension;
import io.termd.core.util.Handler;
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
      private SignalDecoder signalDecoder;
      private ReadBuffer readBuffer;
      private BinaryDecoder decoder;
      private BinaryEncoder encoder;
      private Handler<byte[]> out;
      private Dimension size = null;
      private Handler<Dimension> resizeHandler;

      @Override
      public Handler<int[]> getReadHandler() {
        return readBuffer.getReadHandler();
      }

      @Override
      public void setReadHandler(Handler<int[]> handler) {
        readBuffer.setReadHandler(handler);
      }

      @Override
      public Handler<Dimension> getResizeHandler() {
        return resizeHandler;
      }

      @Override
      public void setResizeHandler(Handler<Dimension> handler) {
        resizeHandler = handler;
        if (handler != null && size != null) {
          handler.handle(size);
        }
      }

      @Override
      public Handler<Signal> getSignalHandler() {
        return signalDecoder.getSignalHandler();
      }

      @Override
      public void setSignalHandler(Handler<Signal> handler) {
        signalDecoder.setSignalHandler(handler);
      }

      @Override
      public Handler<int[]> writeHandler() {
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
        this.out = new Handler<byte[]>() {
          @Override
          public void handle(byte[] event) {
            // beware : this might be blocking
            try {
              out.write(event);
              out.flush();
            } catch (IOException e) {
              e.printStackTrace();
            }
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
        env.addSignalListener(new SignalListener() {
          @Override
          public void signal(org.apache.sshd.server.Signal signal) {
            System.out.println("GOT SIGNAL " + signal);
            updateSize(env);
          }
        }, EnumSet.of(org.apache.sshd.server.Signal.WINCH));
        updateSize(env);

        // Signal handling
        int vintr = getControlChar(env, PtyMode.VINTR, 3);

        //
        readBuffer = new ReadBuffer(new Executor() {
          @Override
          public void execute(Runnable command) {
            schedule(command);
          }
        });
        signalDecoder = new SignalDecoder(vintr).setReadHandler(readBuffer);
        decoder = new BinaryDecoder(512, charset, signalDecoder);
        encoder = new BinaryEncoder(512, charset, out);

        //
        io.termd.core.telnet.netty.ReadlineBootstrap.READLINE.handle(this);
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
              resizeHandler.handle(size);
            }
          }
        }
      }

      @Override
      public void destroy() {
      }
    }

    sshd.setShellFactory(new Factory<Command>() {
      @Override
      public Command create() {
        return new TtyCommand();
      }
    });

    sshd.setPort(5000);
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
    sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
      @Override
      public boolean authenticate(String username, String password, ServerSession session) {
        return true;
      }
    });
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
