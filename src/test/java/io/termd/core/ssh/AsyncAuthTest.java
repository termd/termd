package io.termd.core.ssh;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import io.termd.core.ssh.netty.AsyncAuth;
import io.termd.core.ssh.netty.AsyncUserAuthService;
import io.termd.core.ssh.netty.AsyncUserAuthServiceFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerConnectionServiceFactory;
import org.apache.sshd.util.EchoShellFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class AsyncAuthTest {

  SshServer server;

  private PasswordAuthenticator authenticator;

  @Before
  public void startServer() throws Exception {
    server = SshServer.setUpDefaultServer();
    server.setProperties(Collections.singletonMap(AsyncUserAuthService.AUTH_TIMEOUT, "500"));
    server.setPort(5000);
    server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
    server.setPasswordAuthenticator((username, password, sess) -> authenticator.authenticate(username, password, sess));
    server.setShellFactory(new EchoShellFactory());
    server.setServiceFactories(Arrays.asList(ServerConnectionServiceFactory.INSTANCE, AsyncUserAuthServiceFactory.INSTANCE));
    server.start();
  }

  @After
  public void stopServer() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void testSyncAuthFailed() throws Exception {
    authenticator = (username, password, sess) -> false;
    assertFalse(authenticate());
  }

  @Test
  public void testSyncAuthSucceeded() throws Exception {
    authenticator = (username, password, sess) -> true;
    assertTrue(authenticate());
  }

  @Test
  public void testAsyncAuthFailed() throws Exception {
    authenticator = (username, password, sess) -> {
      AsyncAuth auth = new AsyncAuth();
      new Thread() {
        @Override
        public void run() {
          try {
            Thread.sleep(200);
          } catch (InterruptedException ignore) {
          } finally {
            auth.setAuthed(false);
          }
        }
      }.start();
      throw auth;
    };
    assertFalse(authenticate());
  }

  @Test
  public void testAsyncAuthSucceeded() throws Exception {
    authenticator = (username, password, sess) -> {
      AsyncAuth auth = new AsyncAuth();
      new Thread() {
        @Override
        public void run() {
          try {
            Thread.sleep(200);
          } catch (InterruptedException ignore) {
          } finally {
            auth.setAuthed(true);
          }
        }
      }.start();
      throw auth;
    };
    assertTrue(authenticate());
  }

  @Test
  public void testAsyncAuthTimeout() throws Exception {
    authenticator = (username, password, sess) -> {
      throw new AsyncAuth();
    };
    assertFalse(authenticate());
  }

  @Test
  public void testAsyncAuthSucceededAfterTimeout() throws Exception {
    authenticator = (username, password, sess) -> {
      AsyncAuth auth = new AsyncAuth();
      new Thread() {
        @Override
        public void run() {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ignore) {
          } finally {
            auth.setAuthed(true);
          }
        }
      }.start();
      throw auth;
    };
    assertFalse(authenticate());
  }

  private boolean authenticate() throws Exception {

    JSch jsch = new JSch();
    Session session;
    ChannelShell channel;

    session = jsch.getSession("whatever", "localhost", 5000);
    session.setPassword("whocares");
    session.setUserInfo(new UserInfo() {
      @Override
      public String getPassphrase() {
        return null;
      }

      @Override
      public String getPassword() {
        return null;
      }

      @Override
      public boolean promptPassword(String s) {
        return false;
      }

      @Override
      public boolean promptPassphrase(String s) {
        return false;
      }

      @Override
      public boolean promptYesNo(String s) {
        return true;
      } // Accept all server keys

      @Override
      public void showMessage(String s) {
      }
    });
    try {
      session.connect();
    } catch (JSchException e) {
      if (e.getMessage().equals("Auth cancel")) {
        return false;
      } else {
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
