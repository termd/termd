package examples;

import io.termd.core.http.netty.NettyWebsocketBootstrap;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyWebsocketReadlineExample {

  public static void main(String[] args) throws Exception {
    NettyWebsocketBootstrap bootstrap = new NettyWebsocketBootstrap("localhost", 8080);
    bootstrap.start(TelnetReadlineExample.READLINE);
    System.in.read();
    bootstrap.stop();
  }
}
