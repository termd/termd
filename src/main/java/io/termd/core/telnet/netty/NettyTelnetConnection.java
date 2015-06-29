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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyTelnetConnection extends TelnetConnection {

  final ChannelHandlerContext context;
  private ByteBuf pending;

  public NettyTelnetConnection(TelnetHandler handler, ChannelHandlerContext context) {
    super(handler);
    this.context = context;
  }

  @Override
  public void schedule(Runnable task) {
    context.channel().eventLoop().schedule(task, 0, TimeUnit.SECONDS);
  }

  // Not properly synchronized, but ok for now
  @Override
  protected void send(byte[] data) {
    if (pending == null) {
      pending = Unpooled.buffer();
      pending.writeBytes(data);
      context.channel().eventLoop().execute(() -> {
        ByteBuf buf = pending;
        pending = null;
        context.writeAndFlush(buf);
      });
    } else {
      pending.writeBytes(data);
    }
  }

  @Override
  protected void onClose() {
    super.onClose();
  }

  @Override
  public void close() {
    context.close();
  }
}
