package io.termd.core.telnet.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.termd.core.Function;
import io.termd.core.Handler;
import io.termd.core.telnet.TelnetConnection;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetHandler extends ChannelInboundHandlerAdapter implements Handler<byte[]> {

  private final Function<Handler<byte[]>, TelnetConnection> factory;
  private TelnetConnection conn;
  private ChannelHandlerContext ctx;

  public TelnetHandler(Function<Handler<byte[]>, TelnetConnection> factory) {
    this.factory = factory;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    ByteBuf buf = (ByteBuf) msg;
    int size = buf.readableBytes();
    byte[] data = new byte[size];
    buf.getBytes(0, data);
    conn.handle(data);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    this.ctx = ctx;
    this.conn = factory.call(this);
    conn.init();
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    conn.close();
    this.ctx = null;
    this.conn = null;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }

  @Override
  public void handle(byte[] event) {
    ctx.writeAndFlush(Unpooled.wrappedBuffer(event));
  }
}
