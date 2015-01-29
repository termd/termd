package io.termd.core.term;

import io.termd.core.util.Handler;
import io.termd.core.util.Helper;
import io.termd.core.readline.ReadlineHandler;
import io.termd.core.readline.ReadlineDecoder;
import io.termd.core.readline.ReadlineRequest;

import java.io.InputStream;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineTerm {

  final TermConnection conn;

  public ReadlineTerm(final TermConnection conn, Handler<ReadlineRequest> requestHandler) {
    this.conn = conn;

    //
    InputStream inputrc = ReadlineDecoder.class.getResourceAsStream("inputrc");
    final ReadlineHandler handler = new ReadlineHandler(
        new ReadlineDecoder(inputrc),
        conn.dataHandler(),
        new Executor() {
          @Override
          public void execute(Runnable command) {
            conn.schedule(command);
          }
        },
        requestHandler);
    for (io.termd.core.readline.Function function : Helper.loadServices(Thread.currentThread().getContextClassLoader(), io.termd.core.readline.Function.class)) {
      handler.addFunction(function);
    }

    // Send the init event
    handler.init();

    // Wire the handler
    conn.eventHandler(handler);
  }
}
