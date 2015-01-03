package io.termd.core.ssh;

import io.termd.core.Handler;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.CodePoint;
import io.termd.core.readline.Action;
import io.termd.core.readline.ActionHandler;
import io.termd.core.readline.Reader;
import io.termd.core.term.TermConnection;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.ChannelSessionAware;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.Signal;
import org.apache.sshd.server.SignalListener;
import org.apache.sshd.server.channel.ChannelDataReceiver;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
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

    final InputStream inputrc = Reader.class.getResourceAsStream("inputrc");

    class MyCommand implements Command, SessionAware, ChannelSessionAware, TermConnection {

      private Charset charset;
      private BinaryDecoder decoder;
      private Handler<byte[]> out;
      private HashMap.SimpleEntry<Integer, Integer> size;
      private Handler<Map.Entry<Integer, Integer>> sizeHandler;
      private Handler<int[]> charsHandler;

      @Override
      public void sizeHandler(Handler<Map.Entry<Integer, Integer>> handler) {
        sizeHandler = handler;
        if (size != null && handler != null) {
          handler.handle(new AbstractMap.SimpleEntry<>(size));
        }
      }

      @Override
      public void charsHandler(Handler<int[]> handler) {
        charsHandler = handler;
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
          public void signal(Signal signal) {
            updateSize(env);
          }
        }, EnumSet.of(Signal.WINCH));
        updateSize(env);
        decoder = new BinaryDecoder(512, charset, new Handler<int[]>() {
          @Override
          public void handle(int[] event) {
            if (charsHandler != null) {
              charsHandler.handle(event);
            }
          }
        });

        //
        configure();
      }

      private void configure() {
        this.sizeHandler(new Handler<Map.Entry<Integer, Integer>>() {
          @Override
          public void handle(Map.Entry<Integer, Integer> event) {
            System.out.println("Window size changed width=" + event.getKey() + " height=" + event.getValue());
          }
        });
        final Reader reader = new Reader(inputrc);
        final ActionHandler handler = new ActionHandler(new BinaryEncoder(512, charset, out));
        for (io.termd.core.readline.Function function : CodePoint.loadServices(Thread.currentThread().getContextClassLoader(), io.termd.core.readline.Function.class)) {
          handler.addFunction(function);
        }
        this.charsHandler(new Handler<int[]>() {
          @Override
          public void handle(int[] event) {
            reader.append(event);
            while (true) {
              Action action = reader.reduceOnce().popKey();
              if (action != null) {
                handler.handle(action);
              } else {
                break;
              }
            }

          }
        });
      }

      public void updateSize(Environment env) {
        String columns = env.getEnv().get(Environment.ENV_COLUMNS);
        String lines = env.getEnv().get(Environment.ENV_LINES);
        if (lines != null && columns != null) {
          AbstractMap.SimpleEntry<Integer, Integer> size;
          try {
            int width = Integer.parseInt(columns);
            int height = Integer.parseInt(lines);
            size = new AbstractMap.SimpleEntry<>(width, height);
          }
          catch (Exception ignore) {
            size = null;
          }
          if (size != null) {
            this.size = size;
            if (sizeHandler != null) {
              sizeHandler.handle(new AbstractMap.SimpleEntry<>(size));
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
        return new MyCommand();
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
