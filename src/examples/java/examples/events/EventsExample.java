package examples.events;

import io.termd.core.tty.TtyConnection;

/**
 * This example show how to handle TTY events.
 */
public class EventsExample {

  public static void handle(TtyConnection conn) {

    conn.setEventHandler((event, key) -> {
      switch (event) {
        case INTR:
          conn.write("You did a Ctrl-C\n");
          break;
        case SUSP:
          conn.write("You did a Ctrl-Z\n");
          break;
        case EOF:
          conn.write("You did a Ctrl-D: closing\n");
          conn.close();
          break;
      }
    });

    conn.setSizeHandler(size -> {
      conn.write("You resized your terminal to " + size + "\n");
    });

    conn.setTerminalTypeHandler(term -> {
      conn.write("Your terminal is " + term + "\n");
    });

    conn.setStdinHandler(keys -> {
      for (int key : keys) {
        conn.write("You typed " + key + "\n");
      }
    });
  }

}
