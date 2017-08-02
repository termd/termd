/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.tty;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import io.termd.core.TestBase;
import io.termd.core.ssh.TtyCommand;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class SshTtyTestBase extends TtyTestBase {

  JSch jsch = new JSch();
  Session session;
  ChannelShell channel;
  InputStream in;
  OutputStream out;

  @Override
  protected void assertConnect(String term) throws Exception {
    if (session != null) {
      throw failure("Already a session");
    }
    Exception cause = new Exception("Could not connect");
    for (int i = 0;i < 10;i++) {
      try {
        Session sess = jsch.getSession("whatever", "localhost", 5000);
        sess.setPassword("whocares");
        sess.setUserInfo(new UserInfo() {
          public String getPassphrase() { return null; }
          public String getPassword() { return null; }
          public boolean promptPassword(String s) { return false; }
          public boolean promptPassphrase(String s) { return false; }
          public boolean promptYesNo(String s) { return true; /* Accept all server keys */ }
          public void showMessage(String s) { }
        });
        sess.connect();
        session = sess;
        break;
      } catch (JSchException e) {
        // Retry up to 10 times (see https://sourceforge.net/p/jsch/bugs/111/)
        // Save cause for throwing it later
        cause = e;
      }
    }
    if (session == null) {
      throw cause;
    }
    channel = (ChannelShell) session.openChannel("shell");
    if (term != null) {
      channel.setPtyType(term);
    }
    channel.connect();
    in = channel.getInputStream();
    out = channel.getOutputStream();
  }

  @Override
  public boolean checkDisconnected() {
    try {
      return in != null && in.read() == -1;
    } catch (IOException e) {
      throw TestBase.failure(e);
    }
  }

  @Override
  protected void assertDisconnect(boolean clean) throws Exception {
    if (clean) {
      session.disconnect();
    } else {
      Field socketField = session.getClass().getDeclaredField("socket");
      socketField.setAccessible(true);
      Socket socket = (Socket) socketField.get(session);
      socket.close();
    }
  }

  @Override
  protected void resize(int width, int height) {
    channel.setPtySize(width, height, width * 8, height * 8);
  }

  @Override
  protected void assertWrite(String s) throws Exception {
    out.write(s.getBytes(charset));
    out.flush();
  }

  @Override
  protected String assertReadString(int len) throws Exception {
    byte[] buf = new byte[len];
    while (len > 0) {
      int count = in.read(buf, buf.length - len, len);
      if (count == -1) {
        throw failure("Could not read enough");
      }
      len -= count;
    }
    return new String(buf, "UTF-8");
  }

  @Override
  protected void assertWriteln(String s) throws Exception {
    assertWrite((s + "\r"));
  }

  private SshServer sshd;

  protected abstract SshServer createServer();

  protected TtyCommand createConnection(Consumer<TtyConnection> onConnect) {
    return new TtyCommand(charset, onConnect);
  }

  @Override
  protected void server(Consumer<TtyConnection> onConnect) {
    if (sshd != null) {
      throw failure("Already a server");
    }
    try {
      sshd = createServer();
      sshd.setPort(5000);
      sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(new File("hostkey.ser").toPath()));
      sshd.setPasswordAuthenticator((username, password, session) -> true);
      sshd.setShellFactory(() -> createConnection(onConnect));
      sshd.start();
    } catch (Exception e) {
      throw failure(e);
    }
  }

  @Before
  public void before() {
    sshd = null;
    session = null;
  }

  @Test
  public void testExitCode() throws Exception {
    server(conn -> {
      conn.setStdinHandler(bytes -> {
        conn.close(25);
      });
    });
    assertConnect();
    assertWrite("whatever");
    long timeout = System.currentTimeMillis() + 5000;
    while (!channel.isClosed()) {
      assertTrue(System.currentTimeMillis() < timeout);
      Thread.sleep(10);
    }
    assertEquals(25, channel.getExitStatus());
  }

  @After
  public void after() throws Exception {
    if (out != null) {
      try { out.close(); } catch (Exception ignore) {}
    }
    if (in != null) {
      try { in.close(); } catch (Exception ignore) {}
    }
    if (channel != null) {
      try { channel.disconnect(); } catch (Exception ignore) {}
    }
    if (session != null) {
      try { session.disconnect(); } catch (Exception ignore) {}
    }
    if (sshd != null && !sshd.isClosed()) {
      try {
        sshd.close();
      } catch (Exception ignore) {
      }
    }
  }
}
