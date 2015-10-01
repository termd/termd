package io.termd.core.ssh.netty;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class AsyncAuth extends RuntimeException {

  private volatile Consumer<Boolean> listener;
  private volatile Boolean authed;

  public void setAuthed(boolean authed) {
    Consumer<Boolean> listener;
    synchronized (this) {
      if (this.authed != null) {
        return;
      }
      this.authed = authed;
      listener = this.listener;
    }
    if (listener != null) {
      listener.accept(authed);
    }
  }

  public void setListener(Consumer<Boolean> listener) {
    Boolean result;
    synchronized (this) {
      this.listener = listener;
      result = this.authed;
    }
    if (result != null) {
      listener.accept(result);
    }
  }
}
