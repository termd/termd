package io.termd.core.telnet;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.termd.core.telnet.netty.TelnetChannelHandler;
import io.termd.core.telnet.vertx.TelnetSocketHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import org.apache.commons.net.telnet.TelnetClient;
import org.junit.After;
import org.junit.Before;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class TelnetTestBase extends TestBase {

  public static final Function<Supplier<TelnetHandler>, Closeable> VERTX_SERVER = handlerFactory -> {
    Vertx vertx= Vertx.vertx();
    NetServer server = vertx.createNetServer().connectHandler(new TelnetSocketHandler(vertx, handlerFactory));
    BlockingQueue<AsyncResult<NetServer>> latch = new ArrayBlockingQueue<>(1);
    server.listen(4000, "localhost", latch::add);
    AsyncResult<NetServer> result;
    try {
      result = latch.poll(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw TestBase.failure(e);
    }
    if (result.failed()) {
      throw TestBase.failure(result.cause());
    }
    return () -> {
      server.close();
      vertx.close();
    };
  };

  public static final Function<Supplier<TelnetHandler>, Closeable> NETTY_SERVER = handlerFactory -> {
    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 100)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();
            TelnetChannelHandler handler = new TelnetChannelHandler(handlerFactory);
            p.addLast(handler);
          }
        });
    try {
      b.bind("localhost", 4000).sync();
      return () -> {
        bossGroup.shutdownGracefully();
      };
    } catch (InterruptedException e) {
      throw failure(e);
    }
  };

  protected TelnetClient client;
  protected Closeable server;

  protected abstract Function<Supplier<TelnetHandler>, Closeable> serverFactory();

  protected final String assertReadString(int length) throws Exception {
    return new String(assertReadBytes(length), 0, length, "UTF-8");
  }

  protected final byte[] assertReadBytes(int length) throws Exception {
    byte[] bytes = new byte[length];
    while (length > 0) {
      int i = client.getInputStream().read(bytes, bytes.length - length, length);
      if (i == -1) {
        throw new AssertionError();
      }
      length -= i;
    }
    return bytes;
  }


  @Before
  public void before() throws InterruptedException {
  }

  protected final void server(Supplier<TelnetHandler> factory) {
    if (server != null) {
      throw failure("Already a server");
    }
    server = VERTX_SERVER.apply(factory);
  }

  @After
  public void after() throws Exception {
    if (server != null) {
      server.close();
    }
    if (client != null && client.isConnected()) {
      try {
        client.disconnect();
      } catch (IOException ignore) {
      }
    }
  }
}
