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
}
