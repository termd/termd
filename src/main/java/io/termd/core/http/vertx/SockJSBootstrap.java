package io.termd.core.http.vertx;

import io.termd.core.tty.TtyConnection;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

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
    Vertx vertx = Vertx.vertx();
    Router router = Router.router(vertx);
    SockJSHandler sockJSHandler = SockJSHandler.create(vertx, new SockJSHandlerOptions());
    sockJSHandler.socketHandler(socket -> {
      SockJSTtyConnection conn = new SockJSTtyConnection(socket);
      handler.accept(conn);
    });
    router.route("/term/*").handler(sockJSHandler);
    router.route().handler(ctx -> {
      HttpServerRequest req = ctx.request();

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
          Buffer buf = Buffer.buffer();
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
              resp.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
            }
          }
          resp.putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(buf.length()));
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
    HttpServer httpServer = vertx.createHttpServer();
    httpServer.requestHandler(router::accept);
    httpServer.listen(port, host, event -> {
      if (event.succeeded()) {
        completionHandler.accept(Future.succeededFuture());
      } else {
        completionHandler.accept(Future.failedFuture(event.cause()));
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
