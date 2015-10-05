package examples.plasma;

import io.termd.core.http.netty.NettyWebsocketTtyBootstrap;

import java.util.concurrent.TimeUnit;

public class WebsocketPlasmaExample {

  public synchronized static void main(String[] args) throws Exception {
    NettyWebsocketTtyBootstrap bootstrap = new NettyWebsocketTtyBootstrap().setHost("localhost").setPort(8080);
    bootstrap.start(new Plasma()).get(10, TimeUnit.SECONDS);
    System.out.println("Web server started on localhost:8080");
    WebsocketPlasmaExample.class.wait();
  }
}
