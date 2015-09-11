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

package io.termd.core.telnet.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
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
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GenericFutureListener;
import io.termd.core.telnet.TelnetBootstrap;
import io.termd.core.telnet.TelnetHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyTelnetBootstrap extends TelnetBootstrap {

  public static void main(String[] args) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new NettyTelnetBootstrap("localhost", 4000).start(() -> DEBUG_HANDLER).get();
    latch.await();
  }

  private final String host;
  private final int port;
  private EventLoopGroup bossGroup;
  private EventLoopGroup workerGroup;

  public NettyTelnetBootstrap(String host, int port) {
    this.host = host;
    this.port = port;
  }

  @Override
  public void start(Supplier<TelnetHandler> factory, Consumer<Throwable> doneHandler) {

    bossGroup = new NioEventLoopGroup(1);
    workerGroup = new NioEventLoopGroup();

    ServerBootstrap b = new ServerBootstrap();
    b.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 100)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();
            TelnetChannelHandler handler = new TelnetChannelHandler(factory);
            p.addLast(handler);
          }
        });

    ChannelFuture f = b.bind(port);
    f.addListener(abc -> {
      if (abc.isSuccess()) {
        doneHandler.accept(null);
      } else {
        doneHandler.accept(abc.cause());
      }
    });
  }

  @Override
  public void stop(Consumer<Void> doneHandler) {
    AtomicInteger count = new AtomicInteger(2);
    GenericFutureListener<Future<Object>> adapter = (Future<Object> future) -> {
      if (count.decrementAndGet() == 0) {
        doneHandler.accept(null);
      }
    };
    bossGroup.shutdownGracefully().addListener(adapter);
    workerGroup.shutdownGracefully().addListener(adapter);
  }
}
