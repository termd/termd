package io.termd.core.ssh;

import com.jcraft.jsch.JSchException;
import io.termd.core.TestBase;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerConnectionServiceFactory;
import org.apache.sshd.server.session.ServerUserAuthServiceFactory;
import org.apache.sshd.util.test.EchoShellFactory;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class AsyncAuthTest extends TestBase {

  SshServer server;

  private PasswordAuthenticator authenticator;

  public void startServer() throws Exception {
    startServer(null);
  }

  public void startServer(Integer timeout) throws Exception {
    if (server != null) {
      throw failure("Server already started");
    }
    server = SshServer.setUpDefaultServer();
    if (timeout != null) {
      server.getProperties().put(CoreModuleProperties.AUTH_TIMEOUT.getName(), timeout.toString());
    }
    server.setPort(5000);
    server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
    server.setPasswordAuthenticator((username, password, sess) -> authenticator.authenticate(username, password, sess));
    server.setShellFactory(new EchoShellFactory());
    server.setServiceFactories(Arrays.asList(ServerConnectionServiceFactory.INSTANCE, ServerUserAuthServiceFactory.INSTANCE));
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
    startServer();
    authenticator = (username, password, sess) -> false;
    assertFalse(authenticate());
  }

  @Test
  public void testSyncAuthSucceeded() throws Exception {
    startServer();
    authenticator = (username, password, sess) -> true;
    assertTrue(authenticate());
  }

  @Test
  public void testAsyncAuthFailed() throws Exception {
    startServer();
    authenticator = (username, password, sess) -> {
      AsyncAuthException auth = new AsyncAuthException();
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
    startServer();
    authenticator = (username, password, sess) -> {
      AsyncAuthException auth = new AsyncAuthException();
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
    startServer(500);
    authenticator = (username, password, sess) -> {
      throw new AsyncAuthException();
    };
    try {
      authenticate();
    } catch (JSchException e) {
      assertTrue("Unexpected failure " + e.getMessage(), e.getMessage().startsWith("SSH_MSG_DISCONNECT"));
    }
  }

  @Test
  public void testAsyncAuthSucceededAfterTimeout() throws Exception {
    startServer(500);
    authenticator = (username, password, sess) -> {
      AsyncAuthException auth = new AsyncAuthException();
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
    try {
      authenticate();
    } catch (JSchException e) {
      assertTrue("Unexpected failure " + e.getMessage(), e.getMessage().startsWith("SSH_MSG_DISCONNECT"));
    }
  }

  protected boolean authenticate() throws Exception {
    try (SshClient client = SshClient.setUpDefaultClient()) {
      client.start();
      ClientSession sess = client
              .connect("whatever", "localhost", 5000)
              .verify()
              .getSession();
      sess.addPasswordIdentity("whocares");
      AuthFuture auth = sess.auth();
      auth.await(TimeUnit.SECONDS.toMillis(5000));
      return auth.isSuccess();
    }
  }

}
