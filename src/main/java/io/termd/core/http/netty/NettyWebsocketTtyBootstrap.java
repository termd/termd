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

package io.termd.core.http.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Helper;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Convenience class for quickly starting a Netty Tty server.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyWebsocketTtyBootstrap {

  private final ChannelGroup channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
  private String host;
  private int port;
  private EventLoopGroup group;
  private Channel channel;

  public NettyWebsocketTtyBootstrap() {
    this.host = "localhost";
    this.port = 8080;
  }

  public String getHost() {
    return host;
  }

  public NettyWebsocketTtyBootstrap setHost(String host) {
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public NettyWebsocketTtyBootstrap setPort(int port) {
    this.port = port;
    return this;
  }

  public void start(Consumer<TtyConnection> handler, Consumer<Throwable> doneHandler) {
    group = new NioEventLoopGroup();

    ServerBootstrap b = new ServerBootstrap();
    b.group(group)
        .channel(NioServerSocketChannel.class)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new TtyServerInitializer(channelGroup, handler));

    ChannelFuture f = b.bind(host, port);
    f.addListener(abc -> {
      if (abc.isSuccess()) {
        channel = f.channel();
        doneHandler.accept(null);
      } else {
        doneHandler.accept(abc.cause());
      }
    });
  }

  public CompletableFuture<Void> start(Consumer<TtyConnection> handler) throws Exception {
    CompletableFuture<Void> fut = new CompletableFuture<>();
    start(handler, Helper.startedHandler(fut));
    return fut;
  }

  public void stop(Consumer<Throwable> doneHandler) {
    if (channel != null) {
      channel.close();
    }
    channelGroup.close().addListener((Future<Void> f) -> doneHandler.accept(f.cause()));
  }

  public CompletableFuture<Void> stop() throws InterruptedException {
    CompletableFuture<Void> fut = new CompletableFuture<>();
    stop(Helper.stoppedHandler(fut));
    return fut;
  }
}
