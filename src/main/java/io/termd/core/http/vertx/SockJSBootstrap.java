package io.termd.core.http.vertx;

import io.termd.core.tty.TtyConnection;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.impl.MimeMapping;
import org.vertx.java.core.impl.DefaultFutureResult;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.SockJSServer;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SockJSBootstrap {

  final String host;
  final int port;
  final Consumer<TtyConnection> handler;

  public SockJSBootstrap(String host, int port, Consumer<TtyConnection> handler) {
    this.host = host;
    this.port = port;
    this.handler = handler;
  }

  public void bootstrap(final Consumer<AsyncResult<Void>> completionHandler) {
    final Vertx vertx = VertxFactory.newVertx();
    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(req -> {

      // Not the best but does the job
      String path = req.path();
      if ("/".equals(path)) {
        path = "/index.html";
      }
      URL res = SockJSBootstrap.class.getResource("/io/termd/core/http" + path);
      HttpServerResponse resp = req.response();
      try {
        if (res != null) {
          InputStream in = res.openStream();
          Buffer buf = new Buffer();
          byte[] tmp = new byte[256];
          resp = req.response();
          for (int l = 0; l != -1; l = in.read(tmp)) {
            buf.appendBytes(tmp, 0, l);
          }
          int li = path.lastIndexOf('.');
          if (li != -1 && li != path.length() - 1) {
            String ext = path.substring(li + 1, path.length());
            String contentType = MimeMapping.getMimeTypeForExtension(ext);
            if (contentType != null) {
              resp.putHeader(org.vertx.java.core.http.HttpHeaders.CONTENT_TYPE, contentType);
            }
          }
          resp.putHeader(org.vertx.java.core.http.HttpHeaders.CONTENT_LENGTH, String.valueOf(buf.length()));
          resp.write(buf);
        } else {
          resp.setStatusCode(404);
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        resp.end();
      }
    });
    SockJSServer sockJSServer = vertx.createSockJSServer(httpServer);
    JsonObject config = new JsonObject().putString("prefix", "/term");
    sockJSServer.installApp(config, socket -> {
      SockJSTtyConnection conn = new SockJSTtyConnection(vertx, socket);
      handler.accept(conn);
    });
    httpServer.listen(port, host, event -> {
      if (event.succeeded()) {
        completionHandler.accept(new DefaultFutureResult<>((Void) null));
      } else {
        completionHandler.accept(new DefaultFutureResult<Void>(event.cause()));
      }
    });
  }

  public static void main(String[] args) throws Exception {
    SockJSBootstrap bootstrap = new SockJSBootstrap(
        "localhost",
        8080,
        io.termd.core.telnet.netty.ReadlineBootstrap.READLINE);
    final CountDownLatch latch = new CountDownLatch(1);
    bootstrap.bootstrap(event -> {
      if (event.succeeded()) {
        System.out.println("Server started on " + 8080);
      } else {
        System.out.println("Could not start");
        event.cause().printStackTrace();
        latch.countDown();
      }
    });
    latch.await();
  }
}
