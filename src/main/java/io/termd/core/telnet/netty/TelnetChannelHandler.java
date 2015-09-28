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

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.termd.core.telnet.TelnetHandler;
import io.termd.core.util.Logging;

import java.util.function.Supplier;

/**
 * Telnet server integration with Netty {@link io.netty.channel.socket.ServerSocketChannel}.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetChannelHandler extends ChannelInboundHandlerAdapter {

  private final Supplier<TelnetHandler> factory;
  private NettyTelnetConnection conn;

  public TelnetChannelHandler(Supplier<TelnetHandler> factory) {
    this.factory = factory;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf buf = (ByteBuf) msg;
    int size = buf.readableBytes();
    byte[] data = new byte[size];
    buf.getBytes(0, data);
    conn.receive(data);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    this.conn = new NettyTelnetConnection(factory.get(), ctx);
    conn.onInit();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    conn.onClose();
    this.conn = null;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    Logging.logReportedIoError(cause);
    ctx.close();
  }
}
