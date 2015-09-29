package examples.snake;

import io.termd.core.http.netty.NettyWebsocketTtyBootstrap;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WebsocketSnakeExample {

  public synchronized static void main(String[] args) throws Exception {
    NettyWebsocketTtyBootstrap bootstrap = new NettyWebsocketTtyBootstrap().setHost("localhost").setPort(8080);
    bootstrap.start(new SnakeGame()).get(10, TimeUnit.SECONDS);
    System.out.println("Web server started on localhost/8080");
    WebsocketSnakeExample.class.wait();
  }
}
