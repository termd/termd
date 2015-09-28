package examples.shell;

import io.termd.core.http.netty.NettyWebsocketBootstrap;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyWebsocketShellExample {

  public static void main(String[] args) throws Exception {
    NettyWebsocketBootstrap bootstrap = new NettyWebsocketBootstrap("localhost", 8080);
    bootstrap.start(new Shell());
    System.in.read();
    bootstrap.stop();
  }
}
