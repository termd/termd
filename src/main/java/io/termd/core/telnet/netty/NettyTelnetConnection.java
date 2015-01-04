package io.termd.core.telnet.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyTelnetConnection extends TelnetConnection {

  private final ChannelHandlerContext context;

  public NettyTelnetConnection(TelnetHandler handler, ChannelHandlerContext context) {
    super(handler);
    this.context = context;
  }

  @Override
  protected void send(byte[] data) {
    context.writeAndFlush(Unpooled.wrappedBuffer(data));
  }
}
