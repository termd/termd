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
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.termd.core.telnet.TelnetBootstrap;
import io.termd.core.telnet.TelnetHandler;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyTelnetBootstrap extends TelnetBootstrap {

  private EventLoopGroup group;
  private ChannelGroup channelGroup;

  public NettyTelnetBootstrap() {
    this.group = new NioEventLoopGroup();
    this.channelGroup = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
  }

  public NettyTelnetBootstrap setHost(String host) {
    return (NettyTelnetBootstrap) super.setHost(host);
  }

  public NettyTelnetBootstrap setPort(int port) {
    return (NettyTelnetBootstrap) super.setPort(port);
  }

  @Override
  public void start(Supplier<TelnetHandler> factory, Consumer<Throwable> doneHandler) {
    ServerBootstrap boostrap = new ServerBootstrap();
    boostrap.group(group)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 100)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            channelGroup.add(ch);
            ChannelPipeline p = ch.pipeline();
            TelnetChannelHandler handler = new TelnetChannelHandler(factory);
            p.addLast(handler);
          }
        });

    boostrap.bind(getHost(), getPort()).addListener(fut -> {
      if (fut.isSuccess()) {
        doneHandler.accept(null);
      } else {
        doneHandler.accept(fut.cause());
      }
    });
  }

  @Override
  public void stop(Consumer<Throwable> doneHandler) {
    GenericFutureListener<Future<Object>> adapter = (Future<Object> future) -> {
      doneHandler.accept(future.cause());
    };
    channelGroup.close().addListener(adapter);
  }
}
