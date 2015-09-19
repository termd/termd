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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.io.IoAcceptor;
import org.apache.sshd.common.io.IoConnector;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.IoServiceFactory;
import org.apache.sshd.common.util.CloseableUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class IoServiceFactoryImpl extends CloseableUtils.AbstractCloseable implements IoServiceFactory {

  final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
  final EventLoopGroup workerGroup = new NioEventLoopGroup();

  @Override
  public IoConnector createConnector(IoHandler handler) {
    throw new UnsupportedOperationException("Only implement server for now");
  }

  @Override
  public IoAcceptor createAcceptor(IoHandler handler) {
    return new IoAcceptorImpl(this, handler);
  }

  @Override
  protected CloseFuture doCloseGracefully() {
    AtomicInteger count = new AtomicInteger(2);
    GenericFutureListener<Future<Object>> adapter = (Future<Object> future) -> {
      if (count.decrementAndGet() == 0) {
        closeFuture.setClosed();
      }
    };
    bossGroup.shutdownGracefully().addListener(adapter);
    workerGroup.shutdownGracefully().addListener(adapter);
    return closeFuture;
  }

  @Override
  protected void doCloseImmediately() {
    bossGroup.shutdownGracefully(0, 15, TimeUnit.SECONDS);
    workerGroup.shutdownGracefully(0, 15, TimeUnit.SECONDS);
    super.doCloseImmediately();
  }
}
