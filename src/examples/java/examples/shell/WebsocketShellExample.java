package examples.shell;

import io.termd.core.http.netty.NettyWebsocketTtyBootstrap;

import java.util.concurrent.TimeUnit;

public class WebsocketShellExample {

  public synchronized static void main(String[] args) throws Exception {
    NettyWebsocketTtyBootstrap bootstrap = new NettyWebsocketTtyBootstrap().setHost("localhost").setPort(8080);
    bootstrap.start(new Shell()).get(10, TimeUnit.SECONDS);
    System.out.println("Web server started on localhost:8080");
    WebsocketShellExample.class.wait();
  }
}
