package examples;

import io.termd.core.telnet.TelnetConnection;
import io.termd.core.telnet.TelnetHandler;
import io.termd.core.telnet.netty.NettyTelnetBootstrap;

import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class NettyTelnetExample {

  public static final TelnetHandler DEBUG_HANDLER = new TelnetHandler() {

    @Override
    protected void onOpen(TelnetConnection conn) {
      System.out.println("New client");
    }

    @Override
    protected void onClose() {
      System.out.println("Client closed");
    }

    @Override
    protected void onSize(int width, int height) {
      System.out.println("Resize:(" + width + "," + height + ")");
    }

    @Override
    protected void onTerminalType(String terminalType) {
      System.out.println("Terminal type: " + terminalType);
    }

    @Override
    protected void onNAWS(boolean naws) {
      System.out.println("Option NAWS:" + naws);
    }

    @Override
    protected void onEcho(boolean echo) {
      System.out.println("Option echo:" + echo);
    }

    @Override
    protected void onSGA(boolean sga) {
      System.out.println("Option SGA:" + sga);
    }

    @Override
    protected void onData(byte[] data) {
      for (byte b : data) {
        if (b >= 32) {
          System.out.println("Char:" + (char) b);
        } else {
          System.out.println("Char:<" + b + ">");
        }
      }
    }

    @Override
    protected void onCommand(byte command) {
      System.out.println("Command:" + command);
    }
  };

  public static void main(String[] args) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new NettyTelnetBootstrap().setHost("localhost").setPort(4000).start(() -> DEBUG_HANDLER).get();
    latch.await();
  }
}
