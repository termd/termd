package io.termd.core.telnet;

/**
 * The handler that defines the callbacks for a telnet connection.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TelnetHandler {

  protected void onOpen(TelnetConnection conn) {}
  protected void onClose() {}

  /**
   * Process data sent by the client.
   *
   * @param data the data
   */
  protected void onData(byte[] data) {}

  protected void onSize(int width, int height) {}
  protected void onTerminalType(String terminalType) {}
  protected void onCommand(byte command) {}
  protected void onNAWS(boolean naws) {}
  protected void onEcho(boolean echo) {}
  protected void onSGA(boolean sga) {}
  protected void onSendBinary(boolean binary) { }
  protected void onReceiveBinary(boolean binary) { }

}
