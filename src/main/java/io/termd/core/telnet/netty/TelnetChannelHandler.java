package io.termd.core.telnet.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.termd.core.Provider;
import io.termd.core.telnet.TelnetHandler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetChannelHandler extends ChannelInboundHandlerAdapter {

  private final Provider<TelnetHandler> factory;
  private NettyTelnetConnection conn;

  public TelnetChannelHandler(Provider<TelnetHandler> factory) {
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
    this.conn = new NettyTelnetConnection(factory.provide(), ctx);
    conn.init();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    conn.close();
    this.conn = null;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
