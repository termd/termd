package io.termd.core.term;

import io.termd.core.Handler;
import io.termd.core.Helper;
import io.termd.core.readline.EventHandler;
import io.termd.core.readline.EventQueue;

import java.io.InputStream;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadlineTerm {

  final TermConnection conn;

  public ReadlineTerm(final TermConnection conn, Handler<TermRequest> requestHandler) {
    this.conn = conn;

    //
    InputStream inputrc = EventQueue.class.getResourceAsStream("inputrc");
    final EventHandler handler = new EventHandler(
        new EventQueue(inputrc),
        conn.charsHandler(),
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

    conn.eventHandler(new Handler<TermEvent>() {
      @Override
      public void handle(TermEvent event) {
        if (event instanceof TermEvent.Read) {
          TermEvent.Read read = (TermEvent.Read) event;
          handler.append(read.data);
        } else if (event instanceof TermEvent.Size) {
          TermEvent.Size size = (TermEvent.Size) event;
          System.out.println("Window size changed width=" + size.getWidth() + " height=" + size.getHeight());
        }
      }
    });
  }
}
