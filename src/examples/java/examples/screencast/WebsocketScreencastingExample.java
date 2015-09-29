package examples.screencast;

import io.termd.core.http.netty.NettyWebsocketTtyBootstrap;

import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class WebsocketScreencastingExample {

  public synchronized static void main(String[] args) throws Exception {
    NettyWebsocketTtyBootstrap bootstrap = new NettyWebsocketTtyBootstrap().setHost("localhost").setPort(8080);
    Robot robot = new Robot();
    bootstrap.start(conn -> new Screencaster(robot, conn).handle()).get(10, TimeUnit.SECONDS);
    System.out.println("Web server started on localhost:8080");
    WebsocketScreencastingExample.class.wait();
  }
}
