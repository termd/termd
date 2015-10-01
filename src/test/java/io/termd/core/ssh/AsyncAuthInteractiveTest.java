package io.termd.core.ssh;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class AsyncAuthInteractiveTest extends AsyncAuthTestBase {

  protected boolean authenticate() throws Exception {

    JSch jsch = new JSch();
    Session session;
    ChannelShell channel;

    session = jsch.getSession("whatever", "localhost", 5000);
    session.setUserInfo(new UserInfo() {
      @Override
      public String getPassphrase() {
        throw new UnsupportedOperationException();
      }

      @Override
      public String getPassword() {
        return "whocares";
      }

      @Override
      public boolean promptPassword(String s) {
        return true;
      }

      @Override
      public boolean promptPassphrase(String s) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean promptYesNo(String s) {
        return true;
      }

      @Override
      public void showMessage(String s) {
      }
    });
    try {
      session.connect();
    } catch (JSchException e) {
      switch (e.getMessage()) {
        case "Auth cancel":
        case "Auth fail":
          return false;
        default:
          throw e;
      }
    }
    channel = (ChannelShell) session.openChannel("shell");
    channel.connect();

    if (channel != null) {
      try { channel.disconnect(); } catch (Exception ignore) {}
    }
    if (session != null) {
      try { session.disconnect(); } catch (Exception ignore) {}
    }

    return true;
  }
}
