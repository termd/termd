/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.http.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.termd.core.http.HttpTtyConnection;
import io.termd.core.util.Logging;

import java.io.InputStream;
import java.net.URL;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  private final String wsUri;

  public HttpRequestHandler(String wsUri) {
    this.wsUri = wsUri;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
    if (wsUri.equalsIgnoreCase(request.getUri())) {
      ctx.fireChannelRead(request.retain());
    } else {
      if (HttpHeaders.is100ContinueExpected(request)) {
        send100Continue(ctx);
      }

      HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.INTERNAL_SERVER_ERROR);

      String path = request.getUri();
      if ("/".equals(path)) {
        path = "/index.html";
      }
      URL res = HttpTtyConnection.class.getResource("/io/termd/core/http" + path);
      try {
        if (res != null) {
          DefaultFullHttpResponse fullResp = new DefaultFullHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
          InputStream in = res.openStream();
          byte[] tmp = new byte[256];
          for (int l = 0; l != -1; l = in.read(tmp)) {
            fullResp.content().writeBytes(tmp, 0, l);
          }
          int li = path.lastIndexOf('.');
          if (li != -1 && li != path.length() - 1) {
            String ext = path.substring(li + 1, path.length());
            String contentType;
            switch (ext) {
              case "html":
                contentType = "text/html";
                break;
              case "js":
                contentType = "application/javascript";
                break;
              default:
                contentType = null;
                break;
            }
            if (contentType != null) {
              fullResp.headers().set(HttpHeaders.Names.CONTENT_TYPE, contentType);
            }
          }
          response = fullResp;
        } else {
          response.setStatus(HttpResponseStatus.NOT_FOUND);
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        ctx.write(response);
        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        future.addListener(ChannelFutureListener.CLOSE);
      }
    }
  }

  private static void send100Continue(ChannelHandlerContext ctx) {
    FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
    ctx.writeAndFlush(response);
  }

  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception {
    Logging.logReportedIoError(cause);
    ctx.close();
  }
}
