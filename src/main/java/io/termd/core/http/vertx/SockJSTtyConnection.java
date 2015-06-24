package io.termd.core.http.vertx;

import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.io.TelnetCharset;
import io.termd.core.tty.ReadBuffer;
import io.termd.core.tty.Signal;
import io.termd.core.tty.SignalDecoder;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Dimension;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.SockJSSocket;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SockJSTtyConnection implements TtyConnection {

  private final SockJSSocket socket;
  private Dimension size = null;
  private Consumer<Dimension> resizeHandler;
  private final Context context;
  private final ReadBuffer readBuffer = new ReadBuffer(new Executor() {
    @Override
    public void execute(final Runnable command) {
      context.runOnContext(event -> command.run());
    }
  });
  private final SignalDecoder signalDecoder = new SignalDecoder(3).setReadHandler(readBuffer);
  private final BinaryDecoder decoder = new BinaryDecoder(512, TelnetCharset.INSTANCE, signalDecoder);
  private final BinaryEncoder encoder = new BinaryEncoder(512, StandardCharsets.US_ASCII, new Consumer<byte[]>() {
    @Override
    public void accept(byte[] event) {
      socket.write(Buffer.buffer(event));
    }
  });

  public SockJSTtyConnection(SockJSSocket socket) {
    this.socket = socket;
    this.context = Vertx.currentContext();

    socket.handler(new io.vertx.core.Handler<Buffer>() {
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
  public Consumer<String> getTermHandler() {
    return null;
  }

  @Override
  public void setTermHandler(Consumer<String> handler) {
  }

  @Override
  public Consumer<Dimension> getResizeHandler() {
    return resizeHandler;
  }

  @Override
  public void setResizeHandler(Consumer<Dimension> handler) {
    this.resizeHandler = handler;
    if (handler != null && size != null) {
      handler.accept(size);
    }
  }

  @Override
  public void schedule(final Runnable task) {
    context.runOnContext(v -> task.run());
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
  public Consumer<int[]> getReadHandler() {
    return readBuffer.getReadHandler();
  }

  @Override
  public void setReadHandler(Consumer<int[]> handler) {
    readBuffer.setReadHandler(handler);
  }

  @Override
  public Consumer<int[]> writeHandler() {
    return encoder;
  }
}
