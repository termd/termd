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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.DefaultCloseFuture;
import org.apache.sshd.common.io.IoHandler;
import org.apache.sshd.common.io.IoService;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.io.IoWriteFuture;
import org.apache.sshd.common.util.CloseableUtils;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.ByteArrayBuffer;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class IoSessionImpl extends CloseableUtils.AbstractCloseable implements IoSession {

  private final Map<Object, Object> attributes = new HashMap<>();
  private final IoAcceptorImpl acceptor;
  private final IoHandler handler;
  private ChannelHandlerContext context;
  private SocketAddress remoteAddr;
  private ChannelFuture prev;
  private final DefaultCloseFuture closeFuture = new DefaultCloseFuture(null);
  private final long id;

  public IoSessionImpl(IoAcceptorImpl acceptor, IoHandler handler, IoService service) {
    this.acceptor = acceptor;
    this.handler = handler;
    this.id = acceptor.ioService.sessionSeq.incrementAndGet();
  }

  final ChannelInboundHandlerAdapter adapter = new ChannelInboundHandlerAdapter() {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      context = ctx;
      acceptor.channelGroup.add(ctx.channel());
      acceptor.ioService.sessions.put(id, IoSessionImpl.this);
      prev = context.newPromise().setSuccess();
      remoteAddr = context.channel().remoteAddress();
      handler.sessionCreated(IoSessionImpl.this);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      acceptor.ioService.sessions.remove(id);
      handler.sessionClosed(IoSessionImpl.this);
      context = null;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      ByteBuf buf = (ByteBuf) msg;
      byte[] bytes = new byte[buf.readableBytes()];
      buf.getBytes(0, bytes);
      handler.messageReceived(IoSessionImpl.this, new ByteArrayBuffer(bytes));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      cause.printStackTrace();
    }
  };

  @Override
  public long getId() {
    return id;
  }

  @Override
  public Object getAttribute(Object key) {
    return attributes.get(key);
  }

  @Override
  public Object setAttribute(Object key, Object value) {
    return attributes.put(key, value);
  }

  @Override
  public SocketAddress getRemoteAddress() {
    return remoteAddr;
  }

  @Override
  public SocketAddress getLocalAddress() {
    return context.channel().localAddress();
  }

  @Override
  public IoWriteFuture write(Buffer buffer) {
    ByteBuf buf = Unpooled.buffer(buffer.available());
    buf.writeBytes(buffer.array(), buffer.rpos(), buffer.available());
    IoWriteFutureImpl msg = new IoWriteFutureImpl();
    ChannelPromise next = context.newPromise();
    prev.addListener(whatever -> context.writeAndFlush(buf, next));
    prev = next;
    next.addListener(fut -> {
      if (fut.isSuccess()) {
        msg.setValue(Boolean.TRUE);
      } else {
        msg.setValue(fut.cause());
      }
    });
    return msg;
  }

  @Override
  public IoService getService() {
    return acceptor.ioService;
  }

  @Override
  protected CloseFuture doCloseGracefully() {
    context.
        writeAndFlush(Unpooled.EMPTY_BUFFER).
        addListener(ChannelFutureListener.CLOSE).
        addListener(fut -> {
          closeFuture.setClosed();
        });
    return closeFuture;
  }

  @Override
  protected void doCloseImmediately() {
    context.close();
    super.doCloseImmediately();
  }
}
