package io.termd.core.http.vertx;

import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.Signal;
import io.termd.core.tty.SignalDecoder;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Dimension;
import io.termd.core.util.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.SockJSSocket;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SockJSTtyConnection implements TtyConnection {

  private final Vertx vertx;
  private final SockJSSocket socket;
  private Dimension size = null;
  private Handler<Dimension> resizeHandler;
  private final ReadBuffer readBuffer = new ReadBuffer(new Executor() {
    @Override
    public void execute(final Runnable command) {
      vertx.runOnContext(new org.vertx.java.core.Handler<Void>() {
        @Override
        public void handle(Void event) {
          command.run();
        }
      });
    }
  });
  private final SignalDecoder signalDecoder = new SignalDecoder(3).setReadHandler(readBuffer);
  private final BinaryDecoder decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, signalDecoder);
  private final BinaryEncoder encoder = new BinaryEncoder(512, StandardCharsets.US_ASCII, new Handler<byte[]>() {
    @Override
    public void handle(byte[] event) {
      socket.write(new Buffer(event));
    }
  });

  public SockJSTtyConnection(Vertx vertx, SockJSSocket socket) {
    this.socket = socket;
    this.vertx = vertx;

    socket.dataHandler(new org.vertx.java.core.Handler<Buffer>() {
      @Override
      public void handle(Buffer msg) {
        JsonObject obj = new JsonObject(msg.toString());
        switch (obj.getString("action")) {
          case "read":
            String data = obj.getString("data");
            decoder.write(data.getBytes());
            break;
        }
      }
    });
  }

  @Override
  public Handler<String> getTermHandler() {
    return null;
  }

  @Override
  public void setTermHandler(Handler<String> handler) {
  }

  @Override
  public Handler<Dimension> getResizeHandler() {
    return resizeHandler;
  }

  @Override
  public void setResizeHandler(Handler<Dimension> handler) {
    this.resizeHandler = handler;
    if (handler != null && size != null) {
      handler.handle(size);
    }
  }

  @Override
  public void schedule(Runnable task) {
    throw new UnsupportedOperationException("todo");
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
  public Handler<int[]> getReadHandler() {
    return readBuffer.getReadHandler();
  }

  @Override
  public void setReadHandler(Handler<int[]> handler) {
    readBuffer.setReadHandler(handler);
  }

  @Override
  public Handler<int[]> writeHandler() {
    return encoder;
  }
}
