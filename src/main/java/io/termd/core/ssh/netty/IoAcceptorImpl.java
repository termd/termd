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

package io.termd.core.ssh.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.DefaultCloseFuture;
import org.apache.sshd.common.io.IoAcceptor;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.util.CloseableUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class IoAcceptorImpl extends CloseableUtils.AbstractCloseable implements IoAcceptor {

  final IoServiceFactoryImpl factory;
  final ChannelGroup channelGroup;
  final IoServiceImpl ioService = new IoServiceImpl();
  private final ServerBootstrap bootstrap = new ServerBootstrap();
  private final DefaultCloseFuture closeFuture = new DefaultCloseFuture(null);
  private final Map<SocketAddress, Channel> boundAddresses = new HashMap<>();
  private final IoHandler handler;

  public IoAcceptorImpl(IoServiceFactoryImpl factory, IoHandler handler) {
    this.factory = factory;
    this.handler = handler;
    channelGroup = new DefaultChannelGroup("sshd-acceptor-channels", GlobalEventExecutor.INSTANCE);;
    bootstrap.group(factory.bossGroup, factory.workerGroup)
        .channel(NioServerSocketChannel.class)
        .option(ChannelOption.SO_BACKLOG, 100)
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline p = ch.pipeline();
            p.addLast(new IoSessionImpl(IoAcceptorImpl.this, handler, null).adapter);
          }
        });
  }

  @Override
  public void bind(Collection<? extends SocketAddress> addresses) throws IOException {
    for (SocketAddress address : addresses) {
      bind(address);
    }
  }

  @Override
  public void bind(SocketAddress address) throws IOException {
    InetSocketAddress inetAddress = (InetSocketAddress) address;
    ChannelFuture f = bootstrap.bind(inetAddress);
    Channel channel = f.channel();
    channelGroup.add(channel);
    try {
      f.sync();
      SocketAddress bound = channel.localAddress();
      boundAddresses.put(bound, channel);
      channel.closeFuture().addListener(fut -> {
        boundAddresses.remove(bound);
      });
    } catch (Exception e) {
      throw Helper.toIOException(e);
    }
  }

  @Override
  public void unbind(Collection<? extends SocketAddress> addresses) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void unbind(SocketAddress address) {
    Channel channel = boundAddresses.get(address);
    if (channel != null) {
      ChannelFuture fut;
      if (channel.isOpen()) {
        fut = channel.close();
      } else {
        fut = channel.closeFuture();
      }
      try {
        fut.sync();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void unbind() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Set<SocketAddress> getBoundAddresses() {
    return new HashSet<>(boundAddresses.keySet());
  }

  @Override
  public Map<Long, IoSession> getManagedSessions() {
    return ioService.sessions;
  }

  @Override
  protected CloseFuture doCloseGracefully() {
    channelGroup.close().addListener(fut -> closeFuture.setClosed());
    return closeFuture;
  }

  @Override
  protected void doCloseImmediately() {
    doCloseGracefully();
    super.doCloseImmediately();
  }
}
