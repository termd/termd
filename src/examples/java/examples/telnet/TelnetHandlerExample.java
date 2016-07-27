package examples.telnet;

import io.termd.core.telnet.Option;
import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetHandlerExample extends TelnetHandler {

  private TelnetConnection connection;

  @Override
  protected void onOpen(TelnetConnection conn) {
    System.out.println("Client connected");
    connection = conn;

    // Negociate window size and terminal type
    conn.writeDoOption(Option.TERMINAL_TYPE);
    conn.writeDoOption(Option.NAWS);
  }

  @Override
  protected void onNAWS(boolean naws) {
    if (naws) {
      System.out.println("Client will send window size changes");
    } else {
      System.out.println("Client won't send window size changes");
    }
  }

  @Override
  protected void onData(byte[] data) {
    System.out.println("Client sent " + new String(data));
    connection.write(data);
  }

  @Override
  protected void onSize(int width, int height) {
    System.out.println("Window resized " + width + height);
  }

  @Override
  protected void onTerminalType(String terminalType) {
    System.out.println("Client declared its terminal as " + terminalType);
  }

  @Override
  protected void onClose() {
    System.out.println("Disconnected");
  }
}
