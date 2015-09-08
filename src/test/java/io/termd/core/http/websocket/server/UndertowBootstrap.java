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

package io.termd.core.http.websocket.server;

import io.termd.core.http.websocket.Configurations;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class UndertowBootstrap {

  Logger log = LoggerFactory.getLogger(UndertowBootstrap.class);

  final String host;
  final int port;
  private Undertow server;
  private TermServer termServer;
  final ConcurrentHashMap<String, Term> terms = new ConcurrentHashMap<>();


  public UndertowBootstrap(String host, int port, TermServer termServer) {
    this.host = host;
    this.port = port;
    this.termServer = termServer;
  }

  public void bootstrap(final Consumer<Boolean> completionHandler) {
    server = Undertow.builder()
        .addHttpListener(port, host)
        .setHandler(UndertowBootstrap.this::handleWebSocketRequests)
        .build();

    server.start();

    completionHandler.accept(true);
  }

  private void handleWebSocketRequests(HttpServerExchange exchange) throws Exception {
    String requestPath = exchange.getRequestPath();
    if (requestPath.startsWith(Configurations.TERM_PATH)) {
      log.debug("Connecting to term ...");
      String invokerContext = requestPath.replace(Configurations.TERM_PATH + "/", "");
      Term term = getTerm(invokerContext);
      term.getWebSocketHandler().handleRequest(exchange);
    } else  if (requestPath.startsWith(Configurations.PROCESS_UPDATES_PATH)) {
      log.debug("Connecting status listener ...");
      String invokerContext = requestPath.replace(Configurations.PROCESS_UPDATES_PATH + "/", "");
      Term term = getTerm(invokerContext);
      term.webSocketStatusUpdateHandler().handleRequest(exchange);
    }
  }

  private Term getTerm(String invokerContext) {
    return terms.computeIfAbsent(invokerContext, ctx -> createNewTerm(invokerContext));
  }

  private Term createNewTerm(String invokerContext) {
    log.debug("Creating new term for context [{}].", invokerContext);
    Runnable onDestroy = () -> terms.remove(invokerContext);
    return new Term(termServer, invokerContext, onDestroy, termServer.executor);
  }

  public void stop() {
    if (server != null) {
      server.stop();
    }
  }
}
