package io.termd.core.tty;

import io.termd.core.util.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class SignalDecoder implements Handler<int[]> {

  private Handler<int[]> readHandler;
  private Handler<Signal> signalHandler;
  private final int vintr;

  public SignalDecoder(int vintr) {
    this.vintr = vintr;
  }

  public Handler<int[]> getReadHandler() {
    return readHandler;
  }

  public SignalDecoder setReadHandler(Handler<int[]> readHandler) {
    this.readHandler = readHandler;
    return this;
  }

  public Handler<Signal> getSignalHandler() {
    return signalHandler;
  }

  public SignalDecoder setSignalHandler(Handler<Signal> signalHandler) {
    this.signalHandler = signalHandler;
    return this;
  }

  @Override
  public void handle(int[] data) {
    if (signalHandler != null) {
      for (int i = 0;i < data.length;i++) {
        if (data[i] == vintr) {
          if (signalHandler != null) {
            if (readHandler != null) {
              int[] a = new int[i];
              if (i > 0) {
                System.arraycopy(data, 0, a, 0, i);
                readHandler.handle(a);
              }
            }
            signalHandler.handle(Signal.INT);
            int[] a = new int[data.length - i - 1];
            System.arraycopy(data, i + 1, a, 0, a.length);
            data = a;
            i = 0;
          }
        }
      }
    }
    if (readHandler != null && data.length > 0) {
      readHandler.handle(data);
    }
  }
}
