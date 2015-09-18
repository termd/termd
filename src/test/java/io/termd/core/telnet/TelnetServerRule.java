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

package io.termd.core.telnet;

import io.netty.bootstrap.ServerBootstrap;
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
import org.junit.rules.ExternalResource;

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
public class TelnetServerRule extends ExternalResource {

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
      throw TestBase.failure(e);
    }
  };

  private final Function<Supplier<TelnetHandler>, Closeable> serverFactory;
  protected Closeable server;

  public TelnetServerRule(Function<Supplier<TelnetHandler>, Closeable> serverFactory) {
    this.serverFactory = serverFactory;
  }

  @Override
  protected void after() {
    if (server != null) {
      try {
        server.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public final void start(Supplier<TelnetHandler> telnetFactory) {
    if (server != null) {
      throw TestBase.failure("Already a server");
    }
    server = serverFactory.apply(telnetFactory);
  }
}
